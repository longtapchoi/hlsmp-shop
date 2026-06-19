package me.clanify.donutShop;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;

/**
 * Gui log giao dich mua hang len Discord qua webhook.
 * Moi shop (file shops/*.yml) co the khai bao "discord-webhook-url" rieng;
 * neu de trong thi khong gui gi ca cho shop do.
 *
 * QUAN TRONG: toan bo thao tac mang (HTTP) phai chay tren thread phu
 * (Bukkit.getScheduler().runTaskAsynchronously) de KHONG lam treo main thread / lag server,
 * vi ket noi mang co the mat hang tram ms cho phan hoi tu Discord.
 */
public final class DiscordWebhook {

   private DiscordWebhook() {
   }

   /**
    * Gui embed log mua hang. An toan goi tu main thread; ban than ham nay
    * se tu dong day phan ket noi mang sang thread phu.
    */
   public static void logPurchase(DonutShop plugin, String webhookUrl, String playerName, String shopKey,
                                   String itemName, int amount, double totalPrice, String currencySuffix) {
      if (webhookUrl == null || webhookUrl.isBlank()) {
         return;
      }

      if (!plugin.getConfigManager().getRootConfig().getBoolean("discord.enabled", true)) {
         return;
      }

      String priceFormatted = Utils.formatCurrency(totalPrice, currencySuffix);
      String json = buildPurchaseEmbedJson(playerName, shopKey, itemName, amount, priceFormatted);

      // Chay tren thread phu - khong duoc goi HTTP tren main thread vi se block toan bo server
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sendRaw(plugin, webhookUrl, json));
   }

   private static String buildPurchaseEmbedJson(String playerName, String shopKey, String itemName,
                                                  int amount, String priceFormatted) {
      String safePlayer = escapeJson(playerName);
      String safeShop = escapeJson(shopKey);
      String safeItem = escapeJson(itemName);
      String safePrice = escapeJson(priceFormatted);

      return "{"
            + "\"embeds\":[{"
            + "\"title\":\"\uD83D\uDED2 Giao dich moi\","
            + "\"color\":3066993,"
            + "\"fields\":["
            + "{\"name\":\"Nguoi mua\",\"value\":\"" + safePlayer + "\",\"inline\":true},"
            + "{\"name\":\"Vat pham\",\"value\":\"" + safeItem + " x" + amount + "\",\"inline\":true},"
            + "{\"name\":\"Gia\",\"value\":\"" + safePrice + "\",\"inline\":true},"
            + "{\"name\":\"Shop\",\"value\":\"" + safeShop + "\",\"inline\":true}"
            + "]"
            + "}]"
            + "}";
   }

   private static String escapeJson(String input) {
      if (input == null) {
         return "";
      }
      return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
   }

   private static void sendRaw(DonutShop plugin, String webhookUrl, String json) {
      try {
         URL url = new URL(webhookUrl);
         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
         conn.setRequestMethod("POST");
         conn.setRequestProperty("Content-Type", "application/json");
         conn.setDoOutput(true);
         conn.setConnectTimeout(5000);
         conn.setReadTimeout(5000);

         try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
         }

         int code = conn.getResponseCode();
         if (code >= 300) {
            plugin.getLogger().warning("Discord webhook tra ve ma loi " + code + " khi gui log mua hang.");
         }
         conn.disconnect();
      } catch (IOException e) {
         plugin.getLogger().warning("Khong the gui Discord webhook: " + e.getMessage());
      }
   }
}
