package com.tgbot.noteskeeperbot.commands;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FlagManager {

    private final Map<Long, String> flags = new HashMap<>();

    public void setFlag(long userId, String flag) {
        flags.put(userId, flag);
    }

    public void resetFlag(long userId) {
        flags.remove(userId);
    }

    public boolean flagHasThisCommand(long userId, String commandName) {
        return commandName.equals(flags.get(userId));
    }
}