package org.sebirka.sblockvelocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SbcCommand implements SimpleCommand {

    private final SBlockVelocity plugin;
    private final Logger logger;

    public SbcCommand(SBlockVelocity plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>SBlockVelocity</bold></gold> <dark_gray>»</dark_gray> <white>Используйте: <yellow>/sbc reload</yellow></white>"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!source.hasPermission("sblockvelocity.command.reload")) {
                source.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас нет прав для выполнения этой команды.</red>"));
                return;
            }
            plugin.loadConfig();
            source.sendMessage(MiniMessage.miniMessage().deserialize("<green>Конфигурация SBlockVelocity успешно перезагружена!</green>"));
            logger.info("SBlockVelocity configuration reloaded:");

        } else {
            source.sendMessage(MiniMessage.miniMessage().deserialize("<red>Неизвестная подкоманда. Используйте: <yellow>/sbc reload</yellow></red>"));
        }
    }


    @Override
    public boolean hasPermission(Invocation invocation) {
        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
            return invocation.source().hasPermission("sblockvelocity.command.reload");
        }
        return true;
    }
}