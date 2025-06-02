package com.tgbot.noteskeeperbot.commands.notes.services;

import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.database.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {
    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<NotesEntity> getAllUserNotes(Long userId) {
        return noteRepository.findAllByUserId(userId);
    }
}