package com.tgbot.noteskeeperbot.database.entity;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table (name = "notes")
public class NotesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name = "user_id", nullable = false)
    private Long userId;

    @Column (name = "text", columnDefinition = "TEXT", length = 1000, nullable = false)
    private String noteText;

    @Column (name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    // Сеттеры/Геттеры:
    public Long getId() {
        return id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setNoteText(String text) {
        this.noteText = text;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}