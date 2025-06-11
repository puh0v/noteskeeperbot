package com.tgbot.noteskeeperbot.commands.notes.render;

import com.tgbot.noteskeeperbot.commands.notes.dto.NotesPageDTO;
import com.tgbot.noteskeeperbot.commands.notes.services.NoteService;
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
    private static final Logger logger = LoggerFactory.getLogger(NotesPageBuilder.class);

    public NotesPageBuilder(CallbackButtons callbackButtons, NoteService noteService) {
        this.callbackButtons = callbackButtons;
        this.noteService = noteService;
    }

    // --------------- Если у юзера нет заметок ----------------------
    public SendMessage getNotesIsEmptyMessage(Long userId) {
        logger.info("[NotesPageBuilder] Формирую сообщение с уведомлением об отсутствии заметок для пользователя {} ...", userId);

        SendMessage message = new SendMessage(userId.toString(), "Ваш список заметок пуст");

        InlineKeyboardButton mainMenu = callbackButtons.mainMenuButton();
        List<InlineKeyboardButton> mainMenuButton = List.of(mainMenu);
        List<List<InlineKeyboardButton>> rows = List.of(mainMenuButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        message.setReplyMarkup(inlineKeyboardMarkup);
        logger.info("[NotesPageBuilder] Уведомление об отсутствии заметок для пользователя {} готово!", userId);
        return message;
    }

    // --------------- Получаем текст и кнопки для пагинации в одном объекте ----------------------
    public NotesPageDTO getFieldsFromDTO(Long userId, int page, String pagePrefix, NotesViewMode notesViewMode) {
        logger.info("[NotesPageBuilder] Подготавливаю сообщение со списком заметок и пагинацией для пользователя {} ...", userId);

        // --------------- Рендерим текст ----------------------
        List<NotesEntity> notes = noteService.getAllUserNotes(userId);
        List<String> pagesList = new ArrayList<>();


        int i = 1;
        int notesOnPage = 0;
        String noteBlock;

        StringBuilder currentPageSB = new StringBuilder();

        logger.info("[NotesPageBuilder] Начинаю формировать текстовое сообщение для пользователя {} ...", userId);
        if (notesViewMode == NotesViewMode.PREVIEW) {

            if (page == 0) {
                currentPageSB.append("\uD83D\uDCD4 Ваши заметки \uD83D\uDCD4\n\n\n");
            }

            for (NotesEntity note : notes) {
                noteBlock = "\uD83D\uDCCB  " + note.getNoteText() + "\n_____________________________________\n\n";
                if (currentPageSB.length() + noteBlock.length() > 4000 || notesOnPage >= 10) {
                    pagesList.add(currentPageSB.toString());
                    currentPageSB = new StringBuilder();
                    notesOnPage = 0;
                }
                currentPageSB.append(noteBlock);
                notesOnPage++;
            }

        } else {
            for (NotesEntity note : notes) {
                noteBlock = "\uD83D\uDCCB  " + i++ + ")  " + note.getNoteText() + "\n_____________________________________\n\n";
                if (currentPageSB.length() + noteBlock.length() > 4000 || notesOnPage >= 10) {
                    pagesList.add(currentPageSB.toString());
                    currentPageSB = new StringBuilder();
                    notesOnPage = 0;
                }
                currentPageSB.append(noteBlock);
                notesOnPage++;
            }
        }
        logger.info("[NotesPageBuilder] Текстовое сообщение для пользователя {} сформировано!", userId);

        if (!currentPageSB.toString().isBlank()) {
            pagesList.add(currentPageSB.toString());
        }


        // --------------- Рендерим кнопки ----------------------
        logger.info("[NotesPageBuilder] Создаю кнопки пагинации для пользователя {} ...", userId);

        List<List<InlineKeyboardButton>> fullKeyboard = new ArrayList<>();
        List<InlineKeyboardButton> paginationRow = new ArrayList<>();

        if (page < 0 || page >= pagesList.size()) {
            return new NotesPageDTO("Такой страницы больше не существует. Возможно, вы удалили заметки.", List.of());
        }

        if (page > 0) {
            InlineKeyboardButton prev = new InlineKeyboardButton("⬅\uFE0F Предыдущая");
            prev.setCallbackData(pagePrefix + (page - 1));
            paginationRow.add(prev);
        }

        if ((page == 0) && (pagesList.size() > 1)) {
            InlineKeyboardButton next = new InlineKeyboardButton("Следующая страница ➡\uFE0F");
            next.setCallbackData(pagePrefix + (page + 1));
            paginationRow.add(next);

        } else if ((page > 0) && (page < (pagesList.size() - 1))) {
            InlineKeyboardButton next = new InlineKeyboardButton("Следующая ➡\uFE0F");
            next.setCallbackData(pagePrefix + (page + 1));
            paginationRow.add(next);
        }

        if (!paginationRow.isEmpty()) {
            fullKeyboard.add(paginationRow);
        }
        logger.info("[NotesPageBuilder] Кнопки пагинации для сообщения пользователю {} созданы!", userId);

        logger.info("[NotesPageBuilder] Сообщение со списком заметок и пагинацией для пользователя {} готово!", userId);
        return new NotesPageDTO(pagesList.get(page), fullKeyboard);
    }
}
