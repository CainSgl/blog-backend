package com.cainsgl.article;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ComponentScan(basePackages = {"com.cainsgl.*"})
public class Application
{
    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }

    @Slf4j
    @Component
    public static class AppNamePrinter implements CommandLineRunner
    {
        @Value("${spring.application.name}")
        private String appName;

        @Override
        public void run(String... args)
        {
            System.out.println("\n\n\nspring.app.name: " + appName);
        }
    }
}
