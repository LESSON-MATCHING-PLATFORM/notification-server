package com.kosa.noticeserver;

import com.kosa.noticeserver.config.EnvConfigInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class NoticeServerApplication {
    public static void main(String[] args) { SpringApplication.run(NoticeServerApplication.class, args); }
}
