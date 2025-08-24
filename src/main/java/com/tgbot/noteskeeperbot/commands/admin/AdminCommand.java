package com.tgbot.noteskeeperbot.commands.admin;

import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.services.registration.UserRegistrationService;
import com.tgbot.noteskeeperbot.services.messagesender.MessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

@Component
public class AdminCommand implements Commands {

    @Value("${admin.id}")
    private long adminId;
    private final UserRegistrationService userRegistrationService;
    private final FlagManager flagManager;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(AdminCommand.class);

    public AdminCommand(UserRegistrationService userRegistrationService, FlagManager flagManager, MessageSender messageSender) {
        this.userRegistrationService = userRegistrationService;
        this.flagManager = flagManager;
        this.messageSender = messageSender;
    }

    @Override
    public String getCommandName() {
        return "/admin_command";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        prepareMessage(telegramBotService, update, userId, userMessage);
    }

    /** Метод prepareMessage() определяет, какой блок кода нужно вызвать
     * в зависимости от запроса админа.*/
    private void prepareMessage(TelegramBotService telegramBotService, Update update, Long userId, String userMessage) {
        if (userMessage.equals(getCommandName()) && userId == adminId) {
            handleCommand(telegramBotService, userId);

        } else if (flagManager.flagHasThisCommand(userId, getCommandName())) {
            logger.info("[AdminCommand] Поступил ответ администратора (по флагу)...");

            if (userMessage.equals("/cancel")) {
                logger.info("[AdminCommand] Администратор отменил рассылку пользователям.");

                SendMessage message = messageSender.createMessage(userId, "Рассылка отменена");

                messageSender.sendMessageToUser(userId, message, telegramBotService);
                flagManager.resetFlag(userId);

            } else {
                sendMessage(telegramBotService, update, userId, userMessage);
            }
        }
    }

    /** Обработка команды /admin_command */
    private void handleCommand(TelegramBotService telegramBotService, Long userId) {
        logger.info("[AdminCommand] Поступила команда от администратора...");
        flagManager.resetFlag(userId);

        SendMessage message = messageSender.createMessage(userId, "Отправьте текст для рассылки " +
                "другим пользователям.\n\nДля отмены рассылки отправьте команду \"/cancel\"");

        messageSender.sendMessageToUser(userId, message, telegramBotService);
        flagManager.setFlag(userId, getCommandName());
    }

    /** Метод для рассылки сообщения (с изображением или без) */
    private void sendMessage(TelegramBotService telegramBotService, Update update, Long userId, String userMessage) {
        List<Long> listOfUsers = userRegistrationService.getAllUserIds();

        if (update.getMessage().hasText() && !update.getMessage().hasPhoto()) {
            logger.info("[AdminCommand] Подготовка к рассылке текстового сообщения всем пользователям...");

            for (int i = 0; i < listOfUsers.size(); i++) {
                Long id = listOfUsers.get(i);
                SendMessage message = messageSender.createMessage(id, userMessage);
                messageSender.sendMessageToUser(id, message, telegramBotService);
            }
            flagManager.resetFlag(userId);

        } else if (update.getMessage().hasPhoto()) {
            logger.info("[AdminCommand] Подготовка к рассылке изображения...");

            List<PhotoSize> photoList = update.getMessage().getPhoto();
            PhotoSize largestImage = photoList.get(photoList.size() - 1);
            String fileId = largestImage.getFileId();

            for (int i = 0; i < listOfUsers.size(); i++) {
                Long id = listOfUsers.get(i);

                try {
                    SendPhoto image = new SendPhoto();
                    image.setChatId(id);
                    image.setPhoto(new InputFile(fileId));
                    image.setCaption(userMessage);

                    messageSender.sendImageToUser(id, image, telegramBotService);
                } catch (Exception e) {
                    logger.error("[AdminCommand] Не удалось отправить сообщение пользователю {} : {}", id, e.getMessage(), e);
                }
            }
            flagManager.resetFlag(userId);
        }
    }
}