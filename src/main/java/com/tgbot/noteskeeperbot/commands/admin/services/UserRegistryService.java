package com.tgbot.noteskeeperbot.commands.admin.services;

import com.tgbot.noteskeeperbot.database.entity.UsersEntity;
import com.tgbot.noteskeeperbot.database.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

@Service
public class UserRegistryService {

    private final UsersRepository usersRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserRegistryService.class);

    public UserRegistryService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public void addToDatabase(Long userId) {
        if (!usersRepository.existsByUserId(userId)) {
            logger.info("Поступил запрос на добавление нового пользователя {} в БД...", userId);

            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setUserId(userId);
            usersRepository.save(usersEntity);
        }
    }

    public List<UsersEntity> getAllUsers() {
        return usersRepository.findAll();
    }
}