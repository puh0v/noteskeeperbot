package com.tgbot.noteskeeperbot.commands.notes.dto;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

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
