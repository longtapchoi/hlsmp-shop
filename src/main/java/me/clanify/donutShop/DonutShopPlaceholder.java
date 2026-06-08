package me.clanify.donutShop;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class DonutShopPlaceholder extends PlaceholderExpansion {
   private final DonutShop plugin;

   public DonutShopPlaceholder(DonutShop plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "donutshop";
   }

   public String getAuthor() {
      return this.plugin.getDescription().getAuthors().isEmpty() ? "Unknown" : (String)this.plugin.getDescription().getAuthors().get(0);
   }

   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public String onPlaceholderRequest(Player player, String identifier) {
      if (player == null) {
         return "";
      } else if (identifier.equalsIgnoreCase("totalspent")) {
         double total = this.plugin.getTotalSpent(player.getUniqueId());
         return this.formatBigNumber(total);
      } else {
         return null;
      }
   }

   private String formatBigNumber(double n) {
      if (n >= 1.0E12) {
         return String.format("%.2fT", n / 1.0E12);
      } else if (n >= (double)1.0E9F) {
         return String.format("%.2fB", n / (double)1.0E9F);
      } else if (n >= (double)1000000.0F) {
         return String.format("%.2fM", n / (double)1000000.0F);
      } else if (n >= (double)1000.0F) {
         return String.format("%.2fK", n / (double)1000.0F);
      } else {
         return n == (double)((long)n) ? String.format("%d", (long)n) : String.format("%.2f", n);
      }
   }
}
