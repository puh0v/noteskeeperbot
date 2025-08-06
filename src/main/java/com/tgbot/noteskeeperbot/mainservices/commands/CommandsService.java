package com.tgbot.noteskeeperbot.mainservices.commands;

import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.mainservices.receiver.TelegramBotService;
import com.tgbot.noteskeeperbot.mainservices.UserRegistration.UserRegistrationService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommandsService {

    private final FlagManager flagManager;
    private final UserRegistrationService userRegistrationService;
    private static final Logger logger = LoggerFactory.getLogger(CommandsService.class);

    private final Map<String, Commands> commandsMap = new HashMap<>();

    public CommandsService(FlagManager flagManager, UserRegistrationService userRegistrationService, List<Commands> commands) {
        this.flagManager = flagManager;
        this.userRegistrationService = userRegistrationService;
        for (Commands command : commands) {
            commandsMap.put(command.getCommandName(), command);
        }
    }

    public void executeCommand(Update update, TelegramBotService telegramBotService) {
        logger.info("[CommandsService] Получаю более подробную информацию о сообщении...");

        Optional<Long> optUserId = getUserId(update);
        Optional<String> optUserMessage = getUserMessage(update);

        if (optUserId.isEmpty() || optUserMessage.isEmpty()) {
            return;
        }

        Long userId = optUserId.get();
        String userMessage = optUserMessage.get();

        userRegistrationService.addToDatabase(userId);

        // --------------- Обработка команды через Callback или ручной ввод ----------------------
        if (commandsMap.containsKey(userMessage)) {
            logger.info("[CommandsService] Поступила команда от пользователя {}. Вызываю соответствующий класс...", userId);

            flagManager.resetFlag(userId);
            commandsMap.get(userMessage).execute(userId, userMessage, update, telegramBotService);
            return;
        }

        // --------------- Callback-и (пагинация) ---------------------
        for (Commands prefixes : commandsMap.values()) {
            String prefix = prefixes.getPagePrefix();
            if (userMessage.startsWith(prefix)) {
                logger.info("[CommandsService] Пользователь {} использует пагинацию. Вызываю соответствующий класс...", userId);

                prefixes.execute(userId, userMessage, update, telegramBotService);
                return;
            }
        }

        // --------------- Отправляем сообщение/callback в класс-команду по флагу (напр., /cancel) ---------------------
        for (Commands command : commandsMap.values()) {
            if (flagManager.flagHasThisCommand(userId, command.getCommandName())) {
                logger.info("[CommandsService] Обработка сообщения/Callback-запроса по флагу от пользователя {}. Вызываю соответствующий класс...", userId);

                command.execute(userId, userMessage, update, telegramBotService);
                return;
            }
        }
    }


    public Optional<Long> getUserId(Update update) {
        if (update.hasMessage()) {
            return Optional.of(update.getMessage().getFrom().getId());
        } else if (update.hasCallbackQuery()) {
            return Optional.of(update.getCallbackQuery().getFrom().getId());
        }
        return Optional.empty();
    }

    public Optional<String> getUserMessage(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                return Optional.of(message.getText());
            }
            if (message.getCaption() != null) {
                return Optional.of(message.getCaption());
            }
        } else if (update.hasCallbackQuery()) {
            return Optional.of(update.getCallbackQuery().getData());
        }
        return Optional.empty();
    }
}