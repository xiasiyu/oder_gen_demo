package com.tw.darkhorse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DarkhorseApplication {
    public static void main(String[] args) {
        SpringApplication.run(DarkhorseApplication.class, args);
    }
}
