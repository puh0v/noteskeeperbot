package com.tgbot.noteskeeperbot.botInitializer;

import com.tgbot.noteskeeperbot.mainservices.bot.TelegramBotService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer {

    // Инициализируем бота:
    private final TelegramBotService telegramBotService;

    public BotInitializer(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            e.toString();
        }
    }
}