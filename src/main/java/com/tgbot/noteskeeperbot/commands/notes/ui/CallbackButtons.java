package com.tgbot.noteskeeperbot.commands.notes.ui;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Component
public class CallbackButtons {

    public InlineKeyboardButton mainMenuButton() {
        InlineKeyboardButton mainMenu = new InlineKeyboardButton();
        mainMenu.setText("\uD83C\uDFDB Главное меню");
        mainMenu.setCallbackData("/start");
        return mainMenu;
    }

    public InlineKeyboardButton myNotesButton() {
        InlineKeyboardButton myNotesButton = new InlineKeyboardButton();
        myNotesButton.setText("\uD83D\uDCCB Мои заметки");
        myNotesButton.setCallbackData("/my_notes");
        return myNotesButton;
    }

    public InlineKeyboardButton addNoteButton() {
        InlineKeyboardButton addNoteButton = new InlineKeyboardButton();
        addNoteButton.setText("➕ Добавить заметку");
        addNoteButton.setCallbackData("/add_note");
        return addNoteButton;
    }

    public InlineKeyboardButton deleteNoteButton() {
        InlineKeyboardButton deleteNote = new InlineKeyboardButton();
        deleteNote.setText("\uD83D\uDDD1 Удалить");
        deleteNote.setCallbackData("/delete_note");
        return deleteNote;
    }

    public InlineKeyboardButton continueDeletingButton() {
        InlineKeyboardButton deleteNote = new InlineKeyboardButton();
        deleteNote.setText("\uD83D\uDDD1 Продолжить удаление");
        deleteNote.setCallbackData("/delete_note");
        return deleteNote;
    }

    public InlineKeyboardButton shareNoteButton() {
        InlineKeyboardButton shareNote = new InlineKeyboardButton();
        shareNote.setText("\uD83D\uDCE4 Поделиться");
        shareNote.setCallbackData("/share_note");
        return shareNote;
    }

    public InlineKeyboardButton shareNoteToSomeoneButton(String noteText) {
        InlineKeyboardButton shareNote = new InlineKeyboardButton();
        shareNote.setText("\uD83D\uDCE4 Поделиться заметкой");
        shareNote.setSwitchInlineQuery("\n\n\n" + noteText);
        return shareNote;
    }

    public InlineKeyboardButton cancelButton() {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("\uD83D\uDEAB Отмена");
        button.setCallbackData("/cancel");
        return button;
    }
}
