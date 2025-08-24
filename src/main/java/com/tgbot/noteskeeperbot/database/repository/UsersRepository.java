package com.tgbot.noteskeeperbot.database.repository;

import com.tgbot.noteskeeperbot.database.entity.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    boolean existsByUserId(Long userId);
}
