package com.tgbot.noteskeeperbot.commands.notes.dto;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/** Класс для хранения страницы с заметками и кнопок для пагинации */
public class NotesPageDTO {
    private final String text;
    private final List<List<InlineKeyboardButton>> keyboard;

    public NotesPageDTO(String text, List<List<InlineKeyboardButton>> keyboard) {
        this.text = text;
        this.keyboard = keyboard;
    }

    public String getText() {
        return text;
    }

    public List<List<InlineKeyboardButton>> getKeyboard() {
        return keyboard;
    }
}
