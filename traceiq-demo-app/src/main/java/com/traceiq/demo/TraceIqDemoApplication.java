package com.traceiq.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TraceIqDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceIqDemoApplication.class, args);
    }
}
