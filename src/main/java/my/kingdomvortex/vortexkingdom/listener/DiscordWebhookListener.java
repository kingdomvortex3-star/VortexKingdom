
package my.kingdomvortex.vortexkingdom.listener;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public final class DiscordWebhookListener implements Listener {

    private final VortexKingdom plugin;

    public DiscordWebhookListener(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        sendNotification("join", event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendNotification("quit", event.getPlayer());
    }

    private void sendNotification(String type, Player player) {
        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("discord-webhook.enabled", false)) {
            return;
        }

        if (!config.getBoolean(
                "discord-webhook." + type + ".enabled",
                true
        )) {
            return;
        }

        String webhookUrl = config.getString(
                "discord-webhook.url",
                ""
        );

        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String username = config.getString(
                "discord-webhook.username",
                "VortexKingdom"
        );

        String avatarUrl = config.getString(
                "discord-webhook.avatar-url",
                ""
        );

        String title = config.getString(
                "discord-webhook." + type + ".title",
                type.equals("join")
                        ? "Player Joined"
                        : "Player Left"
        );

        String description = config.getString(
                "discord-webhook." + type + ".description",
                type.equals("join")
                        ? "**{player}** joined the server."
                        : "**{player}** left the server."
        );

        int color = config.getInt(
                "discord-webhook." + type + ".color",
                type.equals("join") ? 5763719 : 15548997
        );

        description = description
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString());

        StringBuilder fields = new StringBuilder();

        fields.append("{")
                .append("\"name\":\"Player\",")
                .append("\"value\":\"")
                .append(jsonEscape(player.getName()))
                .append("\",")
                .append("\"inline\":true")
                .append("}");

        if (config.getBoolean(
                "discord-webhook.include-uuid",
                true
        )) {
            fields.append(",{")
                    .append("\"name\":\"UUID\",")
                    .append("\"value\":\"")
                    .append(player.getUniqueId())
                    .append("\",")
                    .append("\"inline\":false")
                    .append("}");
        }

        String payload = "{"
                + "\"username\":\"" + jsonEscape(username) + "\","
                + (avatarUrl == null || avatarUrl.isBlank()
                    ? ""
                    : "\"avatar_url\":\""
                        + jsonEscape(avatarUrl) + "\",")
                + "\"embeds\":[{"
                + "\"title\":\"" + jsonEscape(title) + "\","
                + "\"description\":\""
                    + jsonEscape(description) + "\","
                + "\"color\":" + color + ","
                + "\"fields\":[" + fields + "],"
                + "\"footer\":{\"text\":\"VortexKingdom Network\"},"
                + "\"timestamp\":\"" + Instant.now() + "\""
                + "}]"
                + "}";

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> sendHttpRequest(webhookUrl, payload)
        );
    }

    private void sendHttpRequest(String webhookUrl, String payload) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    URI.create(webhookUrl).toURL().openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "VortexKingdom-DiscordWebhook"
            );

            byte[] data = payload.getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(data.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(data);
                output.flush();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning(
                        "Discord webhook failed. HTTP status: "
                                + responseCode
                );
            }

        } catch (Exception exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to send Discord webhook notification.",
                    exception
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String jsonEscape(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
