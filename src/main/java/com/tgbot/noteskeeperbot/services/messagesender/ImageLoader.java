package com.tgbot.noteskeeperbot.services.messagesender;

import org.springframework.stereotype.Component;
import java.io.InputStream;

@Component
public class ImageLoader {

    public InputStream getImageStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
