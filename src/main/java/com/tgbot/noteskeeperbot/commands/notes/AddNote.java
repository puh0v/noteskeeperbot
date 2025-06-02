package com.tgbot.noteskeeperbot.commands.notes;

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

import java.time.Instant;
import java.util.List;

@Component
public class AddNote implements Commands {

    private final NoteRepository noteRepository;
    private final FlagManager flagManager;
    private final CallbackButtons callbackButtons;
    private final MessageSender messageSender;

    public AddNote(NoteRepository noteRepository, FlagManager flagManager, CallbackButtons callbackButtons, MessageSender messageSender) {
        this.noteRepository = noteRepository;
        this.flagManager = flagManager;
        this.callbackButtons = callbackButtons;
        this.messageSender = messageSender;
    }

    @Override
    public String getCommandName() {
        return "/add_note";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        String commandName = getCommandName();

        if (userMessage.equals(getCommandName())) {
            flagManager.resetFlag(userId);
            SendMessage message = new SendMessage(userId.toString(), "✏\uFE0F Отправьте текст заметки");

            InlineKeyboardButton cancel = callbackButtons.cancelButton();
            List<InlineKeyboardButton> cancelButton = List.of(cancel);
            List<List<InlineKeyboardButton>> rows = List.of(cancelButton);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            inlineKeyboardMarkup.setKeyboard(rows);

            message.setReplyMarkup(inlineKeyboardMarkup);
            messageSender.sendMessageToUser(userId, message, telegramBotService);

            flagManager.setFlag(userId, getCommandName());

        } else if (flagManager.flagHasThisCommand(userId, getCommandName())) {

            if (!userMessage.equals(commandName) && !userMessage.equals("/cancel")) {
                addNote(userId, userMessage, telegramBotService);

                // СБРОС ФЛАГА НАХОДИТСЯ ВНУТРИ МЕТОДА

            } else if (flagManager.flagHasThisCommand(userId, getCommandName()) && userMessage.equals("/cancel")) {
                SendMessage message = new SendMessage(userId.toString(), "Вы отменили добавление заметки");

                InlineKeyboardButton mainMenu = callbackButtons.mainMenuButton();
                List<InlineKeyboardButton> mainMenuButton = List.of(mainMenu);
                List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                inlineKeyboardMarkup.setKeyboard(rows);

                message.setReplyMarkup(inlineKeyboardMarkup);
                messageSender.sendMessageToUser(userId, message, telegramBotService);

                flagManager.resetFlag(userId);
            }
        }
    }

    public void addNote(Long userId, String userMessage, TelegramBotService telegramBotService) {

        SendMessage message;

        if (userMessage.length() > 1000) {
            message = new SendMessage(userId.toString(), "Хм... Похоже, это уже не заметка, а целый роман 😅\n\n" +
            "✏\uFE0F Попробуйте сократить до 1000 символов и отправьте заметку ещё раз.");
            messageSender.sendMessageToUser(userId, message, telegramBotService);

            return;
        } else {
            message = new SendMessage(userId.toString(), "✅ Заметка успешно добавлена!");
        }
        InlineKeyboardButton addNote = callbackButtons.addNoteButton();
        addNote.setText("➕ Добавить ещё одну");

        List<InlineKeyboardButton> addNoteButton = List.of(addNote);
        List<InlineKeyboardButton> myNotesButton = List.of(callbackButtons.myNotesButton());
        List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
        List<List<InlineKeyboardButton>> rows = List.of(addNoteButton, myNotesButton, mainMenuButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            NotesEntity noteEntity = new NotesEntity();
            noteEntity.setUserId(userId);
            noteEntity.setNoteText(userMessage);
            noteEntity.setCreatedAt(Instant.now());
            noteRepository.save(noteEntity);

            messageSender.sendMessageToUser(userId, message, telegramBotService);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        flagManager.resetFlag(userId);
    }
}