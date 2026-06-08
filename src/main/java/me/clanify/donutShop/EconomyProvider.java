package me.clanify.donutShop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyProvider {
   public final Economy econ;

   public EconomyProvider(DonutShop plugin) {
      RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) {
         plugin.getLogger().severe("Vault not found! Disabling plugin.");
         plugin.getServer().getPluginManager().disablePlugin(plugin);
         this.econ = null;
      } else {
         this.econ = (Economy)rsp.getProvider();
      }

   }

   public boolean hasEnough(Player p, double amt) {
      return this.econ.has(p, amt);
   }

   public void withdraw(Player p, double amt) {
      this.econ.withdrawPlayer(p, amt);
   }
}
