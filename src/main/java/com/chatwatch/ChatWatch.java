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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ChatWatch {
    public static final String PREFIX = "[ChatWatch] ";
    public static ChatWatchConfig config;
    public static final WordFilter FILTER = new WordFilter();

    private static final Path LOG = FabricLoader.getInstance().getConfigDir().resolve("chatwatch-log.txt");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern VANILLA = Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)*<([^>]{1,40})>\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern PLUGIN = Pattern.compile("^(?:\\[[^\\]]*\\]\\s*)*([A-Za-z0-9_]{1,16})\\s*(?:»|›|:|>>|>)\\s+(.*)$", Pattern.DOTALL);

    private ChatWatch() {}

    public static void init() {
        config = ChatWatchConfig.load();
        reloadFilter();
    }

    public static void reloadFilter() {
        FILTER.configure(config.words, config.allow, config.fuzzy);
    }

    public static Text process(Text message) {
        if (config == null || !config.enabled) return message;
        String full = message.getString();
        if (full.startsWith(PREFIX)) return message;

        String sender = "unknown", body = full;
        Matcher m = VANILLA.matcher(full);
        if (m.matches()) {
            sender = m.group(1);
            body = m.group(2);
        } else {
            m = PLUGIN.matcher(full);
            if (m.matches()) {
                sender = m.group(1);
                body = m.group(2);
            }
        }

        List<int[]> badRanges = FILTER.badRanges(body);
        if (badRanges.isEmpty()) return message;

        if (config.logToFile) log(sender, body);
        return highlightRanges(message, full, body, badRanges);
    }

    private static Text highlightRanges(Text original, String full, String body, List<int[]> bodyRanges) {
        int bodyOffset = full.indexOf(body);
        if (bodyOffset < 0) return original;

        List<int[]> ranges = new ArrayList<>();
        for (int[] r : bodyRanges) {
            ranges.add(new int[]{bodyOffset + r[0], bodyOffset + r[1]});
        }

        MutableText result = Text.empty();
        int[] offset = {0};
        original.visit((style, content) -> {
            appendStyled(result, content, style, offset, ranges);
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static void appendStyled(MutableText out, String content, Style style, int[] offset, List<int[]> ranges) {
        int start = offset[0];
        int end = start + content.length();
        int cursor = 0;

        for (int[] range : ranges) {
            int a = Math.max(start, range[0]);
            int b = Math.min(end, range[1]);
            if (a >= b) continue;

            int localA = a - start;
            int localB = b - start;

            if (localA > cursor) {
                out.append(Text.literal(content.substring(cursor, localA)).setStyle(style));
            }

            Style red = style.withColor(Formatting.RED);
            out.append(Text.literal(content.substring(Math.max(cursor, localA), localB)).setStyle(red));
            cursor = localB;
        }

        if (cursor < content.length()) {
            out.append(Text.literal(content.substring(cursor)).setStyle(style));
        }

        offset[0] = end;
    }

    private static void log(String sender, String body) {
        try {
            String line = "[" + LocalDateTime.now().format(TIME) + "] " + sender + ": "
                    + body.replace('\n', ' ') + System.lineSeparator();
            Files.writeString(LOG, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[ChatWatch] Could not write log: " + e);
        }
    }
}
