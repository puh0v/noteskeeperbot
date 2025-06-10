package com.tgbot.noteskeeperbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bot")
public class BotConfig {
    private String name;
    private String token;

    public String getBotUsername() {
        return name;
    }

    public String getBotToken() {
        return token;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setToken(String token) {
        this.token = token;
    }
}