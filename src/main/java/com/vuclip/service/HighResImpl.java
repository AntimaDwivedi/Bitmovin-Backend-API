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

@Component
@ConfigurationProperties(prefix = "bitmovinconfig")
public class HighResImpl {
    private static final Logger logger = LoggerFactory.getLogger(HighResImpl.class);
    public static String apiKey;

    public static String encVersion;

    public static String inputId;

    public static String outputId;


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

    public static void script(String CID) throws BitmovinException, InterruptedException, IOException {

        logger.info("Establishing connection with bitmovin ");
        bitmovinApi = BitmovinApi.builder()
                .withApiKey(getApiKey())
                .build();

        logger.info("Connection with bitmovin has been established");

        Encoding encoding = createEncoding("Encoding for high resolution ", "");

        String inputId = getInputId();

        String inputFilePath = CID + ".mp4";


        String outputId = getOutputId();

        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);

        String outputPath = "HIGH-RESOLUTION/" + CID +  "_V" + utc.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "/";


        H264VideoConfiguration h264Config = createH264VideoConfig();
        AacAudioConfiguration aacConfig = createAacAudioConfig();

        Stream videoStream = createStream(encoding, inputId, inputFilePath, h264Config);
        Stream audioStream = createStream(encoding, inputId, inputFilePath, aacConfig);

        List<Filter> filters = new ArrayList<>();
        filters.add(createDeinterlaceFilter());

        createStreamFilterList(encoding, videoStream, filters);

        createMp4Muxing(encoding, outputId,outputPath, Arrays.asList(videoStream, audioStream), CID + "_high_res.mp4");
        executeEncoding(encoding);

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

    private static DeinterlaceFilter createDeinterlaceFilter() throws BitmovinException {
        return bitmovinApi.encoding.filters.deinterlace.create(new DeinterlaceFilter());
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
        H264VideoConfiguration videoConfiguration1080p = new H264VideoConfiguration();
        videoConfiguration1080p.setName("1080p");
        videoConfiguration1080p.setHeight(1080);
        videoConfiguration1080p.setBitrate(3000000L);
        videoConfiguration1080p.setProfile(ProfileH264.HIGH);

        return bitmovinApi.encoding.configurations.video.h264.create(videoConfiguration1080p);
    }

    private static AacAudioConfiguration createAacAudioConfig() throws BitmovinException {
        AacAudioConfiguration aacConfiguration = new AacAudioConfiguration();
        aacConfiguration.setName("128k");
        aacConfiguration.setBitrate(128000L);
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

