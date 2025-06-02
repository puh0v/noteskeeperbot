package com.tgbot.noteskeeperbot.mainservices.messagesender;

import com.tgbot.noteskeeperbot.mainservices.bot.TelegramBotService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

@Service
public class MessageSender {

    public void sendMessageToUser(Long userId, SendMessage message, TelegramBotService telegramBotService) {
        try {
            telegramBotService.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SendImageToUser(Long userId, SendPhoto image, TelegramBotService telegramBotService) {
        try {
            telegramBotService.execute(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
