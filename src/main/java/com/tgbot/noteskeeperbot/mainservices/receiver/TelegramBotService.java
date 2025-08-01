package com.tgbot.noteskeeperbot.mainservices.receiver;

import com.tgbot.noteskeeperbot.config.BotConfig;
import com.tgbot.noteskeeperbot.mainservices.commands.CommandsService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            try {
                logger.info("[TelegramBotService] Пришло сообщение от пользователя...");
                commandsService.executeCommand(update, this);
            } catch (Exception e) {
                logger.error("[TelegramBotService] Не удалось принять и обработать сообщение пользователя", e);
            }

        } else if (update.hasCallbackQuery()) {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(update.getCallbackQuery().getId());
            try {
                logger.info("[TelegramBotService] Пришёл Callback-запрос от пользователя...");
                commandsService.executeCommand(update, this);
                execute(answer);
            } catch (Exception e) {
                logger.error("[TelegramBotService] Не удалось принять и обработать Callback-запрос пользователя", e);
            }
        }
    }
}