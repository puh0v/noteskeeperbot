package com.tgbot.noteskeeperbot.commands.startcommand;


import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.mainservices.bot.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import org.springframework.beans.factory.annotation.Value;
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

    private final CallbackButtons callBackButtons;
    private static final Logger logger = LoggerFactory.getLogger(StartCommand.class);

    public StartCommand(CallbackButtons callBackButtons) {
        this.callBackButtons = callBackButtons;
    }

    @Value("${bot.name}")
    private String botName;

    @Override
    public String getCommandName() {
        return "/start";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        logger.info("Началось выполнение команды {} пользователя {} ...", getCommandName(), userId);

        String text = "\uD83D\uDCDD Добро пожаловать в Notes Keeper Bot! \uD83D\uDCDD\n\n" +
                "⚠\uFE0F Бот пока в стадии разработки, многие функции ещё в процессе реализации.\n" +
                "\uD83D\uDCDA Проект создан в образовательных целях, а также для моего портфолио.\n\n" +
                "\uD83D\uDC64 Разработкой занимается: @puh0v";


        InlineKeyboardButton myNotesButton = callBackButtons.myNotesButton();
        InlineKeyboardButton addNoteButton = callBackButtons.addNoteButton();

        List<InlineKeyboardButton> myNotesRow = new ArrayList<>();
        myNotesRow.add(myNotesButton);
        List<InlineKeyboardButton> addNoteRow = new ArrayList<>();
        addNoteRow.add(addNoteButton);

        List<List<InlineKeyboardButton>> rows = List.of(myNotesRow, addNoteRow);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(rows);

        try {
            logger.info("Началась подготовка к отправке изображения пользователю {} ...", userId);

            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("static/images/paper.png");
            if (imageStream == null) {
                throw new RuntimeException("Файл \"static.images/paper.png\" не найден");
            }

            SendPhoto image = new SendPhoto();
            image.setChatId(userId.toString());
            image.setPhoto(new InputFile(imageStream, "paper.png"));
            image.setCaption(text);
            image.setReplyMarkup(inlineKeyboardMarkup);
            telegramBotService.execute(image);
        } catch (Exception e) {
            logger.error("Возникла ошибка при подготовке к отправке сообщения пользователю {} : {}", userId, e.getMessage(), e);

            SendMessage fallbackMessage = new SendMessage(userId.toString(), text);
            fallbackMessage.setReplyMarkup(inlineKeyboardMarkup);
            try {
                logger.info("Повторная подготовка к отправке изображения пользователю {} ...", userId);
                telegramBotService.execute(fallbackMessage);
            } catch (Exception e1) {
                logger.error("Возникла повторная ошибка при подготовке к отправке сообщения пользователю {} : {}", userId, e1.getMessage(), e1);
            }
        }
    }
}