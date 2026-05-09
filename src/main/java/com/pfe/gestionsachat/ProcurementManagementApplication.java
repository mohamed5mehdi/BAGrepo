package com.pfe.gestionsachat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ProcurementManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcurementManagementApplication.class, args);
        System.out.println("🚀 [VERSION 2.1] - PROCUREMENT SYSTEM ACTIVATED - PORT 8080");
    }
}