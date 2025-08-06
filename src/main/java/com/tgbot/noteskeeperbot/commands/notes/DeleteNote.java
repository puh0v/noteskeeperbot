package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
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

import java.util.*;

@Component
public class DeleteNote implements Commands {

    private final NoteRepository noteRepository;
    private final MyNotes myNotes;
    private final FlagManager flagManager;
    private final CallbackButtons callbackButtons;
    private final NotesPageBuilder notesPageBuilder;
    private final MessageSender messageSender;
    private static final Logger logger = LoggerFactory.getLogger(DeleteNote.class);

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
            logger.info("[DeleteNote] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);

            if (notes.isEmpty()) {
                logger.info("[DeleteNote] Список заметок пользователя {} пуст.", userId);

                SendMessage message = notesPageBuilder.getNotesIsEmptyMessage(userId);
                messageSender.sendMessageToUser(userId, message, telegramBotService);

            } else {
                logger.info("[DeleteNote] Подготавливаю список заметок для пользователя {} ...", userId);

                flagManager.resetFlag(userId);
                SendMessage message = getReadyPageWithNotes(userId, 0, getPagePrefix());

                messageSender.sendMessageToUser(userId, message, telegramBotService);
                flagManager.setFlag(userId, getCommandName());
            }
        } else if (update.hasCallbackQuery()) {
            logger.info("[DeleteNote] Поступил Callback-запрос от пользователя {} ...", userId);
            String data = update.getCallbackQuery().getData();

            if (data.startsWith(getPagePrefix())) {
                logger.info("[DeleteNote] Пользователь {} перелистывает страницу с заметками...", userId);

                int page = Integer.parseInt(data.replace(getPagePrefix(), ""));
                SendMessage message = getReadyPageWithNotes(userId, page, getPagePrefix());

                messageSender.sendMessageToUser(userId, message, telegramBotService);
                return;
            }
        }

        // --------------- Обрабатываем ответы боту по флагу ----------------------
        if (flagManager.flagHasThisCommand(userId, getCommandName())) {
            if (userMessage.matches("\\d+")) {
                logger.info("[DeleteNote] Пользователь {} ввёл номер заметки...", userId);

                deleteNote(userId, userMessage, notes, telegramBotService);

                // СБРОС ФЛАГА НАХОДИТСЯ ВНУТРИ МЕТОДА

            } else if (userMessage.equals("/cancel")) {
                logger.info("[DeleteNote] Пользователь {} отменил удаление заметки. Формирую сообщение для ответа...", userId);

                SendMessage cancelMessage = new SendMessage(userId.toString(), "\uD83D\uDEAB Вы отменили удаление заметки");

                List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
                List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                inlineKeyboardMarkup.setKeyboard(rows);

                cancelMessage.setReplyMarkup(inlineKeyboardMarkup);

                logger.info("[DeleteNote] Сообщение с ответом пользователю {} готово!", userId);
                messageSender.sendMessageToUser(userId, cancelMessage, telegramBotService);
                flagManager.resetFlag(userId);
            }
        }
    }

    public void deleteNote(Long userId, String userMessage, List<NotesEntity> notes, TelegramBotService telegramBotService) {
        logger.info("[DeleteNote] Проверяю наличие нужной заметки для пользователя {} ...", userId);

        int number = Integer.parseInt(userMessage) - 1;
        if (number >= 0 && number < notes.size()) {
            logger.info("[DeleteNote] Пользователь {} ввёл корректный номер заметки. Начинаю её удаление...", userId);

            NotesEntity noteToDelete = notes.get(number);

            try {
                noteRepository.delete(noteToDelete);
            } catch (Exception e) {
                logger.error("[DeleteNote] Произошла ошибка во время удаления заметки пользователя {} : {}", userId, e.getMessage(), e);
                return;
            }
            logger.info("[DeleteNote] Заметка пользователя {} успешно удалена! Подготавливаю сообщение для ответа...", userId);

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
            logger.info("[DeleteNote] Пользователь {} ввёл несуществующий номер заметки. Формирую сообщение для уведомления ...", userId);

            SendMessage message = new SendMessage(userId.toString(), "❌ Такой заметки не существует. Выберите другую.");

            List<InlineKeyboardButton> cancelButton = List.of(callbackButtons.cancelButton());
            List<List<InlineKeyboardButton>> rows = List.of(cancelButton);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            inlineKeyboardMarkup.setKeyboard(rows);

            message.setReplyMarkup(inlineKeyboardMarkup);

            logger.info("[DeleteNote] Сообщение с уведомлением для пользователя {} готово!", userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);
        }
    }


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

        SendMessage message = new SendMessage(userId.toString(), finalText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[DeleteNote] Страница с заметками для пользователя {} готова!", userId);
        return message;
    }
}