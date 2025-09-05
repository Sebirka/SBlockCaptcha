package org.sebirka.sblockvelocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.inject.Inject;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.api.player.GameMode;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Plugin(
        id = "sblockvelocity",
        name = "sblockvelocity",
        version = "1.0"
)
public class SBlockVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    public static LimboFactory limboFactory;
    private static SBlockVelocity plugin;
    private static VirtualWorld filterWorld;
    private Limbo filterServer;
    private PluginConfig config;
    private final Set<UUID> passedCaptchaPlayers = new HashSet<>();


    @Inject
    public SBlockVelocity(ProxyServer server, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        plugin = this;
        loadConfig();

        this.limboFactory = (LimboFactory) this.server.getPluginManager()
                .getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance)
                .orElseThrow(() -> {
                    logger.error("LimboAPI plugin not found. Make sure it's installed and loaded.");
                    return new IllegalStateException("LimboAPI plugin not found");
                });
    }

    public void loadConfig() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Failed to create data directory", e);
                return;
            }
        }

        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                    logger.info("Default config.yml created.");
                } else {
                    logger.error("Default config.yml not found in resources.");
                }
            } catch (IOException e) {
                logger.error("Failed to create default config.yml", e);
            }
        }

        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> configMap = yaml.load(in);
            this.config = new PluginConfig(configMap);
            logger.info("Config.yml loaded successfully.");

            if (this.limboFactory != null) {
                setFilterWorld();
            } else {
                logger.warn("LimboAPI factory not yet initialized, cannot re-create filter world on config reload.");
            }



        } catch (IOException e) {
            logger.error("Failed to load config.yml", e);
            this.config = new PluginConfig(new HashMap<>());
        }
    }

    @Subscribe
    public void onProxyDisable(ProxyShutdownEvent event) {

    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        if (this.filterServer == null) {
            setFilterWorld();
        }
        registerCommands();
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();

        CommandMeta sbcMeta = commandManager.metaBuilder("sbc")
                .plugin(this)
                .build();
        commandManager.register(sbcMeta, new SbcCommand(this, logger));


    }

    private void setFilterWorld() {
        if (this.filterServer != null) {
            this.filterServer.dispose();
            this.filterServer = null;
        }

        this.filterWorld = this.limboFactory.createVirtualWorld(
                Dimension.OVERWORLD,
                config.getLimboFilterWorldSpawnX(),
                config.getLimboFilterWorldSpawnY(),
                config.getLimboFilterWorldSpawnZ(),
                0f,
                0f
        );

        int platformMinX = config.getLimboFilterWorldPlatformMinX();
        int platformMaxX = config.getLimboFilterWorldPlatformMaxX();
        int platformMinZ = config.getLimboFilterWorldPlatformMinZ();
        int platformMaxZ = config.getLimboFilterWorldPlatformMaxZ();
        int platformY = config.getLimboFilterWorldPlatformY();
        String platformBlock = config.getCaptchaPlatformBlock();

        for (int x = platformMinX; x < platformMaxX; x++) {
            for (int z = platformMinZ; z < platformMaxZ; z++) {
                this.filterWorld.setBlock(x, platformY, z,
                        limboFactory.createSimpleBlock(platformBlock));
            }
        }

        logger.info("Виртуальный Мир создан");

        filterServer = limboFactory.createLimbo(filterWorld)
                .setName(config.getLimboFilterServerName())
                .setReadTimeout(config.getLimboFilterServerReadTimeout())
                .setGameMode(GameMode.ADVENTURE)
                .setShouldRespawn(true)
                .setShouldUpdateTags(true);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPostLogin(LoginLimboRegisterEvent event) {
        Player player = event.getPlayer();
        if (passedCaptchaPlayers.contains(player.getUniqueId())) {
            SBlockVelocity.getLimboFactory().passLoginLimbo(player);
            return;
        }

        event.addOnJoinCallback(() -> new CaptchaSession(this.server, player, plugin).start());
    }

    public static LimboFactory getLimboFactory() {
        return limboFactory;
    }

    public static VirtualWorld getFilterWorld() {
        return filterWorld;
    }

    public static SBlockVelocity getPlugin() {
        return plugin;
    }

    public void addPassedPlayer(UUID playerUuid) {
        passedCaptchaPlayers.add(playerUuid);
    }

    public PluginConfig getConfig() {
        return config;
    }


}