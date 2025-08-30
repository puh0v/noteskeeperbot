package com.tgbot.noteskeeperbot.commands;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FlagManager {
    private final Map<Long, Commands> flags;

    public FlagManager() {
        this.flags = new HashMap<>();
    }


    public void setFlag(long userId, Commands command) {
        flags.put(userId, command);
    }

    public Commands getCommandByFlag(long userId) {
        return flags.get(userId);
    }

    public boolean flagContainsKey(long userId) {
        return flags.containsKey(userId);
    }

    public boolean flagContainsCommand(long userId, String userMessage) {
        String commandName = flags.get(userId).getCommandName();
        return userMessage.equals(commandName);
    }

    public void resetFlag(long userId) {
        flags.remove(userId);
    }
}