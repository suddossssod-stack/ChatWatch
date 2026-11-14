package com.chatwatch;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatWatch {
    public static final String PREFIX = "[ChatWatch] ";
    public static ChatWatchConfig config;
    public static final WordFilter FILTER = new WordFilter();

    private static final Path LOG = FabricLoader.getInstance().getConfigDir().resolve("chatwatch-log.txt");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // "<Name> message"  (optionally with [Rank] prefixes)
    private static final Pattern VANILLA = Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)*<([^>]{1,40})>\\s*(.*)$", Pattern.DOTALL);
    // "Name: message", "Name » message", "Name > message"
    private static final Pattern PLUGIN = Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)*([A-Za-z0-9_]{1,16})\\s*(?:»|›|:|>>|>)\\s+(.*)$", Pattern.DOTALL);

    private ChatWatch() {}

    public static void init() {
        config = ChatWatchConfig.load();
        reloadFilter();
    }

    public static void reloadFilter() {
        FILTER.configure(config.words, config.allow, config.fuzzy);
    }

    /** Called for every line added to the chat window. Returns the line to show (red if flagged). */
    public static Text process(Text message) {
        if (config == null || !config.enabled) return message;
        String full = message.getString();
        if (full.startsWith(PREFIX)) return message;   // our own feedback

        String sender = "unknown", body = full;
        Matcher m = VANILLA.matcher(full);
        if (m.matches()) { sender = m.group(1); body = m.group(2); }
        else {
            m = PLUGIN.matcher(full);
            if (m.matches()) { sender = m.group(1); body = m.group(2); }
        }

        if (!FILTER.isBad(body)) return message;

        if (config.logToFile) log(sender, body);
        MutableText red = Text.empty().setStyle(Style.EMPTY.withColor(Formatting.RED));
        red.append(message);
        return red;
    }

    private static void log(String sender, String body) {
        try {
            String line = "[" + LocalDateTime.now().format(TIME) + "] " + sender + ": " + body.replace('\n', ' ') + System.lineSeparator();
            Files.writeString(LOG, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[ChatWatch] Could not write log: " + e);
        }
    }
}
