package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.database.repository.NoteRepository;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.services.messagesender.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;

@Component
public class AddNote implements Commands {

    private final NoteRepository noteRepository;
    private final FlagManager flagManager;
    private final CallbackButtons callbackButtons;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(AddNote.class);

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

        if (userMessage.equals(getCommandName())) {
            logger.info("[AddNote] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

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

            if (userMessage.equals("/cancel")) {
                logger.info("[AddNote] Пользователь {} отменил добавление заметки. Формирую сообщение для ответа...", userId);

                SendMessage message = new SendMessage(userId.toString(), "Вы отменили добавление заметки");

                InlineKeyboardButton mainMenu = callbackButtons.mainMenuButton();
                List<InlineKeyboardButton> mainMenuButton = List.of(mainMenu);
                List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                inlineKeyboardMarkup.setKeyboard(rows);

                message.setReplyMarkup(inlineKeyboardMarkup);
                messageSender.sendMessageToUser(userId, message, telegramBotService);

                flagManager.resetFlag(userId);

            } else {
                logger.info("[AddNote] Пользователь {} прислал заметку...", userId);

                addNote(userId, userMessage, telegramBotService);

                // СБРОС ФЛАГА НАХОДИТСЯ ВНУТРИ МЕТОДА
            }
        }
    }

    public void addNote(Long userId, String userMessage, TelegramBotService telegramBotService) {
        logger.info("[AddNote] Проверяю заметку пользователя {} на соответствие правилам добавления заметок...", userId);

        SendMessage message;

        if (userMessage.length() > 1000) {
            logger.info("[AddNote] Заметка пользователя {} прешывает 1000 символов. Формирую сообщение с ответом...", userId);

            message = new SendMessage(userId.toString(), "Хм... Похоже, это уже не заметка, а целый роман 😅\n\n" +
            "✏\uFE0F Попробуйте сократить до 1000 символов и отправьте заметку ещё раз.");

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            return;

        } else {
            logger.info("[AddNote] Одобрено добавление заметки для пользователя {} . Подготавливаю сообщение для ответа...", userId);

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

        logger.info("[AddNote] Добавляю заметку пользователя {} в БД...", userId);
        try {
            NotesEntity noteEntity = new NotesEntity();
            noteEntity.setUserId(userId);
            noteEntity.setNoteText(userMessage);
            noteEntity.setCreatedAt(Instant.now());
            noteRepository.save(noteEntity);

        } catch (Exception e) {
            logger.error("[AddNote] Произошла ошибка во время добавления заметки в БД и отправки ответа пользователю {} : {}", userId, e.getMessage(), e);
            return;
        }
        logger.info("[AddNote] Заметка пользователя {} добавлена в БД!", userId);

        messageSender.sendMessageToUser(userId, message, telegramBotService);
        flagManager.resetFlag(userId);
    }
}