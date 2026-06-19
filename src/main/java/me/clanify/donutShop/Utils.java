package me.clanify.donutShop;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class Utils {
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
   private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#.#");

   private Utils() {
   }

   public static String formatColors(String input) {
      if (input == null) {
         return null;
      } else {
         Matcher matcher = HEX_PATTERN.matcher(input);
         StringBuffer buffer = new StringBuffer(input.length() + 32);

         while(matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder repl = new StringBuilder("§x");

            for(char c : hex.toCharArray()) {
               repl.append('§').append(c);
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(repl.toString()));
         }

         matcher.appendTail(buffer);
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   public static List<String> formatColors(List<String> lines) {
      return (List)lines.stream().map(Utils::formatColors).collect(Collectors.toList());
   }

   public static String formatNumber(double n) {
      if (n >= (double)1000000.0F) {
         return ONE_DECIMAL.format(n / (double)1000000.0F) + "M";
      } else if (n >= (double)1000.0F) {
         return ONE_DECIMAL.format(n / (double)1000.0F) + "K";
      } else {
         return n == (double)((long)n) ? String.valueOf((long)n) : ONE_DECIMAL.format(n);
      }
   }

   /**
    * Ghep so tien voi don vi (currency-suffix) cua shop tuong ung.
    * "$" duoc dat truoc so khong cach (vd "$300"), cac don vi khac duoc dat sau so co cach (vd "300 ᴄᴏɪɴ").
    */
   public static String formatCurrency(double amount, String currencySuffix) {
      String numberStr = formatNumber(amount);
      if (currencySuffix == null || currencySuffix.isBlank()) {
         return numberStr;
      } else if (currencySuffix.trim().equals("$")) {
         return "$" + numberStr;
      } else {
         return numberStr + " " + currencySuffix.trim();
      }
   }

   public static void sendMessage(Player player, String raw) {
      if (raw != null && player != null) {
         FileConfiguration cfg = DonutShop.get().getConfigManager().getRootConfig();
         if (cfg.getBoolean("enable-messages", true)) {
            if (DonutShop.get().isShopMessagesEnabled(player.getUniqueId())) {
               String formatted = formatColors(raw);
               player.sendMessage(formatted);
               if (cfg.getBoolean("use-actionbars", false)) {
                  BaseComponent[] comps = TextComponent.fromLegacyText(formatted);
                  player.spigot().sendMessage(ChatMessageType.ACTION_BAR, comps);
               }

            }
         }
      }
   }
}
