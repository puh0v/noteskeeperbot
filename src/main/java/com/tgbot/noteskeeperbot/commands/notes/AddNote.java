package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.services.noteservice.NoteService;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.services.messagesender.MessageSender;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/** Класс для добавления новой заметки пользователя. */
@Component
public class AddNote implements Commands {

    private final NoteService noteService;
    private final FlagManager flagManager;
    private final CallbackButtons callbackButtons;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(AddNote.class);

    public AddNote(NoteService noteService, FlagManager flagManager, CallbackButtons callbackButtons, MessageSender messageSender) {
        this.noteService = noteService;
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
        prepareMessage(telegramBotService, userId, userMessage);
    }

    /** Метод prepareMessage() определяет, какой блок кода нужно вызвать
     * в зависимости от запроса пользователя.
     * Например, если пользователь повторно ввёл команду, отменил добавление заметки, и т.д.*/
    private void prepareMessage(TelegramBotService telegramBotService, Long userId, String userMessage) {
        if (userMessage.equals(getCommandName())) {
            logger.info("[AddNote] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

            handleCommand(telegramBotService, userId);

        } else if (flagManager.flagContainsCommand(userId, getCommandName())) {
            if (userMessage.equals("/cancel")) {
                logger.info("[AddNote] Пользователь {} отменил добавление заметки. Формирую сообщение для ответа...", userId);

                SendMessage message = messageSender.createMessage(userId, "Вы отменили добавление заметки");

                InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsInCaseOfCancel();
                message.setReplyMarkup(inlineKeyboardMarkup);

                messageSender.sendMessageToUser(userId, message, telegramBotService);
                flagManager.resetFlag(userId);

            } else {
                logger.info("[AddNote] Пользователь {} прислал заметку...", userId);
                addNote(userId, userMessage, telegramBotService);
            }
        }
    }

    /** Обработка команды /add_note */
    private void handleCommand(TelegramBotService telegramBotService, Long userId) {
        flagManager.resetFlag(userId);
        SendMessage message = messageSender.createMessage(userId, "✏\uFE0F Отправьте текст заметки");

        InlineKeyboardMarkup inlineKeyboardMarkup = getCancelButton();
        message.setReplyMarkup(inlineKeyboardMarkup);

        messageSender.sendMessageToUser(userId, message, telegramBotService);
        flagManager.setFlag(userId, this);
    }

    /** Метод для работы с заметкой пользователя */
    private void addNote(Long userId, String userMessage, TelegramBotService telegramBotService) {
        logger.info("[AddNote] Проверяю заметку пользователя {} на соответствие правилам добавления заметок...", userId);

        if (userMessage.length() > 1000) {
            logger.info("[AddNote] Заметка пользователя {} превышает 1000 символов. Формирую сообщение с ответом...", userId);

            SendMessage message = messageSender.createMessage(userId, "Хм... Похоже, это уже не заметка, а целый роман 😅\n\n" +
                    "✏\uFE0F Попробуйте сократить до 1000 символов и отправьте заметку ещё раз.");

            messageSender.sendMessageToUser(userId, message, telegramBotService);

        } else if (!userMessage.isBlank()) {
            saveNote(telegramBotService, userId, userMessage);
        }
    }

    /** Сохраняем заметку в БД*/
    private void saveNote(TelegramBotService telegramBotService, Long userId, String userMessage) {
        logger.info("[AddNote] Одобрено добавление заметки для пользователя {} . Подготавливаю сообщение для ответа...", userId);

        try {
            noteService.saveNote(userId, userMessage);
        } catch (DataAccessException e) {
            logger.error("[AddNote] Произошла ошибка во время добавления заметки в БД у пользователя {} : {}", userId, e.getMessage(), e);
            SendMessage message = messageSender.createMessage(userId, "❌ Не удалось сохранить заметку. Попробуйте позже.");


            InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsInCaseOfCancel();
            message.setReplyMarkup(inlineKeyboardMarkup);

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            return;
        }
        logger.info("[AddNote] Заметка пользователя {} добавлена в БД!", userId);

        SendMessage message = messageSender.createMessage(userId, "✅ Заметка успешно добавлена!");

        InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsInCaseOfAddedNote();
        message.setReplyMarkup(inlineKeyboardMarkup);

        messageSender.sendMessageToUser(userId, message, telegramBotService);
        flagManager.resetFlag(userId);
    }


    /** Методы для создания кнопок*/
    private InlineKeyboardMarkup getButtonsInCaseOfAddedNote() {
        InlineKeyboardButton addNote = callbackButtons.addNoteButton();
        addNote.setText("➕ Добавить ещё одну");

        List<InlineKeyboardButton> addNoteButton = List.of(addNote);
        List<InlineKeyboardButton> myNotesButton = List.of(callbackButtons.myNotesButton());
        List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
        List<List<InlineKeyboardButton>> rows = List.of(addNoteButton, myNotesButton, mainMenuButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getButtonsInCaseOfCancel() {
        InlineKeyboardButton mainMenu = callbackButtons.mainMenuButton();
        List<InlineKeyboardButton> mainMenuButton = List.of(mainMenu);
        List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getCancelButton() {
        List<InlineKeyboardButton> cancelButton = List.of(callbackButtons.cancelButton());
        List<List<InlineKeyboardButton>> rows = List.of(cancelButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}