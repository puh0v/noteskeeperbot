package com.tgbot.noteskeeperbot.commands.admin;

import com.tgbot.noteskeeperbot.database.entity.UsersEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.services.UserRegistration.UserRegistrationService;
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
    private final UserRegistrationService userRegistry;
    private final FlagManager flagManager;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(AdminCommand.class);

    public AdminCommand(UserRegistrationService userRegistry, FlagManager flagManager, MessageSender messageSender) {
        this.userRegistry = userRegistry;
        this.flagManager = flagManager;
        this.messageSender = messageSender;
    }

    @Override
    public String getCommandName() {
        return "/admin_command";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {

        if (userMessage.equals(getCommandName()) && userId == adminId) {
            logger.info("[AdminCommand] Поступила команда от администратора...");

            flagManager.resetFlag(userId);
            SendMessage message = new SendMessage(userId.toString(), "Отправьте текст для рассылки " +
                    "другим пользователям.\n\nДля отмены рассылки отправьте команду \"/cancel\"");

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            flagManager.setFlag(userId, getCommandName());

        } else if (flagManager.flagHasThisCommand(userId, getCommandName())) {
            logger.info("[AdminCommand] Поступил ответ администратора (по флагу)...");

            if (userMessage.equals("/cancel")) {
                logger.info("[AdminCommand] Администратор отменил рассылку пользователям.");

                SendMessage message = new SendMessage(userId.toString(), "Рассылка отменена");
                messageSender.sendMessageToUser(userId, message, telegramBotService);

                flagManager.resetFlag(userId);

            } else {
                List<UsersEntity> listOfUsers = userRegistry.getAllUsers();

                if (update.getMessage().hasText() && !update.getMessage().hasPhoto()) {
                    logger.info("[AdminCommand] Подготовка к рассылке текстового сообщения всем пользователям...");

                    for (UsersEntity user : listOfUsers) {
                        Long id = user.getUserId();
                        SendMessage message = new SendMessage(id.toString(), userMessage);
                        messageSender.sendMessageToUser(id, message, telegramBotService);
                    }
                    flagManager.resetFlag(userId);

                } else if (update.getMessage().hasPhoto()) {
                    logger.info("[AdminCommand] Подготовка к рассылке изображения...");

                    List<PhotoSize> photoList = update.getMessage().getPhoto();
                    PhotoSize largestImage = photoList.get(photoList.size() - 1);
                    String fileId = largestImage.getFileId();

                    for (UsersEntity user : listOfUsers) {
                        Long id = user.getUserId();
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
    }
}