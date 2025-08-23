package com.tgbot.noteskeeperbot.services.noteservice;

import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import com.tgbot.noteskeeperbot.database.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public String getUserNote(Long userId, int number) {
        List<NotesEntity> notes = getAllUserNotes(userId);
        NotesEntity noteFromDB = notes.get(number);

        return noteFromDB.getNoteText();
    }

    public void saveNote(Long userId, String note) {
        NotesEntity noteEntity = new NotesEntity();
        noteEntity.setUserId(userId);
        noteEntity.setNoteText(note);
        noteEntity.setCreatedAt(Instant.now());
        noteRepository.save(noteEntity);
    }

    public void deleteNote(Long userId, int index) {
        List<NotesEntity> notes = getAllUserNotes(userId);
        noteRepository.delete(notes.get(index));
    }
}