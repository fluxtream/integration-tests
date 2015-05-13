package org.fluxtream.test.integration.fluxtream_capture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class ITApplication {

    public static void main(String[] args) {
        SpringApplication.run(ITApplication.class, args);
    }

}
