package com.yuemo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yuemo"})
public class YuemoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuemoServerApplication.class, args);
    }
}
