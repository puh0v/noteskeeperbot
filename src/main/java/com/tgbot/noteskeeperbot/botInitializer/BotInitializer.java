package com.tgbot.noteskeeperbot.botInitializer;

import com.tgbot.noteskeeperbot.mainservices.receiver.TelegramBotService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Component
public class BotInitializer {

    private final TelegramBotService telegramBotService;
    private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);

    public BotInitializer(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("[BotInitializer] Началась инициализация бота...");

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
        } catch (Exception e) {
            logger.error("[BotInitializer] Ошибка при инициализации бота: {}", e.getMessage(), e);
            return;
        }

        logger.info("[BotInitializer] Инициализация бота прошла успешно!");
    }
}