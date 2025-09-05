package org.sebirka.sblockvelocity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PluginConfig {
    private final Map<String, Object> configMap;

    public PluginConfig(Map<String, Object> configMap) {
        this.configMap = configMap != null ? configMap : Collections.emptyMap();
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
        return get("captcha.digits_to_draw", 4);
    }

    public int getCaptchaMaxFailedAttempts() {
        return get("captcha.max_failed_attempts", 3);
    }

    public int getCaptchaTimeoutMinutes() {
        return get("captcha.timeout_minutes", 2);
    }

    public String getCaptchaPlatformBlock() {
        return get("captcha.platform_block", "minecraft:snow_block");
    }

    public String getCaptchaDigitBlock() {
        return get("captcha.digit_block", "minecraft:redstone_block");
    }

    public int getCaptchaPlatformSize() {
        return get("captcha.platform_size", 5);
    }

    public int getCaptchaStartY() {
        return get("captcha.captcha_start_y", 101);
    }

    public int getCaptchaStartZ() {
        return get("captcha.captcha_start_z", 10);
    }

    public int getCaptchaMessageDelaySeconds() {
        return get("captcha.captcha_message_delay_seconds", 5);
    }

    public int getCaptchaMessageRepeatSeconds() {
        return get("captcha.captcha_message_repeat_seconds", 15);
    }

    public int getChatClearLines() {
        return get("captcha.chat_clear_lines", 20);
    }

    public String getMessageErrorLoadingCaptcha() {
        return get("messages.error_loading_captcha", "<red>Произошла ошибка при загрузке капчи. Пожалуйста, попробуйте еще раз.</red>");
    }

    public String getMessagePlayerLeftCaptchaZone() {
        return get("messages.player_left_captcha_zone", "<red>Вы покинули зону капчи.</red>");
    }

    public String getMessageTooManyFailedAttempts() {
        return get("messages.too_many_failed_attempts", "<red>Слишком много неверных попыток.</red>");
    }

    public String getMessageCaptchaIncorrect() {
        return get("messages.captcha_incorrect", "<red>Неверно. Попробуйте еще раз. Осталось попыток: %attempts_left%</red>");
    }

    public String getMessageCaptchaTimeout() {
        return get("messages.captcha_timeout", "<red>Превышено время прохождения капчи.</red>");
    }

    public String getCaptchaTitleMain() {
        return get("messages.captcha_title_main", "<gold><bold>BUBBLEGRIEF</bold></gold>");
    }

    public String getCaptchaTitleSubtitle() {
        return get("messages.captcha_title_subtitle", "<white>Идет проверка</white>");
    }

    public String getMessageCaptchaInstructionMain() {
        return get("messages.captcha_instruction_main", "<gold><bold>Captcha</bold></gold>");
    }

    public String getMessageCaptchaInstructionPrefix() {
        return get("messages.captcha_instruction_prefix", "<dark_gray> » </dark_gray>");
    }

    public String getMessageCaptchaInstructionLine1() {
        return get("messages.captcha_instruction_line1", "<white>Пожалуйста, введите цифры, которые вы видите.</white>");
    }

    public String getMessageCaptchaInstructionLine2() {
        return get("messages.captcha_instruction_line2", "<gold><bold>1.</bold></gold> <white>Посмотрите на блоки перед вами.</white>");
    }

    public String getMessageCaptchaInstructionLine3() {
        return get("messages.captcha_instruction_line3", "<gold><bold>2.</bold></gold> <white>Введите 4 цифры в чат </white><yellow><bold>СЛЕВА НАПРАВО</bold></yellow><white>.</white>");
    }

    public String getMessageCaptchaInstructionLine4() {
        return get("messages.captcha_instruction_line4", "<gold><bold>3.</bold></gold> <white>Нажмите Enter.</white>");
    }

    public int getLimboFilterWorldSpawnX() {
        return get("limbo.filter_world_spawn_x", 100);
    }

    public int getLimboFilterWorldSpawnY() {
        return get("limbo.filter_world_spawn_y", 100);
    }

    public int getLimboFilterWorldSpawnZ() {
        return get("limbo.filter_world_spawn_z", 100);
    }

    public int getLimboFilterWorldPlatformY() {
        return get("limbo.filter_world_platform_y", 10);
    }

    public int getLimboFilterWorldPlatformMinX() {
        return get("limbo.filter_world_platform_min_x", 92);
    }

    public int getLimboFilterWorldPlatformMaxX() {
        return get("limbo.filter_world_platform_max_x", 108);
    }

    public int getLimboFilterWorldPlatformMinZ() {
        return get("limbo.filter_world_platform_min_z", 92);
    }

    public int getLimboFilterWorldPlatformMaxZ() {
        return get("limbo.filter_world_platform_max_z", 108);
    }

    public String getLimboFilterServerName() {
        return get("limbo.filter_server_name", "BubbleCaptcha");
    }

    public int getLimboFilterServerReadTimeout() {
        return get("limbo.filter_server_read_timeout", 10000);
    }
}

