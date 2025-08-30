package com.tgbot.noteskeeperbot.services.receiver;

import com.tgbot.noteskeeperbot.config.BotConfig;
import com.tgbot.noteskeeperbot.services.commands.CommandsService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

/** Основной сервис Telegram-бота. Принимает обновления от Telegram API (сообщения или callback-запросы)
 * и передаёт их в CommandsService для дальнейшей обработки.*/
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final CommandsService commandsService;
    private final BotConfig botConfig;
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    public TelegramBotService(CommandsService commandsService, BotConfig botConfig) {
        this.commandsService = commandsService;
        this.botConfig = botConfig;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    /** Обрабатывает входящее обновление от Telegram (сообщение или Callback)*/
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            logger.info("[TelegramBotService] Получен запрос от пользователя...");

            if (update.hasCallbackQuery()) {
                //Сбрасываем состояние "загрузки" у inline-кнопок (визуал)
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(update.getCallbackQuery().getId());
                try {
                    execute(answer);
                } catch (TelegramApiException e) {
                    logger.error("Не удалось сбросить загрузку у inline-кнопки...", e);
                }
            }

            commandsService.executeCommand(update, this);
        }
    }
}