package com.tgbot.noteskeeperbot.services.messagesender;

import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Service
public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    public void sendMessageToUser(Long userId, SendMessage message, TelegramBotService telegramBotService) {
        try {
            logger.info("[MessageSender] Начинаю отправку текстового сообщения пользователю {} ...", userId);
            telegramBotService.execute(message);
            logger.info("[MessageSender] Отправка текстового сообщения пользователю {} выполнена успешно!", userId);
        } catch (TelegramApiException e) {
            logger.error("[MessageSender] Ошибка при отправке текстового сообщения пользователю {} : {}", userId, e.getMessage(), e);
        }
    }

    public void sendImageToUser(Long userId, SendPhoto image, TelegramBotService telegramBotService) {
        try {
            logger.info("[MessageSender] Начинаю отправку изображения пользователю {} ...", userId);
            telegramBotService.execute(image);
            logger.info("[MessageSender] Отправка изображения пользователю {} выполнена успешно!", userId);
        } catch (TelegramApiException e) {
            logger.error("[MessageSender] Ошибка при отправке изображения пользователю {} : {}", userId, e.getMessage(), e);
        }
    }

    public SendMessage createMessage(Long userId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(userId.toString())
                .text(text)
                .build();
        return message;
    }
}