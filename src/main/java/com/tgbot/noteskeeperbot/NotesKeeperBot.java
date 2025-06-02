package com.tgbot.noteskeeperbot;


import com.tgbot.noteskeeperbot.config.BotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class NotesKeeperBot {
    public static void main(String[] args) {
        SpringApplication.run(NotesKeeperBot.class, args);
    }
}