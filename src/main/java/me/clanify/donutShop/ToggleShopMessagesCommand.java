package me.clanify.donutShop;

import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleShopMessagesCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player p)) {
         sender.sendMessage("Only in-game players may run this command.");
         return true;
      } else {
         UUID uuid = p.getUniqueId();
         DonutShop plugin = DonutShop.get();
         boolean currently = plugin.isShopMessagesEnabled(uuid);
         boolean next = !currently;
         plugin.setShopMessagesEnabled(uuid, next);
         if (next) {
            p.sendMessage(Utils.formatColors("&aYou will now receive shop messages."));
         } else {
            p.sendMessage(Utils.formatColors("&cYou will no longer receive shop messages."));
         }

         return true;
      }
   }
}
