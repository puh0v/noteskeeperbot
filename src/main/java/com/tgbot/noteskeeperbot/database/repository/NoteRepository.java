package com.tgbot.noteskeeperbot.database.repository;

import com.tgbot.noteskeeperbot.database.entity.NotesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<NotesEntity, Long> {

    List<NotesEntity> findAllByUserId(Long userId);
}