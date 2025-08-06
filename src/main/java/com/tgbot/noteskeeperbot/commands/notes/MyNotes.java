package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.services.noteservice.NoteService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.services.messagesender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


import java.util.ArrayList;
import java.util.List;

@Component
public class MyNotes implements Commands {

    private final NoteService noteService;
    private final CallbackButtons callbackButtons;
    private final NotesPageBuilder notesPageBuilder;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(MyNotes.class);

    public MyNotes(NoteService noteService, CallbackButtons callbackButtons, NotesPageBuilder notesPageBuilder, MessageSender messageSender) {
        this.noteService = noteService;
        this.callbackButtons = callbackButtons;
        this.notesPageBuilder = notesPageBuilder;
        this.messageSender = messageSender;
    }

    @Override
    public String getCommandName() {
        return "/my_notes";
    }

    @Override
    public String getPagePrefix() {
        return "/mynotes_page_";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);

        if (userMessage.equals(getCommandName())) {
            logger.info("[MyNotes] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

            if (notes.isEmpty()) {
                logger.info("[MyNotes] Список заметок пользователя {} пуст.", userId);

                SendMessage message = notesPageBuilder.getNotesIsEmptyMessage(userId);
                messageSender.sendMessageToUser(userId, message, telegramBotService);
            } else {
                logger.info("[MyNotes] Подготавливаю список заметок для пользователя {} ...", userId);

                SendMessage message = getReadyPageWithNotes(userId, 0, getPagePrefix());
                messageSender.sendMessageToUser(userId, message, telegramBotService);
            }
        } else if (update.hasCallbackQuery()) {
            logger.info("[MyNotes] Поступил Callback-запрос от пользователя {} ...", userId);
            String data = update.getCallbackQuery().getData();

            if (data.startsWith(getPagePrefix())) {
                logger.info("[MyNotes] Пользователь {} перелистывает страницу с заметками...", userId);
                int page = Integer.parseInt(data.replace(getPagePrefix(), ""));

                SendMessage message = getReadyPageWithNotes(userId, page, getPagePrefix());
                messageSender.sendMessageToUser(userId, message, telegramBotService);
            }
        }
    }


    private SendMessage getReadyPageWithNotes(Long userId, Integer page, String pagePrefix) {
        logger.info("[MyNotes] Подготавливаю сообщение с заметками для пользователя {} ...", userId);

        NotesPageDTO notesPageDTO = notesPageBuilder.getFieldsFromDTO(userId, page, pagePrefix, NotesViewMode.PREVIEW);
        String textFromDTO = notesPageDTO.getText();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(notesPageDTO.getKeyboard());

        if (notesPageDTO.getKeyboard().isEmpty() && page > 0) {
            keyboard.add(List.of(callbackButtons.mainMenuButton()));

        } else {
            keyboard.add(List.of(callbackButtons.deleteNoteButton(), callbackButtons.shareNoteButton()));
            keyboard.add(List.of(callbackButtons.mainMenuButton()));
        }

        SendMessage message = new SendMessage(userId.toString(), textFromDTO);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(inlineKeyboardMarkup);
        logger.info("[MyNotes] Страница с заметками для пользователя {} готова!", userId);

        return message;
    }

    public List<NotesEntity> getNotesByUserId(Long userId) {
        return noteService.getAllUserNotes(userId);
    }
}