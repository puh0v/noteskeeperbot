package com.tgbot.noteskeeperbot.commands;


import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Commands {

    String getCommandName();

    default String getPagePrefix() {
        return "false";
    }

    void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService);
}
