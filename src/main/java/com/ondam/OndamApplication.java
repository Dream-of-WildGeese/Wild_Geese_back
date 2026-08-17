package com.ondam;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OndamApplication {

    public static void main(String[] args) {
        SpringApplication.run(OndamApplication.class, args);
    }

}
