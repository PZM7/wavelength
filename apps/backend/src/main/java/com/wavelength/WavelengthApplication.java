package com.wavelength;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WavelengthApplication {
    public static void main(String[] args) {
        SpringApplication.run(WavelengthApplication.class, args);
    }
}
