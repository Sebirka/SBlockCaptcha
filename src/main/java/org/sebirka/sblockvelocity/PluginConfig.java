package org.sebirka.sblockvelocity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginConfig {
    private final Map<String, Object> configMap;
    private final Map<String, Object> cache = new HashMap<>();

    public PluginConfig(Map<String, Object> configMap) {
        this.configMap = configMap != null ? configMap : Collections.emptyMap();
    }

    private <T> T getFromCache(String key, T defaultValue) {
        if (cache.containsKey(key)) {
            return (T) cache.get(key);
        }
        T value = get(key, defaultValue);
        cache.put(key, value);
        return value;
    }

    private <T> T get(String path, T defaultValue) {
        String[] parts = path.split("\\.");
        Map<String, Object> currentMap = this.configMap;
        Object value = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (currentMap != null && currentMap.containsKey(part)) {
                value = currentMap.get(part);
                if (i < parts.length - 1) {
                    if (value instanceof Map) {
                        currentMap = (Map<String, Object>) value;
                    } else {
                        return defaultValue;
                    }
                }
            } else {
                return defaultValue;
            }
        }
        if (value != null) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int getCaptchaDigitsToDraw() {
        return getFromCache("captcha.digits_to_draw", 4);
    }

    public int getCaptchaMaxFailedAttempts() {
        return getFromCache("captcha.max_failed_attempts", 3);
    }

    public int getCaptchaTimeoutMinutes() {
        return getFromCache("captcha.timeout_minutes", 2);
    }

    public String getCaptchaPlatformBlock() {
        return getFromCache("captcha.platform_block", "minecraft:snow_block");
    }

    public String getCaptchaDigitBlock() {
        return getFromCache("captcha.digit_block", "minecraft:redstone_block");
    }

    public int getCaptchaPlatformSize() {
        return getFromCache("captcha.platform_size", 5);
    }

    public int getCaptchaStartY() {
        return getFromCache("captcha.captcha_start_y", 101);
    }

    public int getCaptchaStartZ() {
        return getFromCache("captcha.captcha_start_z", 10);
    }

    public int getCaptchaMessageDelaySeconds() {
        return getFromCache("captcha.captcha_message_delay_seconds", 5);
    }

    public int getCaptchaMessageRepeatSeconds() {
        return getFromCache("captcha.captcha_message_repeat_seconds", 15);
    }

    public int getChatClearLines() {
        return getFromCache("captcha.chat_clear_lines", 20);
    }

    public String getMessageErrorLoadingCaptcha() {
        return getFromCache("messages.error_loading_captcha", "<red>Произошла ошибка при загрузке капчи. Пожалуйста, попробуйте еще раз.</red>");
    }

    public String getMessagePlayerLeftCaptchaZone() {
        return getFromCache("messages.player_left_captcha_zone", "<red>Вы покинули зону капчи.</red>");
    }

    public String getMessageTooManyFailedAttempts() {
        return getFromCache("messages.too_many_failed_attempts", "<red>Слишком много неверных попыток.</red>");
    }

    public String getMessageCaptchaIncorrect() {
        return getFromCache("messages.captcha_incorrect", "<red>Неверно. Попробуйте еще раз. Осталось попыток: %attempts_left%</red>");
    }

    public String getMessageCaptchaTimeout() {
        return getFromCache("messages.captcha_timeout", "<red>Превышено время прохождения капчи.</red>");
    }

    public String getMessageCaptchaInstructionMain() {
        return getFromCache("messages.captcha_instruction_main", "<gold><bold>Captcha</bold></gold>");
    }

    public String getMessageCaptchaInstructionPrefix() {
        return getFromCache("messages.captcha_instruction_prefix", "<dark_gray> » </dark_gray>");
    }

    public List<String> getCaptchaInstruction() {
        return getFromCache("messages.captcha_instruction", Arrays.asList(
                "<reset><white>Пожалуйста, введите цифры, которые вы видите.</white>",
                "<reset><bold><gold>1.</gold></bold> <white>Посмотрите на блоки перед вами.</white>",
                "<reset><bold><gold>2.</gold></bold> <white>Введите 4 цифры в чат </white><yellow>СЛЕВА НАПРАВО</yellow><white>.</white>",
                "<reset><bold><gold>3.</gold></bold> <white>Нажмите Enter.</white>"
        ));
    }

    public int getLimboFilterWorldSpawnX() {
        return getFromCache("limbo.filter_world_spawn_x", 100);
    }

    public int getLimboFilterWorldSpawnY() {
        return getFromCache("limbo.filter_world_spawn_y", 100);
    }

    public int getLimboFilterWorldSpawnZ() {
        return getFromCache("limbo.filter_world_spawn_z", 100);
    }

    public int getLimboFilterWorldPlatformY() {
        return getFromCache("limbo.filter_world_platform_y", 10);
    }

    public int getLimboFilterWorldPlatformMinX() {
        return getFromCache("limbo.filter_world_platform_min_x", 92);
    }

    public int getLimboFilterWorldPlatformMaxX() {
        return getFromCache("limbo.filter_world_platform_max_x", 108);
    }

    public int getLimboFilterWorldPlatformMinZ() {
        return getFromCache("limbo.filter_world_platform_min_z", 92);
    }

    public int getLimboFilterWorldPlatformMaxZ() {
        return getFromCache("limbo.filter_world_platform_max_z", 108);
    }

    public String getLimboFilterServerName() {
        return getFromCache("limbo.filter_server_name", "BubbleCaptcha");
    }

    public int getLimboFilterServerReadTimeout() {
        return getFromCache("limbo.filter_server_read_timeout", 10000);
    }
}