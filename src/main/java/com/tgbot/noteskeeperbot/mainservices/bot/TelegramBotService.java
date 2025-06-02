package com.tgbot.noteskeeperbot.mainservices.bot;

import com.tgbot.noteskeeperbot.config.BotConfig;
import com.tgbot.noteskeeperbot.mainservices.commands.CommandsService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;


@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final CommandsService commandsService;
    private final BotConfig botConfig;

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
                commandsService.executeCommand(update, this);
            } catch (Exception e) {
                e.printStackTrace();//TODO
            }
        } else if (update.hasCallbackQuery()) {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(update.getCallbackQuery().getId());
            try {
                commandsService.executeCommand(update, this);
                execute(answer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}