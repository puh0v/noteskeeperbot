package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
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

@Component
public class ShareNote implements Commands {

    private final MyNotes myNotes;
    private final FlagManager flagManager;
    private final MessageSender messageSender;
    private final NotesPageBuilder notesPageBuilder;
    private final CallbackButtons callbackButtons;
    private static final Logger logger = LoggerFactory.getLogger(ShareNote.class);


    public ShareNote(MyNotes myNotes, FlagManager flagManager, MessageSender messageSender, NotesPageBuilder notesPageBuilder, CallbackButtons callbackButtons) {
        this.myNotes = myNotes;
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
        List<NotesEntity> notes = myNotes.getNotesByUserId(userId);

        // --------------- Обрабатываем команды или Callback-и с пагинацией ----------------------
        if (userMessage.equals(getCommandName())) {
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
                flagManager.setFlag(userId, getCommandName());
            }
        } else if (update.hasCallbackQuery()) {
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
                return;
            }
        }

        // --------------- Обрабатываем ответы от пользователя по флагу ----------------------
        if (flagManager.flagHasThisCommand(userId, getCommandName())) {
            if (userMessage.matches("\\d+")) {
                logger.info("[ShareNote] Пользователь {} ввёл номер заметки...", userId);

                shareNote(notes, userId, userMessage, telegramBotService);

                // СБРОС ФЛАГА НАХОДИТСЯ ВНУТРИ МЕТОДА

            } else if (userMessage.equals("/cancel")) {
                logger.info("[ShareNote] Пользователь {} отменил пересылку заметки. Формирую сообщение для ответа...", userId);

                SendMessage message = new SendMessage(userId.toString(), "\uD83D\uDEAB Вы отменили пересылку заметки");

                InlineKeyboardButton mainMenu = callbackButtons.mainMenuButton();

                List<InlineKeyboardButton> mainMenuButton = List.of(mainMenu);
                List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                inlineKeyboardMarkup.setKeyboard(rows);

                message.setReplyMarkup(inlineKeyboardMarkup);

                logger.info("[ShareNote] Сообщение с ответом пользователю {} готово!", userId);
                messageSender.sendMessageToUser(userId, message, telegramBotService);
                flagManager.resetFlag(userId);
            }
        }
    }


    public void shareNote(List<NotesEntity> notes, Long userId, String userMessage, TelegramBotService telegramBotService) {
        logger.info("[ShareNote] Проверяю наличие нужной заметки для пользователя {} ...", userId);

        int number = Integer.parseInt(userMessage) - 1;
        if (number >= 0 && number < notes.size()) {
            logger.info("[ShareNote] Пользователь {} ввёл корректный номер заметки. Формирую сообщение для пересылки......", userId);

            NotesEntity note = notes.get(number);
            String noteText = note.getNoteText();

            SendMessage message = new SendMessage(userId.toString(), "\uD83D\uDCDC Ваша заметка:\n\n" + noteText);

            InlineKeyboardButton shareNote = callbackButtons.shareNoteToSomeoneButton(noteText);

            List<InlineKeyboardButton> shareNoteButton = List.of(shareNote);
            List<InlineKeyboardButton> mainMenuButton = List.of(callbackButtons.mainMenuButton());
            List<List<InlineKeyboardButton>> keyboardRows = List.of(shareNoteButton, mainMenuButton);
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

            message.setReplyMarkup(markup);

            logger.info("[ShareNote] Сообщение с заметкой для пользователя {} готово!", userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);
            flagManager.resetFlag(userId);
        } else {
            logger.info("[ShareNote] Пользователь {} ввёл несуществующий номер заметки. Формирую сообщение для уведомления ...", userId);

            SendMessage message = new SendMessage(userId.toString(), "❌ Такой заметки не существует. Выберите другую.");

            List<InlineKeyboardButton> cancelButtonRow = List.of(callbackButtons.cancelButton());
            List<List<InlineKeyboardButton>> rows = List.of(cancelButtonRow);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            inlineKeyboardMarkup.setKeyboard(rows);

            message.setReplyMarkup(inlineKeyboardMarkup);

            logger.info("[ShareNote] Сообщение с уведомлением для пользователя {} готово!", userId);
            messageSender.sendMessageToUser(userId, message, telegramBotService);
        }
    }


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

        SendMessage message = new SendMessage(userId.toString(), finalText);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[ShareNote] Страница с заметками для пользователя {} готова!", userId);
        return message;
    }
}