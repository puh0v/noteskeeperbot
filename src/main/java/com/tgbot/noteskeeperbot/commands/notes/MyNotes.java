package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.mainservices.bot.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.notes.services.NoteService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.mainservices.messagesender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import java.util.ArrayList;
import java.util.List;

@Component
public class MyNotes implements Commands {

    private final NoteService noteService;
    private final CallbackButtons callbackButtons;
    private final NotesPageBuilder notesPageBuilder;
    private final MessageSender messageSender;

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
            if (notes.isEmpty()) {
                SendMessage message = notesPageBuilder.getNotesIsEmptyMessage(userId);
                messageSender.sendMessageToUser(userId, message, telegramBotService);
            } else if (!notes.isEmpty()) {
                SendMessage message = getReadyPageWithNotes(userId, 0, getPagePrefix());

                messageSender.sendMessageToUser(userId, message, telegramBotService);
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();

            if (data.startsWith(getPagePrefix())) {
                int page = Integer.parseInt(data.replace(getPagePrefix(), ""));
                SendMessage message = getReadyPageWithNotes(userId, page, getPagePrefix());

                messageSender.sendMessageToUser(userId, message, telegramBotService);
            }
        }
    }

    private SendMessage getReadyPageWithNotes(Long userId, Integer page, String pagePrefix) {
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
        return message;
    }

    public List<NotesEntity> getNotesByUserId(Long userId) {
        return noteService.getAllUserNotes(userId);
    }
}