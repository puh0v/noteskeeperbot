package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.MyNotesDTO;
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
        MyNotesDTO myNotesDTO = new MyNotesDTO(telegramBotService, update, userId, userMessage);
        prepareMessage(myNotesDTO);
    }


    private void prepareMessage(MyNotesDTO myNotesDTO) {
        Long userId = myNotesDTO.getUserId();
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);

        if (myNotesDTO.getUserMessage().equals(getCommandName())) {
            logger.info("[MyNotes] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);
            handleCommand(notes, myNotesDTO);
        }
        else if (myNotesDTO.getUpdate().hasCallbackQuery()) {
            logger.info("[MyNotes] Поступил Callback-запрос от пользователя {} ...", userId);
            handleCallbackQuery(myNotesDTO);
        }
    }


    private void handleCommand(List<NotesEntity> notes, MyNotesDTO myNotesDTO) {
        Long userId = myNotesDTO.getUserId();

        if (notes.isEmpty()) {
            logger.info("[MyNotes] Список заметок пользователя {} пуст.", userId);

            sendEmptyPage(myNotesDTO);
        } else {
            logger.info("[MyNotes] Подготавливаю список заметок для пользователя {} ...", userId);

            sendReadyPage(myNotesDTO, 0);
        }
    }

    private void handleCallbackQuery(MyNotesDTO myNotesDTO) {
        Long userId = myNotesDTO.getUserId();
        String data = myNotesDTO.getUpdate().getCallbackQuery().getData();
        SendMessage message = new SendMessage();

        if (data.startsWith(getPagePrefix())) {
            logger.info("[MyNotes] Пользователь {} перелистывает страницу с заметками...", userId);
            int page = Integer.parseInt(data.replace(getPagePrefix(), ""));

            message = getReadyPage(myNotesDTO, page, message);
            messageSender.sendMessageToUser(userId, message, myNotesDTO.getTelegramBotService());
        }
    }


    private void sendReadyPage(MyNotesDTO myNotesDTO, int page) {
        SendMessage message = new SendMessage();

        message = getReadyPage(myNotesDTO, page, message);
        messageSender.sendMessageToUser(myNotesDTO.getUserId(), message, myNotesDTO.getTelegramBotService());
    }

    private void sendEmptyPage(MyNotesDTO myNotesDTO) {
        SendMessage message = notesPageBuilder.getEmptyMessage(myNotesDTO.getUserId());
        messageSender.sendMessageToUser(myNotesDTO.getUserId(), message, myNotesDTO.getTelegramBotService());
    }


    private SendMessage getReadyPage(MyNotesDTO myNotesDTO, int page, SendMessage message) {
        Long userId = myNotesDTO.getUserId();

        logger.info("[MyNotes] Подготавливаю сообщение с заметками для пользователя {} ...", userId);

        NotesPageDTO notesPageDTO = notesPageBuilder.getFieldsFromDTO(userId, page, getPagePrefix(), NotesViewMode.PREVIEW);
        String textFromDTO = notesPageDTO.getText();

        message.setChatId(userId.toString());
        message.setText(textFromDTO);

        InlineKeyboardMarkup inlineKeyboardMarkup = getPaginationAndNotes(page, notesPageDTO);
        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[MyNotes] Страница с заметками для пользователя {} готова!", userId);
        return message;
    }


    private InlineKeyboardMarkup getPaginationAndNotes(int page, NotesPageDTO notesPageDTO) {

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(notesPageDTO.getKeyboard());

        if (notesPageDTO.getKeyboard().isEmpty() && page > 0) {
            keyboard.add(List.of(callbackButtons.mainMenuButton()));
        } else {
            keyboard.add(List.of(callbackButtons.deleteNoteButton(), callbackButtons.shareNoteButton()));
            keyboard.add(List.of(callbackButtons.mainMenuButton()));
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        return inlineKeyboardMarkup;
    }


    public List<NotesEntity> getNotesByUserId(Long userId) {
        return noteService.getAllUserNotes(userId);
    }
}