package org.sebirka.sblockvelocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class CaptchaSession implements LimboSessionHandler {
    private final ProxyServer server;
    private final Player player;
    private final SBlockVelocity plugin;
    private LimboPlayer limboPlayer;
    private Limbo limboServer;
    private VirtualWorld captchaWorld;
    private ScheduledTask captchaMessageTask;
    private ScheduledTask timeoutTask;
    private int[] drawnDigits;
    public static final Map<Integer, boolean[][]> DIGIT_PATTERNS = new HashMap<>();
    private int failedAttempts = 0;
    private final int maxFailedAttempts;
    private boolean lastMessageWasError = false;

    static {
        DIGIT_PATTERNS.put(0, new boolean[][]{{true, true, true}, {true, false, true}, {true, false, true}, {true, false, true}, {true, true, true}});
        DIGIT_PATTERNS.put(1, new boolean[][]{{false, true, false}, {true, true, false}, {false, true, false}, {false, true, false}, {true, true, true}});
        DIGIT_PATTERNS.put(2, new boolean[][]{{true, true, true}, {false, false, true}, {true, true, true}, {true, false, false}, {true, true, true}});
        DIGIT_PATTERNS.put(3, new boolean[][]{{true, true, true}, {false, false, true}, {true, true, true}, {false, false, true}, {true, true, true}});
        DIGIT_PATTERNS.put(4, new boolean[][]{{true, false, true}, {true, false, true}, {true, true, true}, {false, false, true}, {false, false, true}});
        DIGIT_PATTERNS.put(5, new boolean[][]{{true, true, true}, {true, false, false}, {true, true, true}, {false, false, true}, {true, true, true}});
        DIGIT_PATTERNS.put(6, new boolean[][]{{true, true, true}, {true, false, false}, {true, true, true}, {true, false, true}, {true, true, true}});
        DIGIT_PATTERNS.put(7, new boolean[][]{{true, true, true}, {false, false, true}, {false, true, false}, {true, false, false}, {true, false, false}});
        DIGIT_PATTERNS.put(8, new boolean[][]{{true, true, true}, {true, false, true}, {true, true, true}, {true, false, true}, {true, true, true}});
        DIGIT_PATTERNS.put(9, new boolean[][]{{true, true, true}, {true, false, true}, {true, true, true}, {false, false, true}, {true, true, true}});
    }

    public CaptchaSession(ProxyServer server, Player player, SBlockVelocity plugin) {
        this.server = server;
        this.plugin = plugin;
        this.player = player;
        this.maxFailedAttempts = plugin.getConfig().getCaptchaMaxFailedAttempts();
        this.drawnDigits = new int[plugin.getConfig().getCaptchaDigitsToDraw()];
    }

    public void start() {
        try {
            int platformSize = plugin.getConfig().getCaptchaPlatformSize();
            int captchaStartY = plugin.getConfig().getCaptchaStartY();
            int captchaStartZ = plugin.getConfig().getCaptchaStartZ();
            int spawnY = 100;
            this.captchaWorld = SBlockVelocity.getLimboFactory().createVirtualWorld(
                    Dimension.OVERWORLD,
                    0,
                    spawnY,
                    0,
                    0f,
                    0f
            );

            for (int chunkX = -2; chunkX <= 2; chunkX++) {
                for (int chunkZ = -2; chunkZ <= 2; chunkZ++) {
                    SBlockVelocity.getLimboFactory().createVirtualChunk(chunkX, chunkZ);
                }
            }

            drawStaticPlatform(platformSize);
            drawRandomDigits(captchaStartY, captchaStartZ);

            this.limboServer = SBlockVelocity.getLimboFactory().createLimbo(this.captchaWorld)
                    .setName("SBlock_Captcha_" + player.getUniqueId().toString().substring(0, 8))
                    .setReadTimeout(plugin.getConfig().getLimboFilterServerReadTimeout())
                    .setGameMode(GameMode.ADVENTURE)
                    .setShouldRespawn(true)
                    .setShouldUpdateTags(true);
            this.limboServer.spawnPlayer(player, this);

            sendTitle(player,
                    plugin.getConfig().getCaptchaTitleMain(),
                    plugin.getConfig().getCaptchaTitleSubtitle(),
                    MiniMessage.miniMessage().deserialize(plugin.getConfig().getCaptchaTitleMain()).color(),
                    MiniMessage.miniMessage().deserialize(plugin.getConfig().getCaptchaTitleSubtitle()).color(),
                    0, 2500, 0);

        } catch (Throwable t) {
            player.disconnect(MiniMessage.miniMessage().deserialize(plugin.getConfig().getMessageErrorLoadingCaptcha()));

            cleanup();
        }
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer limboPlayer) {
        this.limboPlayer = limboPlayer;
        this.limboPlayer.teleport(0, 100, 0, 0f, 0f);
        this.limboPlayer.disableFalling();
        scheduleTimeout();
        captchaMessageTask = this.server.getScheduler()
                .buildTask(plugin, () -> {
                    if (!lastMessageWasError) {
                        sendCaptchamessage(player, plugin.getConfig().getChatClearLines());
                    }
                    lastMessageWasError = false;
                })
                .delay(plugin.getConfig().getCaptchaMessageDelaySeconds(), TimeUnit.SECONDS)
                .repeat(plugin.getConfig().getCaptchaMessageRepeatSeconds(), TimeUnit.SECONDS)
                .schedule();
    }

    @Override
    public void onMove(double posX, double posY, double posZ) {
        if (posY < 99 && limboPlayer != null) {
            player.disconnect(MiniMessage.miniMessage().deserialize(plugin.getConfig().getMessagePlayerLeftCaptchaZone()));

            cleanup();
        } else if (limboPlayer != null && (Math.abs(posX - 0) > 0.1 || Math.abs(posZ - 0) > 0.1)) {
            limboPlayer.teleport(0, 100, 0, 0f, 0f);
        }
    }

    @Override
    public void onChat(String message) {
        StringBuilder expectedDigitsBuilder = new StringBuilder();
        for (int digit : drawnDigits) {
            expectedDigitsBuilder.append(digit);
        }
        String expectedCaptcha = expectedDigitsBuilder.toString();
        String reversedCaptcha = new StringBuilder(expectedCaptcha).reverse().toString(); // Создаем зеркальную строку

        String trimmedMessage = message.trim();
        if (trimmedMessage.equals(reversedCaptcha)) {
            SBlockVelocity.getLimboFactory().passLoginLimbo(player);
            SBlockVelocity.getPlugin().addPassedPlayer(player.getUniqueId());
            cleanup();
        } else {
            failedAttempts++;
            if (failedAttempts >= maxFailedAttempts) {
                player.disconnect(MiniMessage.miniMessage().deserialize(plugin.getConfig().getMessageTooManyFailedAttempts()));
                cleanup();
                return;
            }
            lastMessageWasError = true;
            Component errorMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getMessageCaptchaIncorrect()
                            .replace("%attempts_left%", String.valueOf(maxFailedAttempts - failedAttempts))
            );
            player.sendMessage(errorMessage);
            server.getScheduler().buildTask(plugin, () -> {
                sendCaptchamessage(player, plugin.getConfig().getChatClearLines());
                lastMessageWasError = false;
            }).delay(2, TimeUnit.SECONDS).schedule(); 
        }
    }

    private void scheduleTimeout() {
        if (plugin != null) {
            timeoutTask = server.getScheduler()
                    .buildTask(plugin, () -> {
                        player.disconnect(MiniMessage.miniMessage().deserialize(plugin.getConfig().getMessageCaptchaTimeout()));

                        cleanup();
                    })
                    .delay(plugin.getConfig().getCaptchaTimeoutMinutes(), TimeUnit.MINUTES)
                    .schedule();
        }
    }

    @Override
    public void onDisconnect() {

        cleanup();
    }

    private void drawStaticPlatform(int platformSize) {
        String platformBlock = plugin.getConfig().getCaptchaPlatformBlock();
        for (int x = -platformSize; x <= platformSize; x++) {
            for (int z = -platformSize; z <= platformSize; z++) {
                captchaWorld.setBlock(x, 99, z,
                        SBlockVelocity.getLimboFactory().createSimpleBlock(platformBlock));
            }
        }
    }

    void drawRandomDigits(int startY, int startZ) {
        Random random = new Random();
        int currentX = -7;
        String blockType = plugin.getConfig().getCaptchaDigitBlock();
        int digitsToDraw = plugin.getConfig().getCaptchaDigitsToDraw();
        for (int i = 0; i < digitsToDraw; i++) {
            int digit = random.nextInt(10);
            drawnDigits[i] = digit;
            drawDigit(digit, currentX, startY, startZ, blockType);
            currentX += 4;
        }
    }
    boolean[][] getMirroredPattern(boolean[][] originalPattern) {
        if (originalPattern == null || originalPattern.length == 0) {
            return originalPattern;
        }
        int height = originalPattern.length;
        int width = originalPattern[0].length;
        boolean[][] mirrored = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mirrored[y][x] = originalPattern[y][width - 1 - x];
            }
        }
        return mirrored;
    }

    private void drawDigit(int digit, int startX, int startY, int startZ, String blockType) {
        boolean[][] originalPattern = DIGIT_PATTERNS.get(digit);
        if (originalPattern == null) {
            return;
        }
        boolean[][] patternToDraw = getMirroredPattern(originalPattern);
        for (int y = 0; y < patternToDraw.length; y++) {
            for (int x = 0; x < patternToDraw[0].length; x++) {
                if (patternToDraw[y][x]) {
                    this.captchaWorld.setBlock(startX + x, startY + (patternToDraw.length - 1 - y), startZ,
                            SBlockVelocity.getLimboFactory().createSimpleBlock(blockType));
                }
            }
        }
    }
    private void cleanup() {
        if (captchaMessageTask != null) {
            captchaMessageTask.cancel();
            captchaMessageTask = null;
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        if (limboServer != null) {
            limboServer.dispose();
            limboServer = null;
        }
        captchaWorld = null;
        limboPlayer = null;
    }

    public static void sendCaptchamessage(Player player, int chatclear) {
        SBlockVelocity plugin = SBlockVelocity.getPlugin();
        for (int i = 0; i < chatclear; i++) {
            player.sendMessage(Component.text(" "));
        }
        String[] lines = {
                plugin.getConfig().getMessageCaptchaInstructionLine1(),
                plugin.getConfig().getMessageCaptchaInstructionLine2(),
                plugin.getConfig().getMessageCaptchaInstructionLine3(),
                plugin.getConfig().getMessageCaptchaInstructionLine4()
        };

        for (String line : lines) {
            Component fullLine = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getMessageCaptchaInstructionMain() +
                            plugin.getConfig().getMessageCaptchaInstructionPrefix() +
                            line
            );
            player.sendMessage(fullLine);
        }

        player.sendMessage(Component.text(" "));
    }

    public static void sendTitle(Player player, String titleMiniMessage, String subtitleMiniMessage,
                                 TextColor color, TextColor color2, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                MiniMessage.miniMessage().deserialize(titleMiniMessage),
                MiniMessage.miniMessage().deserialize(subtitleMiniMessage),
                Title.Times.times(
                        Duration.ofMillis(fadeIn),
                        Duration.ofMillis(stay),
                        Duration.ofMillis(fadeOut)
                )
        ));
    }
}