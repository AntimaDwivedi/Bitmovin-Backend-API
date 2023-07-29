package com.vuclip.controller;


import com.vuclip.entity.FileEntity;

import com.vuclip.service.FileGenerationService;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("videoGeneration")
public class FileGenerationController {
    private static final Logger logger = LoggerFactory.getLogger(FileGenerationController.class);

    @ResponseStatus
    @PostMapping(value = "/listOfCids", produces = "application/json")
    public ResponseEntity<String> getPubSubMessageId(@RequestBody FileEntity fileInputs){
        try {
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<String> future = (Future<String>) service.submit(() -> {
                try {
                    logger.info("Request is accepted for " +
                            " these Cid " + fileInputs.getCids() + " and task code " + fileInputs.getTask() + " "
                            + "for Low/High Res file generation tab" + " , hit  by user " + fileInputs.getUserEmail() + " at " + Instant.now());
                    FileGenerationService.getTaskRequest(fileInputs);
                    return "Success";
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            while (!future.isDone()) {
                JSONObject resp = new JSONObject();
                String response="Request has been submitted";
                resp.put("message",response );
                return new ResponseEntity<>(resp.toString(),HttpStatus.OK);
            }

        } catch (Exception e) {
            logger.error("Exception : " + e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}



