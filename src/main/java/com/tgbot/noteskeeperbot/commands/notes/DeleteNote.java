package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.database.repository.NoteRepository;
import com.tgbot.noteskeeperbot.mainservices.bot.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.mainservices.messagesender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Component
public class DeleteNote implements Commands {

    private final NoteRepository noteRepository;
    private final MyNotes myNotes;
    private final FlagManager flagManager;
    private final CallbackButtons callbackButtons;
    private final NotesPageBuilder notesPageBuilder;
    private final MessageSender messageSender;

    public DeleteNote(NoteRepository noteRepository, MyNotes myNotes, FlagManager flagManager, CallbackButtons callbackButtons, NotesPageBuilder notesPageBuilder, MessageSender messageSender) {
        this.noteRepository = noteRepository;
        this.myNotes = myNotes;
        this.flagManager = flagManager;
        this.callbackButtons = callbackButtons;
        this.notesPageBuilder = notesPageBuilder;
        this.messageSender = messageSender;
    }

    @Override
    public String getCommandName() {
        return "/delete_note";
    }

    @Override
    public String getPagePrefix() {
        return "/delete_note_page_";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        List<NotesEntity> notes = myNotes.getNotesByUserId(userId);

        if (userMessage.equals(getCommandName())) {
            if (notes.isEmpty()) {
                SendMessage message = notesPageBuilder.getNotesIsEmptyMessage(userId);
                messageSender.sendMessageToUser(userId, message, telegramBotService);

            } else if (!notes.isEmpty()) {
                flagManager.resetFlag(userId);

                SendMessage message = getReadyPageWithNotes(userId, 0, getPagePrefix());

                messageSender.sendMessageToUser(userId, message, telegramBotService);
                flagManager.setFlag(userId, getCommandName());
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();

            if (data.startsWith(getPagePrefix())) {
                int page = Integer.parseInt(data.replace(getPagePrefix(), ""));
                SendMessage message = getReadyPageWithNotes(userId, page, getPagePrefix());

                messageSender.sendMessageToUser(userId, message, telegramBotService);
                return;
            }
        }

        // --------------- Обрабатываем ответы боту по флагу ----------------------
        if (flagManager.flagHasThisCommand(userId, getCommandName())) {
            if (userMessage.matches("\\d+")) {
                deleteNote(userId, userMessage, notes, telegramBotService);

                // СБРОС ФЛАГА НАХОДИТСЯ ВНУТРИ МЕТОДА

            } else if (userMessage.equals("/cancel")) {
                SendMessage cancelMessage = new SendMessage(userId.toString(), "\uD83D\uDEAB Вы отменили удаление заметки");

                List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
                List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                inlineKeyboardMarkup.setKeyboard(rows);

                cancelMessage.setReplyMarkup(inlineKeyboardMarkup);

                messageSender.sendMessageToUser(userId, cancelMessage, telegramBotService);
                flagManager.resetFlag(userId);
            }
        }
    }

    public void deleteNote(Long userId, String userMessage, List<NotesEntity> notes, TelegramBotService telegramBotService) {
        int number = Integer.parseInt(userMessage) - 1;

        if (number >= 0 && number < notes.size()) {
            NotesEntity noteToDelete = notes.get(number);
            noteRepository.delete(noteToDelete);

            SendMessage message = new SendMessage(userId.toString(), "✅ Заметка успешно удалена!");

            List<InlineKeyboardButton> deleteButtonRow = List.of(callbackButtons.continueDeletingButton());
            List<InlineKeyboardButton> mainMenuButtonRow = List.of(callbackButtons.mainMenuButton());
            List<List<InlineKeyboardButton>> rows = List.of(deleteButtonRow, mainMenuButtonRow);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            inlineKeyboardMarkup.setKeyboard(rows);

            message.setReplyMarkup(inlineKeyboardMarkup);

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            flagManager.resetFlag(userId);
        } else {
            SendMessage message = new SendMessage(userId.toString(), "❌ Такой заметки не существует. Выберите другую.");

            List<InlineKeyboardButton> cancelButton = List.of(callbackButtons.cancelButton());
            List<List<InlineKeyboardButton>> rows = List.of(cancelButton);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            inlineKeyboardMarkup.setKeyboard(rows);

            message.setReplyMarkup(inlineKeyboardMarkup);
            messageSender.sendMessageToUser(userId, message, telegramBotService);
        }
    }


    private SendMessage getReadyPageWithNotes(Long userId, Integer page, String pagePrefix) {
        NotesPageDTO notesPageDTO = notesPageBuilder.getFieldsFromDTO(userId, page, pagePrefix, NotesViewMode.SELECTABLE);
        String textFromDTO = notesPageDTO.getText();

        String finalText;
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(notesPageDTO.getKeyboard());

        if (notesPageDTO.getKeyboard().isEmpty() && page > 0) {
            finalText = textFromDTO;
            keyboard.add(List.of(callbackButtons.mainMenuButton()));
        } else {
            finalText = (page == 0)
                    ? "\uD83D\uDDD1 Отправьте номер той заметки, которую хотите удалить.\n\n\n" + textFromDTO
                    : textFromDTO;
            keyboard.add(List.of(callbackButtons.cancelButton()));
        }

        SendMessage message = new SendMessage(userId.toString(), finalText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }
}