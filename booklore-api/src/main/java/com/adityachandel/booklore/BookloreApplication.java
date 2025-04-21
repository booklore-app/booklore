package com.adityachandel.booklore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class BookloreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookloreApplication.class, args);
    }
}
