package com.vuclip.service;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "bitmovinconfig")
public class LowResImpl {

    private static final Logger logger = LoggerFactory.getLogger(LowResImpl.class);
    public static String apiKey;
    public static String encVersion;
    public static String inputId;
    public static String outputId;
    public static String contentApi;
    private static BitmovinApi bitmovinApi;
    private static CloudRegion cloudRegion = CloudRegion.GOOGLE_US_CENTRAL_1;

    public static String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public static String getInputId() {
        return inputId;
    }

    public void setInputId(String inputId) {
        this.inputId = inputId;
    }

    public static String getOutputId() {
        return outputId;
    }

    public void setOutputId(String outputId) {
        this.outputId = outputId;
    }

    public static String getEncVersion() { return encVersion; }

    public void setEncVersion(String encVersion) { this.encVersion = encVersion;}

    public static void script(String CID) throws InterruptedException, IOException, BitmovinException {

        try {
            logger.info("Establishing connection with bitmovin ");

            bitmovinApi = BitmovinApi.builder()
                    .withApiKey(getApiKey())
                    .build();

            logger.info("Connection with bitmovin has been established");
            Encoding encoding = createEncoding("Encoding for low resolution", "");

            String inputId = getInputId();

            String inputFilePath = CID + ".mp4";

            logger.info("inputfilePath " + inputFilePath);



            String outputId = getOutputId();
            ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);

            String outputPath = "LOW-RESOLUTION/" + CID +  "_V" + utc.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "/";


            H264VideoConfiguration h264Config = createH264VideoConfig();
            AacAudioConfiguration aacConfig = createAacAudioConfig();

            Stream videoStream = createStream(encoding, inputId, inputFilePath, h264Config);
            Stream audioStream = createStream(encoding, inputId, inputFilePath, aacConfig);

            List<Filter> filters = new ArrayList<>();
            filters.add(createWatermarkFilter());
            filters.add(createTextFilter());
            createStreamFilterList(encoding, videoStream, filters);
            createMp4Muxing(encoding, outputId,outputPath, Arrays.asList(videoStream, audioStream), CID + "_low_res.mp4");
            logger.info("Encoding Id of this request is " + encoding.getId());
            executeEncoding(encoding);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                try {
                    executeEncoding(encoding);
                } catch (InterruptedException e) {
                    logger.error(String.valueOf(e));
                } catch (BitmovinException e) {
                    logger.error(String.valueOf(e));
                }
            });
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    logger.info("ERROR: Stopping going to next step");
                }
            }catch(InterruptedException e){
                //e.printStackTrace();
                logger.error(String.valueOf(e));
            }

        } catch (BitmovinException exception) {
            logger.error(String.valueOf(exception));
        }
    }

    private static StreamFilterList createStreamFilterList(
            Encoding encoding, Stream stream, List<Filter> filters) throws BitmovinException {
        int position = 0;
        List<StreamFilter> streamFilters = new ArrayList<>();

        for (Filter filter : filters) {
            StreamFilter streamFilter = new StreamFilter();
            streamFilter.setId(filter.getId());
            streamFilter.setPosition(position++);
            streamFilters.add(streamFilter);
        }

        return bitmovinApi.encoding.encodings.streams.filters.create(
                encoding.getId(), stream.getId(), streamFilters);

    }

    private static Encoding createEncoding(String name, String description) throws BitmovinException {
        Encoding encoding = new Encoding();
        encoding.setName(name);
        encoding.setDescription(description);
        encoding.setCloudRegion(cloudRegion);
        encoding.setEncoderVersion(getEncVersion());
        return bitmovinApi.encoding.encodings.create(encoding);
    }

    private static WatermarkFilter createWatermarkFilter() throws BitmovinException {
        WatermarkFilter watermarkFilter = new WatermarkFilter();
        watermarkFilter.setImage("https://drive.google.com/file/d/1y_qa7N6g8fZVb7OzhZQKJTh0czbXB6oR/view?usp=sharing");
        watermarkFilter.setTop(10);
        watermarkFilter.setRight(10);
        return bitmovinApi.encoding.filters.watermark.create(watermarkFilter);
    }

    private static TextFilter createTextFilter() throws BitmovinException {
        TextFilter textFilter = new TextFilter();
        textFilter.setText("For Subtitling Only - VIU");
        textFilter.setX("(main_w-144) / 16");
        textFilter.setY("(main_h-144) / 2");
        textFilter.setFontSize(144);
        textFilter.setFont(TextFilterFont.DEJAVUSANS);
        textFilter.setFontColor("#FFFFFF");
        textFilter.setAlpha(3);
        return bitmovinApi.encoding.filters.text.create(textFilter);
    }

    private static Stream createStream(
            Encoding encoding, String inputId, String inputPath, CodecConfiguration codecConfiguration)
            throws BitmovinException {

            StreamInput streamInput = new StreamInput();
            streamInput.setInputId(inputId);
            streamInput.setInputPath(inputPath);
            streamInput.setSelectionMode(StreamSelectionMode.AUTO);
            Stream stream = new Stream();
            stream.addInputStreamsItem(streamInput);
            stream.setCodecConfigId(codecConfiguration.getId());
            stream.setMode(StreamMode.STANDARD);
            return bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream);

    }
    private static H264VideoConfiguration createH264VideoConfig() throws BitmovinException {
        H264VideoConfiguration videoConfiguration240p = new H264VideoConfiguration();
        videoConfiguration240p.setName("240p");
        videoConfiguration240p.setHeight(240);
        videoConfiguration240p.setProfile(ProfileH264.HIGH);
        videoConfiguration240p.setBitrate(500000L);
        return bitmovinApi.encoding.configurations.video.h264.create(videoConfiguration240p);
    }

    private static AacAudioConfiguration createAacAudioConfig() throws BitmovinException {
        AacAudioConfiguration aacConfiguration = new AacAudioConfiguration();
        aacConfiguration.setName("32k");
        aacConfiguration.setBitrate(32000L);
        aacConfiguration.setRate(48000d);

        return bitmovinApi.encoding.configurations.audio.aac.create(aacConfiguration);
    }

    private static Mp4Muxing createMp4Muxing(
            Encoding encoding, String outputId, String outputPath, List<Stream> streams, String fileName)
            throws BitmovinException {
        Mp4Muxing muxing = new Mp4Muxing();
        muxing.addOutputsItem(buildEncodingOutput(outputId, outputPath));
        muxing.setFilename(fileName);

        for (Stream stream : streams) {
            MuxingStream muxingStream = new MuxingStream();
            muxingStream.setStreamId(stream.getId());
            muxing.addStreamsItem(muxingStream);
        }
        return bitmovinApi.encoding.encodings.muxings.mp4.create(encoding.getId(), muxing);
    }

    private static EncodingOutput buildEncodingOutput(String outputId, String outputPath) {
        AclEntry aclEntry = new AclEntry();
        aclEntry.setPermission(AclPermission.PUBLIC_READ);
        EncodingOutput encodingOutput = new EncodingOutput();
        encodingOutput.setOutputPath(outputPath);
        encodingOutput.setOutputId(outputId);
        encodingOutput.addAclItem(aclEntry);
        return encodingOutput;
    }

    private static void executeEncoding(Encoding encoding)
            throws InterruptedException, BitmovinException {
         bitmovinApi.encoding.encodings.start(encoding.getId(), new StartEncodingRequest());

                   Task task;
                   do {
                       Thread.sleep(5000);
                       task = bitmovinApi.encoding.encodings.status(encoding.getId());
                       logger.info("Encoding status is {} (progress: {} %)", task.getStatus(), task.getProgress());
                   } while (task.getStatus() != Status.FINISHED
                           && task.getStatus() != Status.ERROR);

                   if (task.getStatus() == Status.ERROR) {
                       logTaskErrors(task);
                       throw new RuntimeException("Encoding failed");
                   }
                   logger.info("Encoding finished successfully");

               }
    private static void logTaskErrors(Task task) {
        task.getMessages().stream()
                .filter(msg -> msg.getType() == MessageType.ERROR)
                .forEach(msg -> logger.error(msg.getText()));
    }
}


