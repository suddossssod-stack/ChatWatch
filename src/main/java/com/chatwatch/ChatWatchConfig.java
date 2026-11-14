package com.chatwatch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Saved to .minecraft/config/chatwatch.json */
public final class ChatWatchConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("chatwatch.json");

    public boolean enabled = true;
    public boolean fuzzy = false;
    public boolean logToFile = true;

    public List<String> words = new ArrayList<>(List.of(
        "fuck", "shit", "bitch", "bastard", "asshole", "ass", "dumbass",
        "jackass", "dick", "cock", "cunt", "pussy", "piss", "slut",
        "whore", "motherfucker", "stfu", "kys",
        "crap", "damn", "dammit", "hell", "bullshit", "horseshit",
        "dipshit", "shithead", "shitface", "fuckface", "fuckhead",
        "clusterfuck", "fucker", "fucking", "fucked", "fucks",
        "shitty", "shitting", "bitches", "bitchy", "bastards",
        "asswipe", "prick", "douche", "douchebag"
    ));

    public List<String> allow = new ArrayList<>(List.of(
        "hello", "shell", "shellac", "class", "classic", "glass",
        "grass", "pass", "passage", "password", "compass",
        "assume", "assist", "assess", "assassin", "bass", "mass",
        "massive", "brass", "bypass", "harass", "embassy",
        "scunthorpe", "cocktail", "cockpit", "cockroach",
        "cockatoo", "peacock", "hancock", "woodcock", "shuttlecock",
        "arsenal", "arsenic", "classroom", "assorted",
        "assortment", "passion", "passenger", "compassion",
        "dickens", "dickinson", "shiitake", "shitake", "mishit",
        "analysis", "analyst", "bastille", "witch", "where",
        "pitch", "ditch", "hitch", "which", "switch", "titch"
    ));

    public static ChatWatchConfig load() {
        try {
            if (Files.exists(FILE)) {
                ChatWatchConfig c = GSON.fromJson(Files.readString(FILE), ChatWatchConfig.class);
                if (c != null) return c;
            }
        } catch (Exception e) {
            System.err.println("[ChatWatch] Could not read config, using defaults: " + e);
        }

        ChatWatchConfig c = new ChatWatchConfig();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[ChatWatch] Could not save config: " + e);
        }
    }
}
