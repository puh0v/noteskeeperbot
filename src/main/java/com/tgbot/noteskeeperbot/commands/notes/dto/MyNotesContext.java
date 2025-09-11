package com.tgbot.noteskeeperbot.commands.notes.dto;

import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import org.telegram.telegrambots.meta.api.objects.Update;

/** Контекст выполнения команды /my_notes*/
public record MyNotesContext (TelegramBotService telegramBotService, Update update, long userId, String userMessage) {}