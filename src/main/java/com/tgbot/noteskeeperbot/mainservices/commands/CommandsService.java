package com.tgbot.noteskeeperbot.mainservices.commands;

import com.tgbot.noteskeeperbot.commands.Commands;
import com.tgbot.noteskeeperbot.commands.FlagManager;
import com.tgbot.noteskeeperbot.commands.notes.MyNotes;
import com.tgbot.noteskeeperbot.commands.notes.render.NotesPageBuilder;
import com.tgbot.noteskeeperbot.mainservices.bot.TelegramBotService;
import com.tgbot.noteskeeperbot.commands.admin.services.UserRegistryService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommandsService {

    private final FlagManager flagManager;
    private final UserRegistryService userRegistry;

    private final Map<String, Commands> commandsMap = new HashMap<>();

    public CommandsService(FlagManager flagManager, UserRegistryService userRegistry, List<Commands> commands) {
        this.flagManager = flagManager;
        this.userRegistry = userRegistry;
        for (Commands command : commands) {
            commandsMap.put(command.getCommandName(), command);
        }
    }

    public void executeCommand(Update update, TelegramBotService telegramBotService) {
        Optional<Long> optUserId = getUserId(update);
        Optional<String> optUserMessage = getUserMessage(update);

        if (optUserId.isEmpty() || optUserMessage.isEmpty()) {
            return;
        }

        Long userId = optUserId.get();
        String userMessage = optUserMessage.get();

        userRegistry.addToDatabase(userId);

        // --------------- Обработка команды через Callback или текст ----------------------
        if (commandsMap.containsKey(userMessage)) {
            flagManager.resetFlag(userId);
            commandsMap.get(userMessage).execute(userId, userMessage, update, telegramBotService);
            return;
        }
        // --------------- Callback-и (перелистывание страниц заменток) ---------------------
        for (Commands prefixes : commandsMap.values()) {
            String prefix = prefixes.getPagePrefix();
            if (userMessage.startsWith(prefix)) {
                prefixes.execute(userId, userMessage, update, telegramBotService);
                return;
            }
        }
        // --------------- Отправляем сообщение/callback в класс-команду по флагу (напр., /cancel) ---------------------
        for (Commands command : commandsMap.values()) {
            if (flagManager.flagHasThisCommand(userId, command.getCommandName())) {
                command.execute(userId, userMessage, update, telegramBotService);
                return;
            }
        }
    }


    // --------------- Геттеры ----------------------
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