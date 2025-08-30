package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.services.noteservice.NoteService;
import com.tgbot.noteskeeperbot.services.receiver.TelegramBotService;
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

/** Класс для пересылки заметок пользователя другим пользователям Telegram. */
@Component
public class ShareNote implements Commands {

    private final NoteService noteService;
    private final FlagManager flagManager;
    private final MessageSender messageSender;
    private final NotesPageBuilder notesPageBuilder;
    private final CallbackButtons callbackButtons;
    private static final Logger logger = LoggerFactory.getLogger(ShareNote.class);


    public ShareNote(NoteService noteService, FlagManager flagManager, MessageSender messageSender, NotesPageBuilder notesPageBuilder, CallbackButtons callbackButtons) {
        this.noteService = noteService;
        this.flagManager = flagManager;
        this.messageSender = messageSender;
        this.notesPageBuilder = notesPageBuilder;
        this.callbackButtons = callbackButtons;
    }

    @Override
    public String getCommandName() {
        return "/share_note";
    }

    @Override
    public String getPagePrefix() {
        return "/share_note_page_";
    }

    @Override
    public void execute(Long userId, String userMessage, Update update, TelegramBotService telegramBotService) {
        prepareMessage(telegramBotService, update, userId, userMessage);
    }


    /** Метод prepareMessage() определяет, какой блок кода нужно вызвать в зависимости от запроса пользователя.
     * Например, если пользователь повторно ввёл команду, перелистывает страницу и т.д.*/
    private void prepareMessage(TelegramBotService telegramBotService, Update update, Long userId, String userMessage) {
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);

        if (userMessage.equals(getCommandName())) {
            handleCommand(notes, telegramBotService, userId);

        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(telegramBotService, userId, update);
        }

        if (flagManager.flagContainsCommand(userId, getCommandName())) {
            handleShareNoteInput(notes, telegramBotService, userId, userMessage);
        }
    }

    /** Обработка команды /share_note */
    private void handleCommand(List<NotesEntity> notes, TelegramBotService telegramBotService, Long userId) {
        logger.info("[ShareNote] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

        if (notes.isEmpty()) {
            logger.info("[ShareNote] Список заметок пользователя {} пуст.", userId);

            SendMessage message = notesPageBuilder.getEmptyMessage(userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);

        } else {
            logger.info("[ShareNote] Подготавливаю список заметок для пользователя {} ...", userId);
            flagManager.resetFlag(userId);

            SendMessage message = getReadyPageWithNotes(userId, 0, getPagePrefix());
            messageSender.sendMessageToUser(userId, message, telegramBotService);

            flagManager.setFlag(userId, this);
        }
    }

    /** Если пользователь перелистывает страницы, prepareMessage() обратится к этому методу*/
    private void handleCallbackQuery(TelegramBotService telegramBotService, Long userId, Update update) {
        logger.info("[ShareNote] Поступил Callback-запрос от пользователя {} ...", userId);
        String data = update.getCallbackQuery().getData();

        if (data.startsWith(getPagePrefix())) {
            logger.info("[ShareNote] Пользователь {} перелистывает страницу с заметками...", userId);

            SendMessage message;
            int page = Integer.parseInt(data.replace(getPagePrefix(), ""));

            if (page == 0) {
                message = getReadyPageWithNotes(userId, 0, getPagePrefix());
            } else if (page > 0) {
                message = getReadyPageWithNotes(userId, page, getPagePrefix());
            } else {
                return;
            }

            messageSender.sendMessageToUser(userId, message, telegramBotService);
        }
    }

    /** Данный метод предназначен на случай, если пользователь выбрал заметку для пересылки
     * или же решил отменить действие*/
    private void handleShareNoteInput(List<NotesEntity> notes, TelegramBotService telegramBotService, Long userId, String userMessage) {
        String trimmedText = userMessage.trim();

        if (trimmedText.equals("/cancel")) {
            logger.info("[ShareNote] Пользователь {} отменил пересылку заметки. Формирую сообщение для ответа...", userId);

            SendMessage message = new SendMessage(userId.toString(), "\uD83D\uDEAB Вы отменили пересылку заметки");

            InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsInCaseOfCancel();
            message.setReplyMarkup(inlineKeyboardMarkup);

            logger.info("[ShareNote] Сообщение с ответом пользователю {} готово!", userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);

            flagManager.resetFlag(userId);

        } else if (trimmedText.matches("\\d{1,9}")) {
            logger.info("[ShareNote] Пользователь {} ввёл номер заметки...", userId);
            shareNote(notes, userId, trimmedText, telegramBotService);
        }
    }


    /** Отправляет заметку пользователя по её порядковому номеру.
     * Если номер некорректный — отправляется сообщение об ошибке. */
    public void shareNote(List<NotesEntity> notes, Long userId, String userMessage, TelegramBotService telegramBotService) {
        logger.info("[ShareNote] Проверяю наличие нужной заметки для пользователя {} ...", userId);
        int number = Integer.parseInt(userMessage) - 1;

        if (number >= 0 && number < notes.size()) {
            logger.info("[ShareNote] Пользователь {} ввёл корректный номер заметки. Формирую сообщение для пересылки......", userId);
            String note = noteService.getUserNote(userId, number);

            SendMessage message = messageSender.createMessage(userId, "\uD83D\uDCDC Ваша заметка:\n\n" + note);

            InlineKeyboardMarkup markup = getButtonsInCaseOfShare(note);
            message.setReplyMarkup(markup);

            logger.info("[ShareNote] Сообщение с заметкой для пользователя {} готово!", userId);

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            flagManager.resetFlag(userId);

        } else {
            logger.info("[ShareNote] Пользователь {} ввёл несуществующий номер заметки. Формирую сообщение для уведомления ...", userId);

            SendMessage message = messageSender.createMessage(userId, "❌ Такой заметки не существует. Выберите другую.");

            InlineKeyboardMarkup inlineKeyboardMarkup = getCancelButton();
            message.setReplyMarkup(inlineKeyboardMarkup);

            logger.info("[ShareNote] Сообщение с уведомлением для пользователя {} готово!", userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);
        }
    }


    /** Формирует страницу с заметками и кнопками пагинации.*/
    private SendMessage getReadyPageWithNotes(Long userId, Integer page, String pagePrefix) {
        logger.info("[ShareNote] Подготавливаю сообщение с заметками для пользователя {} ...", userId);

        NotesPageDTO notesPageDTO = notesPageBuilder.getFieldsFromDTO(userId, page, pagePrefix, NotesViewMode.SELECTABLE);
        String textFromDTO = notesPageDTO.getText();

        String finalText;
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(notesPageDTO.getKeyboard());

        if (notesPageDTO.getKeyboard().isEmpty() && page > 0) {
            finalText = textFromDTO;
            keyboard.add(List.of(callbackButtons.mainMenuButton()));

        } else {
            finalText = (page == 0)
                ? "✉\uFE0F Отправьте номер заметки, которую вы хотите переслать\n\n\n" + textFromDTO
                : textFromDTO;

            keyboard.add(List.of(callbackButtons.cancelButton()));
        }

        SendMessage message = messageSender.createMessage(userId, finalText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[ShareNote] Страница с заметками для пользователя {} готова!", userId);
        return message;
    }


    /** Методы для создания кнопок*/
    private InlineKeyboardMarkup getButtonsInCaseOfShare(String note) {
        InlineKeyboardButton shareNote = callbackButtons.shareNoteToSomeoneButton(note);

        List<InlineKeyboardButton> shareNoteButton = List.of(shareNote);
        List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
        List<List<InlineKeyboardButton>> keyboardRows = List.of(shareNoteButton, mainMenuButton);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(keyboardRows);

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