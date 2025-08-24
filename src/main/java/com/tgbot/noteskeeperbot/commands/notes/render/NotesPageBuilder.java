package com.tgbot.noteskeeperbot.commands.notes.render;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.services.messagesender.MessageSender;
import com.tgbot.noteskeeperbot.services.noteservice.NoteService;
import com.tgbot.noteskeeperbot.commands.notes.ui.CallbackButtons;
import com.tgbot.noteskeeperbot.commands.notes.ui.NotesViewMode;
import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


import java.util.ArrayList;
import java.util.List;

@Component
public class NotesPageBuilder {

    private final CallbackButtons callbackButtons;
    private final NoteService noteService;
    private final MessageSender messageSender;
    private static final int MAX_SYMBOLS = 4000;
    private static final int MAX_NOTES_PER_PAGE = 10;
    private static final Logger logger = LoggerFactory.getLogger(NotesPageBuilder.class);

    public NotesPageBuilder(CallbackButtons callbackButtons, NoteService noteService, MessageSender messageSender) {
        this.callbackButtons = callbackButtons;
        this.noteService = noteService;
        this.messageSender = messageSender;
    }


    /** Если у пользователя нет заметок */
    public SendMessage getEmptyMessage(Long userId) {
        logger.info("[NotesPageBuilder] Формирую сообщение с уведомлением об отсутствии заметок для пользователя {} ...", userId);

        SendMessage message = messageSender.createMessage(userId, "Ваш список заметок пуст");

        InlineKeyboardMarkup inlineKeyboardMarkup = getButtonsForEmptyMessage();
        message.setReplyMarkup(inlineKeyboardMarkup);

        logger.info("[NotesPageBuilder] Уведомление об отсутствии заметок для пользователя {} готово!", userId);
        return message;
    }


    /** Получаем заметки и кнопки для пагинации в одном объекте */
    public NotesPageDTO getFieldsFromDTO(Long userId, int page, String pagePrefix, NotesViewMode notesViewMode) {
        logger.info("[NotesPageBuilder] Формирую DTO для пользователя {} ...", userId);
        List<String> listOfPages = getPagesWithNotes(userId, page, notesViewMode);

        if (page < 0 || page >= listOfPages.size()) {
            return new NotesPageDTO("Такой страницы больше не существует. Возможно, вы удалили заметки.", List.of());
        }

        List<List<InlineKeyboardButton>> fullKeyboard = getPaginationButtons(userId, page, pagePrefix, listOfPages);

        logger.info("[NotesPageBuilder] DTO для пользователя {} сформировано!", userId);
        return new NotesPageDTO(listOfPages.get(page), fullKeyboard);
    }


    /** Создаём страницы с заметками */
    private List<String> getPagesWithNotes(Long userId, int page, NotesViewMode notesViewMode) {
        logger.info("[NotesPageBuilder] Начинаю формировать страницы с заметками для пользователя {} ...", userId);

        if (notesViewMode == NotesViewMode.PREVIEW) {
            return pagesForPreviewMode(userId, page);

        } else {
            return pagesForSelectableMode(userId);
        }
    }


    /** Метод для создания страниц с заметками для их просмотра */
    private List<String> pagesForPreviewMode(Long userId, int page) {
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);
        List<String> listOfPages = new ArrayList<>();

        int notesOnPage = 0;
        String lineWithNote;

        StringBuilder currentPage = new StringBuilder();

        if (page == 0) {
            currentPage.append("\uD83D\uDCD4 Ваши заметки \uD83D\uDCD4\n\n\n");
        }

        for (NotesEntity note : notes) {
            lineWithNote = "\uD83D\uDCCB  " + note.getNoteText() + "\n_____________________________________\n\n";

            if ((currentPage.length() + lineWithNote.length() > MAX_SYMBOLS) || (notesOnPage >= MAX_NOTES_PER_PAGE)) {
                listOfPages.add(currentPage.toString());
                currentPage = new StringBuilder();
                notesOnPage = 0;
            }

            currentPage.append(lineWithNote);
            notesOnPage++;
        }

        if (currentPage.length() > 0) {
            listOfPages.add(currentPage.toString());
        }
        logger.info("[NotesPageBuilder] Текстовое сообщение для пользователя {} готово!", userId);

        return listOfPages;
    }


    /** Метод для создания страниц с заметками для взаимодействия с ними (выбор заметки по её порядковому номеру) */
    private List<String> pagesForSelectableMode(Long userId) {
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);
        List<String> listOfPages = new ArrayList<>();

        int i = 1;
        int notesOnPage = 0;
        String lineWithNote;
        StringBuilder currentPage = new StringBuilder();

        for (NotesEntity note : notes) {
            lineWithNote = "\uD83D\uDCCB  " + i++ + ")  " + note.getNoteText() + "\n_____________________________________\n\n";

            if ((currentPage.length() + lineWithNote.length() > MAX_SYMBOLS) || (notesOnPage >= MAX_NOTES_PER_PAGE)) {
                listOfPages.add(currentPage.toString());
                currentPage = new StringBuilder();
                notesOnPage = 0;
            }
            currentPage.append(lineWithNote);
            notesOnPage++;
        }

        if (currentPage.length() > 0) {
            listOfPages.add(currentPage.toString());
        }
        logger.info("[NotesPageBuilder] Текстовое сообщение для пользователя {} готово!", userId);

        return listOfPages;
    }


    /** Методы для создания кнопок*/
    private InlineKeyboardMarkup getButtonsForEmptyMessage() {
        InlineKeyboardButton mainMenu = callbackButtons.mainMenuButton();
        List<InlineKeyboardButton> mainMenuButton = List.of(mainMenu);
        List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private List<List<InlineKeyboardButton>> getPaginationButtons(Long userId, int page, String pagePrefix, List<String> listOfPages) {
        logger.info("[NotesPageBuilder] Создаю кнопки пагинации для пользователя {} ...", userId);

        List<InlineKeyboardButton> paginationRow = new ArrayList<>();
        List<List<InlineKeyboardButton>> fullKeyboard = new ArrayList<>();

        if (page > 0) {
            InlineKeyboardButton prev = new InlineKeyboardButton("⬅\uFE0F Предыдущая");
            prev.setCallbackData(pagePrefix + (page - 1));
            paginationRow.add(prev);
        }

        if ((page == 0) && (listOfPages.size() > 1)) {
            InlineKeyboardButton next = new InlineKeyboardButton("Следующая страница ➡\uFE0F");
            next.setCallbackData(pagePrefix + (page + 1));
            paginationRow.add(next);

        } else if ((page > 0) && (page < (listOfPages.size() - 1))) {
            InlineKeyboardButton next = new InlineKeyboardButton("Следующая ➡\uFE0F");
            next.setCallbackData(pagePrefix + (page + 1));
            paginationRow.add(next);
        }

        if (!paginationRow.isEmpty()) {
            fullKeyboard.add(paginationRow);
        }
        logger.info("[NotesPageBuilder] Кнопки пагинации для сообщения пользователю {} созданы!", userId);

        return fullKeyboard;
    }
}
