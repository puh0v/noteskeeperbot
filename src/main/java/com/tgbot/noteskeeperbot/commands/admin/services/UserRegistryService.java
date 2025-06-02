package com.tgbot.noteskeeperbot.commands.admin.services;

import com.tgbot.noteskeeperbot.database.entity.UsersEntity;
import com.tgbot.noteskeeperbot.database.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRegistryService {

    private final UsersRepository usersRepository;

    public UserRegistryService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public void addToDatabase(Long userId) {
        if (!usersRepository.existsByUserId(userId)) {
            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setUserId(userId);
            usersRepository.save(usersEntity);
        }
    }

    public List<UsersEntity> getAllUsers() {
        return usersRepository.findAll();
    }
}