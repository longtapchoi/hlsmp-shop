package me.clanify.donutShop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DonutShopCommand implements TabExecutor {
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!sender.hasPermission("donutshop.admin")) {
         String noPerm = DonutShop.get().getConfigManager().getRootConfig().getString("messages.no-permission", "&cYou don’t have permission.");
         if (sender instanceof Player) {
            Player p = (Player)sender;
            MenuManager.sendToPlayer(p, noPerm);
         } else {
            sender.sendMessage(Utils.formatColors(noPerm));
         }

         return true;
      } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
         DonutShop.get().getConfigManager().reload();
         String msg = DonutShop.get().getConfigManager().getRootConfig().getString("messages.reload", "&aConfig reloaded!");
         if (sender instanceof Player) {
            Player p = (Player)sender;
            MenuManager.sendToPlayer(p, msg);
         } else {
            sender.sendMessage(Utils.formatColors(msg));
         }

         return true;
      } else if (args.length == 2 && args[0].equalsIgnoreCase("addcategory")) {
         String newShopName = args[1].toLowerCase().replaceAll("[^a-z0-9_\\-]", "");
         if (newShopName.isEmpty()) {
            if (sender instanceof Player) {
               Player p = (Player)sender;
               MenuManager.sendToPlayer(p, "&cInvalid shop name.");
            } else {
               sender.sendMessage(Utils.formatColors("&cInvalid shop name."));
            }

            return true;
         } else {
            ConfigManager cfgMgr = DonutShop.get().getConfigManager();
            if (cfgMgr.getShopKeys().contains(newShopName)) {
               if (sender instanceof Player) {
                  Player p = (Player)sender;
                  MenuManager.sendToPlayer(p, "&cA shop named '" + newShopName + "' already exists.");
               } else {
                  sender.sendMessage(Utils.formatColors("&cA shop named '" + newShopName + "' already exists."));
               }

               return true;
            } else {
               boolean success = cfgMgr.createNewShop(newShopName);
               if (!success) {
                  if (sender instanceof Player) {
                     Player p = (Player)sender;
                     MenuManager.sendToPlayer(p, "&cFailed to create new shop: '" + newShopName + "'.");
                  } else {
                     sender.sendMessage(Utils.formatColors("&cFailed to create new shop: '" + newShopName + "'."));
                  }

                  return true;
               } else {
                  if (sender instanceof Player) {
                     Player p = (Player)sender;
                     MenuManager.sendToPlayer(p, "&aCreated new shop category: &e" + newShopName);
                  } else {
                     sender.sendMessage(Utils.formatColors("&aCreated new shop category: &e" + newShopName));
                  }

                  return true;
               }
            }
         }
      } else if (args.length == 2 && args[0].equalsIgnoreCase("removecategory")) {
         String shopName = args[1].toLowerCase().replaceAll("[^a-z0-9_\\-]", "");
         if (shopName.isEmpty()) {
            if (sender instanceof Player) {
               Player p = (Player)sender;
               MenuManager.sendToPlayer(p, "&cInvalid shop name.");
            } else {
               sender.sendMessage(Utils.formatColors("&cInvalid shop name."));
            }

            return true;
         } else {
            ConfigManager cfgMgr = DonutShop.get().getConfigManager();
            if (!cfgMgr.getShopKeys().contains(shopName)) {
               if (sender instanceof Player) {
                  Player p = (Player)sender;
                  MenuManager.sendToPlayer(p, "&cShop category '" + shopName + "' does not exist.");
               } else {
                  sender.sendMessage(Utils.formatColors("&cShop category '" + shopName + "' does not exist."));
               }

               return true;
            } else {
               boolean ok = cfgMgr.removeShopCategory(shopName);
               if (ok) {
                  if (sender instanceof Player) {
                     Player p = (Player)sender;
                     MenuManager.sendToPlayer(p, "&aRemoved shop category: &e" + shopName);
                  } else {
                     sender.sendMessage(Utils.formatColors("&aRemoved shop category: &e" + shopName));
                  }
               } else if (sender instanceof Player) {
                  Player p = (Player)sender;
                  MenuManager.sendToPlayer(p, "&cFailed to remove shop category: &e" + shopName);
               } else {
                  sender.sendMessage(Utils.formatColors("&cFailed to remove shop category: &e" + shopName));
               }

               return true;
            }
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.formatColors("&cOnly players can run this command."));
            return true;
         } else {
            Player player = (Player)sender;
            String category = args[1].toLowerCase();

            double price;
            try {
               price = Double.parseDouble(args[2]);
            } catch (NumberFormatException var24) {
               MenuManager.sendToPlayer(player, "&cPrice must be a number. Usage: /donutshop addhanditem <category> <price>");
               return true;
            }

            ConfigManager cfgMgr = DonutShop.get().getConfigManager();
            if (!cfgMgr.getShopKeys().contains(category)) {
               MenuManager.sendToPlayer(player, "&cShop category '" + category + "' does not exist.");
               return true;
            } else {
               ItemStack inHand = player.getInventory().getItemInMainHand();
               if (inHand != null && inHand.getType() != Material.AIR) {
                  FileConfiguration shopCfg = cfgMgr.getShopConfig(category);
                  int rows = shopCfg.getInt("rows", 3);
                  int totalSlots = rows * 9;
                  Set<Integer> usedSlots = new HashSet();
                  if (shopCfg.isConfigurationSection("items")) {
                     ConfigurationSection itemsSection = shopCfg.getConfigurationSection("items");

                     for(String itemKey : itemsSection.getKeys(false)) {
                        ConfigurationSection iSec = itemsSection.getConfigurationSection(itemKey);
                        if (iSec != null && iSec.isSet("slot")) {
                           usedSlots.add(iSec.getInt("slot"));
                        }
                     }
                  }

                  if (shopCfg.isConfigurationSection("back-button")) {
                     usedSlots.add(shopCfg.getConfigurationSection("back-button").getInt("slot"));
                  }

                  int freeSlot = -1;

                  for(int s = 0; s < totalSlots; ++s) {
                     if (!usedSlots.contains(s)) {
                        freeSlot = s;
                        break;
                     }
                  }

                  if (freeSlot < 0) {
                     MenuManager.sendToPlayer(player, "&cNo free slot found in shop '" + category + "'.");
                     return true;
                  } else {
                     String matNameLower = inHand.getType().name().toLowerCase();
                     String itemKey = matNameLower + "_" + System.currentTimeMillis();
                     String basePath = "items." + itemKey + ".";
                     shopCfg.set(basePath + "price", price);
                     shopCfg.set(basePath + "slot", freeSlot);
                     shopCfg.set(basePath + "giveitem", true);
                     shopCfg.set(basePath + "givelore", false);
                     shopCfg.set(basePath + "command", "");
                     shopCfg.set(basePath + "material", inHand.getType().name());
                     shopCfg.set(basePath + "displayname", "");
                     shopCfg.set(basePath + "amount", 1);
                     List<String> defaultLore = new ArrayList();
                     String shopCurrency = cfgMgr.getShopConfig(category).getString("currency-suffix", "$");
                     defaultLore.add("&fBuy Price: &#0bf52b" + Utils.formatCurrency(price, shopCurrency));
                     shopCfg.set(basePath + "lore", defaultLore);
                     if (!inHand.getEnchantments().isEmpty()) {
                        List<String> enchList = new ArrayList();

                        for(Map.Entry<Enchantment, Integer> entry : inHand.getEnchantments().entrySet()) {
                           String keyName = ((Enchantment)entry.getKey()).getKey().getKey();
                           enchList.add(keyName + ";" + String.valueOf(entry.getValue()));
                        }

                        shopCfg.set(basePath + "enchantments", enchList);
                     }

                     cfgMgr.saveShopConfig(category);
                     String rawMat = inHand.getType().name();
                     String niceName = (String)Arrays.stream(rawMat.split("_")).map((s) -> {
                        String var10000 = s.substring(0, 1).toUpperCase();
                        return var10000 + s.substring(1).toLowerCase();
                     }).collect(Collectors.joining(" "));
                     MenuManager.sendToPlayer(player, "&aAdded &e" + niceName + " &ato shop &e" + category + " &afor &e$" + Utils.formatNumber(price) + ".");
                     return true;
                  }
               } else {
                  MenuManager.sendToPlayer(player, "&cYou must be holding an item to add.");
                  return true;
               }
            }
         }
      } else {
         String u1 = "&cUsage: /donutshop reload";
         String u2 = "&cUsage: /donutshop addcategory <name>";
         String u3 = "&cUsage: /donutshop removecategory <name>";
         String u4 = "&cUsage: /donutshop addhanditem <category> <price>";
         if (sender instanceof Player) {
            Player p = (Player)sender;
            MenuManager.sendToPlayer(p, u1);
            MenuManager.sendToPlayer(p, u2);
            MenuManager.sendToPlayer(p, u3);
            MenuManager.sendToPlayer(p, u4);
         } else {
            sender.sendMessage(Utils.formatColors(u1));
            sender.sendMessage(Utils.formatColors(u2));
            sender.sendMessage(Utils.formatColors(u3));
            sender.sendMessage(Utils.formatColors(u4));
         }

         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      if (args.length == 1) {
         List<String> subs = List.of("reload", "addcategory", "removecategory", "addhanditem");
         return (List)subs.stream().filter((s) -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
      } else if (args.length == 2 && args[0].equalsIgnoreCase("removecategory")) {
         ConfigManager cfgMgr = DonutShop.get().getConfigManager();
         List<String> shops = new ArrayList(cfgMgr.getShopKeys());
         return (List)shops.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
      } else if (args.length == 2 && args[0].equalsIgnoreCase("addhanditem")) {
         ConfigManager cfgMgr = DonutShop.get().getConfigManager();
         List<String> shops = new ArrayList(cfgMgr.getShopKeys());
         return (List)shops.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
      } else {
         return Collections.emptyList();
      }
   }
}
