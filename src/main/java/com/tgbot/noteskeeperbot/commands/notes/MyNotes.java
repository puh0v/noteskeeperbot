package com.tgbot.noteskeeperbot.commands.notes;

import com.tgbot.noteskeeperbot.commands.notes.dto.MyNotesContext;
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
        MyNotesContext myNotesContext = new MyNotesContext(telegramBotService, update, userId, userMessage);
        prepareMessage(myNotesContext);
    }

    /** Метод prepareMessage() определяет, какой блок кода нужно вызвать в зависимости от запроса пользователя.
     * Например, если пользователь повторно ввёл команду, перелистывает страницу и т.д.*/
    private void prepareMessage(MyNotesContext myNotesContext) {
        Long userId = myNotesContext.userId();
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);

        if (myNotesContext.userMessage().equals(getCommandName())) {
            logger.info("[MyNotes] Начинаю выполнение команды {} для пользователя {} ...", getCommandName(), userId);
            handleCommand(notes, myNotesContext);
        }
        else if (myNotesContext.update().hasCallbackQuery()) {
            logger.info("[MyNotes] Поступил Callback-запрос от пользователя {} ...", userId);
            handleCallbackQuery(myNotesContext);
        }
    }

    /** Обработка команды /my_notes */
    private void handleCommand(List<NotesEntity> notes, MyNotesContext myNotesContext) {
        Long userId = myNotesContext.userId();

        if (notes.isEmpty()) {
            logger.info("[MyNotes] Список заметок пользователя {} пуст.", userId);

            sendEmptyPage(myNotesContext);
        } else {
            logger.info("[MyNotes] Подготавливаю список заметок для пользователя {} ...", userId);

            sendReadyPage(myNotesContext, 0);
        }
    }

    /** Если пользователь перелистывает страницы, prepareMessage() обратится к этому методу*/
    private void handleCallbackQuery(MyNotesContext myNotesContext) {
        Long userId = myNotesContext.userId();
        String data = myNotesContext.update().getCallbackQuery().getData();
        SendMessage message = new SendMessage();

        if (data.startsWith(getPagePrefix())) {
            logger.info("[MyNotes] Пользователь {} перелистывает страницу с заметками...", userId);
            int page = Integer.parseInt(data.replace(getPagePrefix(), ""));

            message = getReadyPageWithNotes(myNotesContext, page, message);
            messageSender.sendMessageToUser(userId, message, myNotesContext.telegramBotService());
        }
    }


    private void sendReadyPage(MyNotesContext myNotesContext, int page) {
        SendMessage message = new SendMessage();

        message = getReadyPageWithNotes(myNotesContext, page, message);
        messageSender.sendMessageToUser(myNotesContext.userId(), message, myNotesContext.telegramBotService());
    }

    private void sendEmptyPage(MyNotesContext myNotesContext) {
        SendMessage message = notesPageBuilder.getEmptyMessage(myNotesContext.userId());
        messageSender.sendMessageToUser(myNotesContext.userId(), message, myNotesContext.telegramBotService());
    }

    /** Формирует страницу с заметками и кнопками пагинации.*/
    private SendMessage getReadyPageWithNotes(MyNotesContext myNotesContext, int page, SendMessage message) {
        Long userId = myNotesContext.userId();

        logger.info("[MyNotes] Подготавливаю сообщение с заметками для пользователя {} ...", userId);

        NotesPageDTO notesPageDTO = notesPageBuilder.getFieldsFromDTO(userId, page, getPagePrefix(), NotesViewMode.PREVIEW);
        String textFromDTO = notesPageDTO.getText();

        message.setChatId(userId.toString());
        message.setText(textFromDTO);

        InlineKeyboardMarkup inlineKeyboardMarkup = getPaginationButtons(page, notesPageDTO);
        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[MyNotes] Страница с заметками для пользователя {} готова!", userId);
        return message;
    }

    /** Получаем кнопки для пагинации.*/
    private InlineKeyboardMarkup getPaginationButtons(int page, NotesPageDTO notesPageDTO) {
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
}