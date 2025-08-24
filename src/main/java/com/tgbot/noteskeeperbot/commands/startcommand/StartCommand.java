package com.tgbot.noteskeeperbot.commands.startcommand;


import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.config.BotConfig;
import com.tgbot.noteskeeperbot.services.messagesender.ImageLoader;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.services.messagesender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class StartCommand implements Commands {

    private final CallbackButtons callbackButtons;
    private final MessageSender messageSender;
    private final BotConfig botConfig;
    private final ImageLoader imageLoader;
    private static final String IMAGE_PATH = "images/paper.png";
    private static final Logger logger = LoggerFactory.getLogger(StartCommand.class);

    public StartCommand(CallbackButtons callbackButtons, MessageSender messageSender, BotConfig botConfig, ImageLoader imageLoader) {
        this.callbackButtons = callbackButtons;
        this.messageSender = messageSender;
        this.botConfig = botConfig;
        this.imageLoader = imageLoader;
    }

    @Override
    public String getCommandName() {
        return "/start";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        logger.info("[StartCommand] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

        prepareMessage(telegramBotService, userId);
    }

    /** Метод prepareMessage() отвечает за подготовку данных для формирования сообщения. */
    private void prepareMessage(TelegramBotService telegramBotService, Long userId) {
        String text = welcomeText();
        InlineKeyboardMarkup inlineKeyboardMarkup = prepareButtons();
        sendMessage(telegramBotService, userId, text, inlineKeyboardMarkup);
    }

    /** Подготовка текста для сообщения в главном меню. */
    private String welcomeText() {
        String text = "\uD83D\uDCDD Добро пожаловать в " + botConfig.getBotUsername() + "! \uD83D\uDCDD\n\n" +
                "⚠\uFE0F Бот пока в стадии разработки, многие функции ещё в процессе реализации.\n" +
                "\uD83D\uDCDA Проект создан в образовательных целях, а также для моего портфолио.\n\n" +
                "\uD83D\uDC64 Разработкой занимается: @puh0v";
        return text;
    }

    /** Методы для финальной подготовки и отправки сообщения */
    private void sendMessage(TelegramBotService telegramBotService, Long userId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        logger.info("[StartCommand] Начинаю подготовку изображения для пользователя {} ...", userId);
        SendPhoto image = new SendPhoto();
        image.setChatId(userId.toString());

        try {
            InputStream imageStream = imageLoader.getImageStream(IMAGE_PATH);

            if (imageStream == null) {
                throw new RuntimeException("Файл \"" + IMAGE_PATH + "\" не найден");
            }

            image.setPhoto(new InputFile(imageStream, "paper.png"));
            image.setCaption(text);
            image.setReplyMarkup(inlineKeyboardMarkup);

        } catch (RuntimeException e) {
            logger.error("[StartCommand] Возникла ошибка при подготовке изображения для пользователя {} : {}", userId, e.getMessage(), e);

            SendMessage fallbackMessage = messageSender.createMessage(userId, text);
            fallbackMessage.setReplyMarkup(inlineKeyboardMarkup);

            logger.info("[StartCommand] Повторная подготовка изображения для пользователя {} ...", userId);
            try {
                messageSender.sendMessageToUser(userId, fallbackMessage, telegramBotService);
            } catch (Exception e1) {
                logger.error("[StartCommand] Возникла повторная ошибка при подготовке изображения для пользователя {} : {}", userId, e1.getMessage(), e1);
            }
            return;
        }

        messageSender.sendImageToUser(userId, image, telegramBotService);
    }

    /** Метод для создания кнопок в главном меню. */
    private InlineKeyboardMarkup prepareButtons() {
        InlineKeyboardButton myNotesButton = callbackButtons.myNotesButton();
        InlineKeyboardButton addNoteButton = callbackButtons.addNoteButton();

        List<InlineKeyboardButton> myNotesRow = new ArrayList<>();
        myNotesRow.add(myNotesButton);
        List<InlineKeyboardButton> addNoteRow = new ArrayList<>();
        addNoteRow.add(addNoteButton);

        List<List<InlineKeyboardButton>> rows = List.of(myNotesRow, addNoteRow);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(rows);

        return inlineKeyboardMarkup;
    }
}