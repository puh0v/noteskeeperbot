package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.services.noteservice.NoteService;
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

import java.util.*;

@Component
public class DeleteNote implements Commands {

    private final NoteService noteService;
    private final FlagManager flagManager;
    private final CallbackButtons callbackButtons;
    private final NotesPageBuilder notesPageBuilder;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(DeleteNote.class);

    public DeleteNote(NoteService noteService, FlagManager flagManager, CallbackButtons callbackButtons, NotesPageBuilder notesPageBuilder, MessageSender messageSender) {
        this.noteService = noteService;
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
        prepareMessage(telegramBotService, update, userId, userMessage);
    }


    /** Метод prepareMessage() определяет, какой блок кода нужно вызвать
     * в зависимости от запроса пользователя.
     * Например, если пользователь повторно ввёл команду, перелистывает страницу и т.д.*/
    private void prepareMessage(TelegramBotService telegramBotService, Update update, Long userId, String userMessage) {
        if (userMessage.equals(getCommandName())) {
            handleCommand(telegramBotService, userId);
            return;

        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(telegramBotService, userId, update);
            return;
        }

        if (flagManager.flagHasThisCommand(userId, getCommandName())) {
            handleDeleteNoteInput(telegramBotService, userId, userMessage);
        }
    }

    /** Обработка ситуации, когда пользователь ввёл команду /delete_note повторно.*/
    private void handleCommand(TelegramBotService telegramBotService, Long userId) {
        logger.info("[DeleteNote] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

        List<NotesEntity> notes = noteService.getAllUserNotes(userId);

        if (notes.isEmpty()) {
            logger.info("[DeleteNote] Список заметок пользователя {} пуст.", userId);

            SendMessage message = notesPageBuilder.getEmptyMessage(userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);

        } else {
            logger.info("[DeleteNote] Подготавливаю список заметок для пользователя {} ...", userId);

            flagManager.resetFlag(userId);
            SendMessage message = getReadyPageWithNotes(userId, 0, getPagePrefix());

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            flagManager.setFlag(userId, getCommandName());
        }
    }

    /** Если пользователь перелистывает страницы, prepareMessage() обратится к этому методу*/
    private void handleCallbackQuery(TelegramBotService telegramBotService, Long userId, Update update) {
        logger.info("[DeleteNote] Поступил Callback-запрос от пользователя {} ...", userId);
        String data = update.getCallbackQuery().getData();

        if (data.startsWith(getPagePrefix())) {
            logger.info("[DeleteNote] Пользователь {} перелистывает страницу с заметками...", userId);

            int page = Integer.parseInt(data.replace(getPagePrefix(), ""));
            SendMessage message = getReadyPageWithNotes(userId, page, getPagePrefix());

            messageSender.sendMessageToUser(userId, message, telegramBotService);
        }
    }

    /** Данный метод предназначен на случай, если пользователь выбрал заметку для удаления
     * или же решил отменить удаление*/
    private void handleDeleteNoteInput(TelegramBotService telegramBotService, Long userId, String userMessage) {
        userMessage = userMessage.trim();

        if (userMessage.equals("/cancel")) {
            logger.info("[DeleteNote] Пользователь {} отменил удаление заметки. Формирую сообщение для ответа...", userId);

            SendMessage cancelMessage = messageSender.createMessage(userId, "\uD83D\uDEAB Вы отменили удаление заметки");

            InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsInCaseOfCancel();
            cancelMessage.setReplyMarkup(inlineKeyboardMarkup);

            logger.info("[DeleteNote] Сообщение с ответом пользователю {} готово!", userId);
            messageSender.sendMessageToUser(userId, cancelMessage, telegramBotService);

            flagManager.resetFlag(userId);

        } else if (userMessage.matches("\\d{1,9}")) {
            logger.info("[DeleteNote] Пользователь {} ввёл номер заметки...", userId);
            deleteNote(userId, userMessage, telegramBotService);
        }
    }


    /** Удаляет заметку пользователя по её порядковому номеру.
     * Если номер некорректный — отправляется сообщение об ошибке. */
    public void deleteNote(Long userId, String userMessage, TelegramBotService telegramBotService) {
        logger.info("[DeleteNote] Проверяю наличие нужной заметки для пользователя {} ...", userId);

        List<NotesEntity> notes = noteService.getAllUserNotes(userId);
        int index = Integer.parseInt(userMessage) - 1;

        if (index >= 0 && index < notes.size()) {
            logger.info("[DeleteNote] Пользователь {} ввёл корректный номер заметки. Начинаю её удаление...", userId);

            try {
                noteService.deleteNote(userId, index);
            } catch (Exception e) {
                logger.error("[DeleteNote] Произошла ошибка во время удаления заметки пользователя {} : {}", userId, e.getMessage(), e);
                return;
            }
            logger.info("[DeleteNote] Заметка пользователя {} успешно удалена! Подготавливаю сообщение для ответа...", userId);

            SendMessage message = messageSender.createMessage(userId, "✅ Заметка успешно удалена!");

            InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsInCaseOfDelete();
            message.setReplyMarkup(inlineKeyboardMarkup);

            messageSender.sendMessageToUser(userId, message, telegramBotService);
            flagManager.resetFlag(userId);
        } else {
            logger.info("[DeleteNote] Пользователь {} ввёл несуществующий номер заметки. Формирую сообщение для уведомления ...", userId);
            sendErrorMessage(userId, "❌ Такой заметки не существует. Выберите другую.", telegramBotService);
        }
    }

    /** Формирует страницу с заметками и кнопками пагинации.*/
    private SendMessage getReadyPageWithNotes(Long userId, Integer page, String pagePrefix) {
        logger.info("[DeleteNote] Подготавливаю сообщение с заметками для пользователя {} ...", userId);

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

        SendMessage message = messageSender.createMessage(userId, finalText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[DeleteNote] Страница с заметками для пользователя {} готова!", userId);
        return message;
    }

    /** Ответ пользователю на случай некорректного ввода (неверный номер заметки, и т.п.)*/
    private void sendErrorMessage(Long userId, String text, TelegramBotService telegramBotService) {
        SendMessage message = messageSender.createMessage(userId, text);

        InlineKeyboardMarkup inlineKeyboardMarkup = getCancelButton();
        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[DeleteNote] Сообщение с уведомлением для пользователя {} готово!", userId);
        messageSender.sendMessageToUser(userId, message, telegramBotService);
    }


    /** Методы для создания кнопок*/
    private InlineKeyboardMarkup getButtonsInCaseOfDelete() {
        List<InlineKeyboardButton> deleteButtonRow = List.of(callbackButtons.continueDeletingButton());
        List<InlineKeyboardButton> mainMenuButtonRow = List.of(callbackButtons.mainMenuButton());
        List<List<InlineKeyboardButton>> rows = List.of(deleteButtonRow, mainMenuButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getButtonsInCaseOfCancel() {
        List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
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