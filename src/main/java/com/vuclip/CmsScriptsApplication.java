package com.vuclip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.io.IOException;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CmsScriptsApplication {


    private static final Logger logger = LoggerFactory.getLogger(CmsScriptsApplication.class);

    public static void main(String[] args) throws IOException {

        SpringApplication.run(CmsScriptsApplication.class, args);


    }


}
