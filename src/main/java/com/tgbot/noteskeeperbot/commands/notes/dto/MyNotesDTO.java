package com.tgbot.noteskeeperbot.commands.notes.dto;

import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import org.telegram.telegrambots.meta.api.objects.Update;

/** Класс для хранения данных о пользователе. Основная цель: разгрузить параметры методов в классе MyNotes */
public class MyNotesDTO {
    private final TelegramBotService telegramBotService;
    private final Update update;
    private final long userId;
    private final String userMessage;

    public MyNotesDTO(TelegramBotService telegramBotService, Update update, long userId, String userMessage) {
        this.telegramBotService = telegramBotService;
        this.update = update;
        this.userId = userId;
        this.userMessage = userMessage;
    }

    public TelegramBotService getTelegramBotService() {
        return telegramBotService;
    }

    public Update getUpdate() {
        return update;
    }

    public long getUserId() {
        return userId;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
