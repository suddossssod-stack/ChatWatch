package com.chatwatch;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.Locale;

public class ChatWatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatWatch.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("chatwatch")
                .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                    ChatWatch.config.enabled = !ChatWatch.config.enabled;
                    ChatWatch.config.save();
                    say(ctx.getSource(), "Filter " + (ChatWatch.config.enabled ? "ON" : "OFF"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("fuzzy").executes(ctx -> {
                    ChatWatch.config.fuzzy = !ChatWatch.config.fuzzy;
                    ChatWatch.config.save(); ChatWatch.reloadFilter();
                    say(ctx.getSource(), "Close-match mode " + (ChatWatch.config.fuzzy ? "ON" : "OFF"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("list").executes(ctx -> {
                    say(ctx.getSource(), "Words: " + String.join(", ", ChatWatch.config.words));
                    say(ctx.getSource(), "Allowed: " + String.join(", ", ChatWatch.config.allow));
                    return 1;
                }))
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("word", StringArgumentType.word()).executes(ctx -> {
                        String w = StringArgumentType.getString(ctx, "word").toLowerCase(Locale.ROOT);
                        if (!ChatWatch.config.words.contains(w)) ChatWatch.config.words.add(w);
                        ChatWatch.config.save(); ChatWatch.reloadFilter();
                        say(ctx.getSource(), "Added \"" + w + "\"");
                        return 1;
                    })))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("word", StringArgumentType.word()).executes(ctx -> {
                        String w = StringArgumentType.getString(ctx, "word").toLowerCase(Locale.ROOT);
                        boolean removed = ChatWatch.config.words.remove(w);
                        ChatWatch.config.save(); ChatWatch.reloadFilter();
                        say(ctx.getSource(), removed ? "Removed \"" + w + "\"" : "\"" + w + "\" wasn't in the list");
                        return 1;
                    })))
                .then(ClientCommandManager.literal("allow")
                    .then(ClientCommandManager.argument("word", StringArgumentType.word()).executes(ctx -> {
                        String w = StringArgumentType.getString(ctx, "word").toLowerCase(Locale.ROOT);
                        if (!ChatWatch.config.allow.contains(w)) ChatWatch.config.allow.add(w);
                        ChatWatch.config.save(); ChatWatch.reloadFilter();
                        say(ctx.getSource(), "\"" + w + "\" will no longer be flagged");
                        return 1;
                    })))
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    ChatWatch.init();
                    say(ctx.getSource(), "Config reloaded");
                    return 1;
                }))
            ));
    }

    private static void say(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src, String msg) {
        src.sendFeedback(Text.literal(ChatWatch.PREFIX + msg));
    }
}
