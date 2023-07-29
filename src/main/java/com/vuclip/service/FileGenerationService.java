package com.vuclip.service;

import com.bitmovin.api.sdk.common.BitmovinException;
import com.vuclip.entity.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class FileGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(LowResImpl.class);
    public static String INVALID_MSG = "Invalid Cid";

    public static void getTaskRequest(FileEntity fileInputs) throws IOException, InterruptedException, BitmovinException {
        List<String> msg_id_list = new ArrayList<>();
        List<String> cid_list = new ArrayList<>();
        String cids = fileInputs.getCids();
        for (String cid : cids.split(",")) {
            if (cid.isEmpty() || cid.length() == 0 || cid.matches(".*[a-zA-Z]+.*") || cid.matches(".*[!@#$%&*()_+=|<>?{}~[//]/].*") || cid.contains(".") || cid.contains("\\") || cid.matches("0")) {
                logger.info("Cid  is not valid :" + cid);
                cid_list.add(cid);
                msg_id_list.add(INVALID_MSG);
                logger.info("Request is not processed");
            }
            cid_list.add(cid);
            logger.info("cid of request is  " + cid);
            if (fileInputs.getTask().equals("LOW_RES")) {
                LowResImpl.script(cid);
            } else {
                HighResImpl.script(cid);
            }
        }
    }

}
