package me.clanify.donutShop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player p)) {
         return true;
      } else if (!p.hasPermission("donutshop.open")) {
         String noPerm = DonutShop.get().getConfigManager().getRootConfig().getString("messages.no-permission", "&cYou don’t have permission.");
         Utils.sendMessage(p, noPerm);
         return true;
      } else {
         MenuManager.openMainMenu(p);
         return true;
      }
   }
}
