package com.tgbot.noteskeeperbot.services.registration;

import com.tgbot.noteskeeperbot.database.entity.UsersEntity;
import com.tgbot.noteskeeperbot.database.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserRegistrationService {

    private final UsersRepository usersRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationService.class);

    public UserRegistrationService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public void saveUserToDatabase(Long userId) {
        if (!usersRepository.existsByUserId(userId)) {
            logger.info("[UserRegistrationService] Новый пользователь: {} . Начинаю регистрацию...", userId);

            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setUserId(userId);

            try {
                usersRepository.save(usersEntity);
            } catch (Exception e) {
                logger.error("[UserRegistrationService] Произошла ошибка во время регистрации нового пользователя", e.getMessage(), e);
                return;
            }
            logger.info("[UserRegistrationService] Регистрация пользователя {} прошла успешно!", userId);
        }
    }

    public List<Long> getAllUserIds() {
        List<UsersEntity> usersEntities = usersRepository.findAll();
        List<Long> userIds = new ArrayList<>();
        for (UsersEntity usersEntity : usersEntities) {
            userIds.add(usersEntity.getUserId());
        }
        return userIds;
    }
}