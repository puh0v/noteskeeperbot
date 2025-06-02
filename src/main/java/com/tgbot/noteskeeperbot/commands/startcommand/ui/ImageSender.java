package com.tgbot.noteskeeperbot.commands.startcommand.ui;

import org.springframework.stereotype.Component;
import java.io.InputStream;

@Component
public class ImageSender {

    public InputStream getImagesStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
