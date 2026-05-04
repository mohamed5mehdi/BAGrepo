package com.pfe.gestionsachat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class GestionsAchatApplication {
    public static void main(String[] args) {
        SpringApplication.run(GestionsAchatApplication.class, args);
        System.out.println("🚀 [VERSION 2.1] - FIX FOR EACH ACTIVATED - PORT 8080");
    }
}