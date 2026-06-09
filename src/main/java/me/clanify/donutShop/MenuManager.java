package me.clanify.donutShop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class MenuManager {
   private static final DonutShop plugin = DonutShop.get();
   private static final Map<UUID, String> pendingShop = new HashMap();
   private static final Map<UUID, String> pendingItem = new HashMap();
   private static final Map<UUID, Integer> pendingAmount = new HashMap();
   private static final Map<UUID, Integer> openShopPage = new HashMap();
   private static final Map<UUID, Long> lastConfirmAt = new HashMap();

   public static boolean isShopInventory(String title) {
      FileConfiguration root = plugin.getConfigManager().getRootConfig();
      if (title.equals(Utils.formatColors(root.getString("main-menu.title")))) {
         return true;
      } else {
         for(String shopKey : plugin.getConfigManager().getShopKeys()) {
            FileConfiguration shopCfg = plugin.getConfigManager().getShopConfig(shopKey);
            if (shopCfg != null) {
               String baseTitle = Utils.formatColors(shopCfg.getString("title"));
               if (title.equals(baseTitle) || title.startsWith(baseTitle + " ")) {
                  return true;
               }
            }
         }

         String raw = root.getString("confirm-menu.title", "");
         if (!raw.isEmpty()) {
            boolean small = root.getBoolean("confirm-menu.use-small-font-title", false);
            String placeholder = "{item-name}";
            String rawPrefix = raw.contains(placeholder) ? raw.split(Pattern.quote(placeholder))[0] : raw;
            String prefix = Utils.formatColors(rawPrefix);
            if (small) {
               prefix = applySmallFont(prefix);
            }

            if (title.startsWith(prefix)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void openMainMenu(Player p) {
      FileConfiguration root = plugin.getConfigManager().getRootConfig();
      ConfigurationSection m = root.getConfigurationSection("main-menu");
      if (m != null) {
         int rows = m.getInt("rows", 3);
         String title = Utils.formatColors(m.getString("title", "&8Shop"));
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
         ConfigurationSection catSection = m.getConfigurationSection("categories");
         if (catSection != null) {
            for(String catKey : catSection.getKeys(false)) {
               ConfigurationSection c = catSection.getConfigurationSection(catKey);
               if (c != null) {
                  int slot = c.getInt("slot");
                  String matName = c.getString("material", "CHEST");
                  String displayName = c.getString("displayname", "&e" + catKey);
                  List<String> lore = c.getStringList("lore");
                  String action = c.getString("action", catKey);
                  if (plugin.getConfigManager().getShopKeys().contains(action)) {
                     inv.setItem(slot, makeItem(matName, 1, displayName, lore));
                  } else if (action.startsWith("command:")) {
                     inv.setItem(slot, makeItem(matName, 1, displayName, lore));
                  }
               }
            }
         }

         p.openInventory(inv);
         boolean play = root.getBoolean("play-open-sound", true);
         if (play) {
            String openSoundName = root.getString("sounds.open-sound", "ENTITY_PLAYER_LEVELUP");

            Sound openSound;
            try {
               openSound = Sound.valueOf(openSoundName);
            } catch (IllegalArgumentException var15) {
               openSound = Sound.ENTITY_PLAYER_LEVELUP;
            }

            p.playSound(p.getLocation(), openSound, 1.0F, 1.0F);
         }

      }
   }

   private static void openShopMenu(Player p, String shopKey) {
      openShopMenu(p, shopKey, 1);
   }

   private static void openShopMenu(Player p, String shopKey, int page) {
      openShopMenu(p, shopKey, page, false);
   }

   private static void openShopMenu(Player p, String shopKey, int page, boolean updateInPlace) {
      FileConfiguration shopCfg = plugin.getConfigManager().getShopConfig(shopKey);
      if (shopCfg == null) {
         p.sendMessage(Utils.formatColors("&cShop not found: " + shopKey));
      } else {
         openShopPage.put(p.getUniqueId(), page);
         int rows = shopCfg.getInt("rows", 3);
         int maxPage = getMaxPageCount(shopCfg);
         String baseTitle = Utils.formatColors(shopCfg.getString("title", shopKey));
         String title = maxPage > 1 ? baseTitle + " &7(" + page + "/" + maxPage + ")" : baseTitle;
         title = Utils.formatColors(title);

         Inventory inv;
         boolean reuseInv = false;
         if (updateInPlace && p.getOpenInventory() != null) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (top != null && top.getSize() == rows * 9) {
               inv = top;
               inv.clear();
               reuseInv = true;
            } else {
               inv = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
            }
         } else {
            inv = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
         }
         String costToken = shopCfg.getString("money-cost-placeholder", "%price%");
         String pageKey = page == 1 ? "items" : "page" + page;
         ConfigurationSection itemsSection = shopCfg.getConfigurationSection(pageKey);
         if (itemsSection != null) {
            for(String itemKey : itemsSection.getKeys(false)) {
               ConfigurationSection i = itemsSection.getConfigurationSection(itemKey);
               if (i != null) {
                  double price = i.getDouble("price");
                  String priceStr = Utils.formatNumber(price);
                  List<String> lore = new ArrayList();

                  for(String line : i.getStringList("lore")) {
                     String replaced = line.replace(costToken, priceStr).replace("%price%", priceStr);
                     lore.add(Utils.formatColors(replaced));
                  }

                  ItemStack is = makeConfiguredItem(i, i.getInt("amount"), lore);
                  inv.setItem(i.getInt("slot"), is);
               }
            }
         }

         if (shopCfg.isConfigurationSection("back-button")) {
            ConfigurationSection bb = shopCfg.getConfigurationSection("back-button");
            inv.setItem(bb.getInt("slot"), makeItem(bb.getString("material"), 1, bb.getString("displayname"), bb.getStringList("lore")));
         }

         if (shopCfg.isConfigurationSection("filler")) {
            ConfigurationSection fl = shopCfg.getConfigurationSection("filler");
            if (fl.getBoolean("enabled", false)) {
               Material fillMat = Material.valueOf(fl.getString("material", "GRAY_STAINED_GLASS_PANE"));
               String fillName = fl.getString("displayname", "&7 ");
               List<String> fillLore = fl.getStringList("lore");
               int fillerRows = Math.min(fl.getInt("rows", rows), rows);
               int startRow = rows - fillerRows;
               ItemStack fillerItem = makeItem(fillMat.name(), 1, fillName, fillLore);

               for(int r = startRow; r < rows; ++r) {
                  for(int c = 0; c < 9; ++c) {
                     int slotIndex = r * 9 + c;
                     if (inv.getItem(slotIndex) == null) {
                        inv.setItem(slotIndex, fillerItem);
                     }
                  }
               }
            }
         }

         if (shopCfg.isConfigurationSection("page-arrows")) {
            ConfigurationSection pa = shopCfg.getConfigurationSection("page-arrows");
            int maxPageCount = getMaxPageCount(shopCfg);
            if (page < maxPageCount && pa.isConfigurationSection("next-page")) {
               ConfigurationSection np = pa.getConfigurationSection("next-page");
               inv.setItem(np.getInt("slot"), makeItem(np.getString("material"), 1, np.getString("displayName"), np.getStringList("lore")));
            }

            if (page > 1 && pa.isConfigurationSection("previous-page")) {
               ConfigurationSection pp = pa.getConfigurationSection("previous-page");
               inv.setItem(pp.getInt("slot"), makeItem(pp.getString("material"), 1, pp.getString("displayName"), pp.getStringList("lore")));
            }
         }

         if (!reuseInv) {
            p.openInventory(inv);
         } else {
            p.updateInventory();
         }
      }
   }

   private static int getMaxPageCount(FileConfiguration shopCfg) {
      int count = shopCfg.isConfigurationSection("items") ? 1 : 0;

      for(int i = 2; shopCfg.isConfigurationSection("page" + i); ++i) {
         ++count;
      }

      return count;
   }

   public static void handleMenuClick(Player p, String title, int slot) {
      FileConfiguration root = plugin.getConfigManager().getRootConfig();
      String mainTitle = Utils.formatColors(root.getString("main-menu.title"));
      if (title.equals(mainTitle)) {
         ConfigurationSection catSection = root.getConfigurationSection("main-menu.categories");
         if (catSection != null) {
            for(String catKey : catSection.getKeys(false)) {
               ConfigurationSection c = catSection.getConfigurationSection(catKey);
               if (c != null && slot == c.getInt("slot")) {
                  String type = c.getString("type", "shop");
                  String action = c.getString("action", catKey);
                  if (!"command".equalsIgnoreCase(type) && !action.startsWith("command:")) {
                     openShopMenu(p, action, 1);
                  } else {
                     String cmd = action.startsWith("command:") ? action.substring("command:".length()).replace("%player%", p.getName()) : c.getString("command", "").replace("%player%", p.getName());
                     p.performCommand(cmd);
                  }

                  return;
               }
            }
         }
      }

      for(String shopKey : plugin.getConfigManager().getShopKeys()) {
         FileConfiguration shopCfg = plugin.getConfigManager().getShopConfig(shopKey);
         if (shopCfg != null) {
            String shopTitle = Utils.formatColors(shopCfg.getString("title"));
            if (title.equals(shopTitle) || title.startsWith(shopTitle + " ")) {
               int currentPage = (Integer)openShopPage.getOrDefault(p.getUniqueId(), 1);
               int maxPage = getMaxPageCount(shopCfg);
               if (shopCfg.isConfigurationSection("back-button")) {
                  ConfigurationSection bb = shopCfg.getConfigurationSection("back-button");
                  if (slot == bb.getInt("slot")) {
                     openMainMenu(p);
                     return;
                  }
               }

               if (shopCfg.isConfigurationSection("page-arrows")) {
                  ConfigurationSection pa = shopCfg.getConfigurationSection("page-arrows");
                  if (currentPage < maxPage && pa.isConfigurationSection("next-page")) {
                     ConfigurationSection np = pa.getConfigurationSection("next-page");
                     if (slot == np.getInt("slot")) {
                        openShopMenu(p, shopKey, currentPage + 1, true);
                        return;
                     }
                  }

                  if (currentPage > 1 && pa.isConfigurationSection("previous-page")) {
                     ConfigurationSection pp = pa.getConfigurationSection("previous-page");
                     if (slot == pp.getInt("slot")) {
                        openShopMenu(p, shopKey, currentPage - 1, true);
                        return;
                     }
                  }
               }

               String pageKey = currentPage == 1 ? "items" : "page" + currentPage;
               ConfigurationSection items = shopCfg.getConfigurationSection(pageKey);
               if (items != null) {
                  for(String itemKey : items.getKeys(false)) {
                     ConfigurationSection i = items.getConfigurationSection(itemKey);
                     if (i != null && slot == i.getInt("slot")) {
                        pendingShop.put(p.getUniqueId(), shopKey);
                        pendingItem.put(p.getUniqueId(), itemKey);
                        pendingAmount.put(p.getUniqueId(), i.getInt("amount"));
                        openConfirmMenu(p, shopKey, itemKey);
                        return;
                     }
                  }
               }

               return;
            }
         }
      }

      String raw = root.getString("confirm-menu.title", "");
      if (!raw.isEmpty()) {
         boolean small = root.getBoolean("confirm-menu.use-small-font-title", false);
         String placeholder = "{item-name}";
         String rawPrefix = raw.contains(placeholder) ? raw.split(Pattern.quote(placeholder))[0] : raw;
         String prefixColored = Utils.formatColors(rawPrefix);
         String confirmPrefix = small ? applySmallFont(prefixColored) : prefixColored;
         if (title.startsWith(confirmPrefix)) {
            ConfigurationSection cm = root.getConfigurationSection("confirm-menu");
            if (cm == null) {
               return;
            }

            int confirmSlot = cm.getConfigurationSection("confirm-purchase").getInt("slot");
            if (slot == confirmSlot) {
               int cooldownTicks = root.getInt("confirm-cooldown-ticks", 2);
               if (cooldownTicks > 0) {
                  long now = System.currentTimeMillis();
                  long last = (Long)lastConfirmAt.getOrDefault(p.getUniqueId(), 0L);
                  long requiredMs = (long)cooldownTicks * 50L;
                  if (now - last < requiredMs) {
                     Utils.sendMessage(p, "&7Please wait a moment before confirming again.");
                     return;
                  }

                  lastConfirmAt.put(p.getUniqueId(), now);
               }

               doPurchase(p);
               return;
            }

            int declineSlot = cm.getConfigurationSection("decline-purchase").getInt("slot");
            if (slot == declineSlot) {
               String shopKey = (String)pendingShop.get(p.getUniqueId());
               int returnPage = (Integer)openShopPage.getOrDefault(p.getUniqueId(), 1);
               openShopMenu(p, shopKey, returnPage);
               return;
            }

            for(String key : Arrays.asList("add1", "add10", "add64", "remove1", "remove10", "remove64")) {
               if (cm.isConfigurationSection(key)) {
                  ConfigurationSection btn = cm.getConfigurationSection(key);
                  if (slot == btn.getInt("slot")) {
                     boolean add = key.startsWith("add");
                     int step = btn.getInt("amount");
                     modifyPendingAmount(p, add, step);
                     openConfirmMenu(p, (String)pendingShop.get(p.getUniqueId()), (String)pendingItem.get(p.getUniqueId()));
                     return;
                  }
               }
            }
         }
      }

   }

   private static void modifyPendingAmount(Player p, boolean add, int step) {
      UUID id = p.getUniqueId();
      int amt = (Integer)pendingAmount.getOrDefault(id, 1);
      String shopKey = (String)pendingShop.get(id);
      String itemKey = (String)pendingItem.get(id);
      FileConfiguration shopCfg = plugin.getConfigManager().getShopConfig(shopKey);
      if (shopCfg != null) {
         int currentPage = (Integer)openShopPage.getOrDefault(id, 1);
         String pageKey = currentPage == 1 ? "items" : "page" + currentPage;
         ConfigurationSection itemSec = shopCfg.getConfigurationSection(pageKey + "." + itemKey);
         if (itemSec == null) {
            itemSec = shopCfg.getConfigurationSection("items." + itemKey);
            if (itemSec == null) {
               return;
            }
         }

         String matName = itemSec.getString("material");
         if (matName != null) {
            int maxStack = Material.valueOf(matName).getMaxStackSize();
            if (add) {
               pendingAmount.put(id, Math.min(maxStack, amt + step));
            } else {
               pendingAmount.put(id, Math.max(1, amt - step));
            }

         }
      }
   }

   public static void openConfirmMenu(Player p, String shopKey, String itemKey) {
      FileConfiguration root = plugin.getConfigManager().getRootConfig();
      ConfigurationSection cm = root.getConfigurationSection("confirm-menu");
      if (cm != null) {
         FileConfiguration shopCfg = plugin.getConfigManager().getShopConfig(shopKey);
         if (shopCfg != null) {
            String costToken = shopCfg.getString("money-cost-placeholder", "%price%");
            int currentPage = (Integer)openShopPage.getOrDefault(p.getUniqueId(), 1);
            String pageKey = currentPage == 1 ? "items" : "page" + currentPage;
            ConfigurationSection item = shopCfg.getConfigurationSection(pageKey + "." + itemKey);
            if (item == null) {
               item = shopCfg.getConfigurationSection("items." + itemKey);
               if (item == null) {
                  return;
               }
            }

            int amount = (Integer)pendingAmount.getOrDefault(p.getUniqueId(), 1);
            double base = item.getDouble("price");
            double total = base * (double)amount;
            String totalStr = Utils.formatNumber(total);
            String niceName = getNiceMaterialName(Material.valueOf(item.getString("material")));
            String rawTitle = cm.getString("title", "&aConfirm Purchase");
            String interp = rawTitle.replace("{item-name}", niceName);
            String colored = Utils.formatColors(interp);
            if (cm.getBoolean("use-small-font-title", false)) {
               colored = applySmallFont(colored);
            }

            int rows = cm.getInt("rows", 3);
            Inventory inv = Bukkit.createInventory((InventoryHolder)null, rows * 9, colored);
            List<String> lore = new ArrayList();

            for(String line : cm.getConfigurationSection("item-purchase").getStringList("lore")) {
               String replaced = line.replace(costToken, totalStr).replace("%price%", totalStr).replace("%clicked-amount%", String.valueOf(amount));
               lore.add(Utils.formatColors(replaced));
            }

            inv.setItem(cm.getConfigurationSection("item-purchase").getInt("slot"), makeConfiguredItem(item, amount, lore));
            ConfigurationSection cp = cm.getConfigurationSection("confirm-purchase");
            inv.setItem(cp.getInt("slot"), makeItem(cp.getString("material"), cp.getInt("amount", 1), cp.getString("displayname"), cp.getStringList("lore")));
            ConfigurationSection dp = cm.getConfigurationSection("decline-purchase");
            inv.setItem(dp.getInt("slot"), makeItem(dp.getString("material"), dp.getInt("amount", 1), dp.getString("displayname"), dp.getStringList("lore")));
            if (item.getBoolean("giveitem", true)) {
               int maxStack = Material.valueOf(item.getString("material")).getMaxStackSize();

               for(String key : List.of("add1", "add10", "add64", "remove1", "remove10", "remove64")) {
                  if (cm.isConfigurationSection(key)) {
                     ConfigurationSection btn = cm.getConfigurationSection(key);
                     int step = btn.getInt("amount");
                     boolean isAdd = key.startsWith("add");
                     boolean show;
                     if (key.equals("add64")) {
                        show = amount < maxStack;
                     } else if (key.equals("remove64")) {
                        show = amount > 1;
                     } else if (step == maxStack) {
                        show = isAdd ? amount < maxStack : amount > 1;
                     } else {
                        show = isAdd ? amount + step <= maxStack : amount - step >= 1;
                     }

                     if (show) {
                        inv.setItem(btn.getInt("slot"), makeItem(btn.getString("material"), step, btn.getString("displayname"), Collections.emptyList()));
                     }
                  }
               }
            }

            p.openInventory(inv);
         }
      }
   }

   public static String applySmallFont(String input) {
      String normal = "QWERTYUIOPASDFGHJKLZXCVBNM";
      String small = "ǫᴡᴇʀᴛʏᴜɪᴏᴘᴀѕᴅꜰɢʜᴊᴋʟᴢхᴄᴠʙɴᴍ";
      Map<Character, Character> map = new HashMap();

      for(int i = 0; i < normal.length(); ++i) {
         char U = normal.charAt(i);
         char L = Character.toLowerCase(U);
         char S = small.charAt(i);
         map.put(U, S);
         map.put(L, S);
      }

      StringBuilder out = new StringBuilder(input.length());

      for(int i = 0; i < input.length(); ++i) {
         char c = input.charAt(i);
         if (i > 0 && input.charAt(i - 1) == 167) {
            out.append(c);
         } else {
            out.append(map.getOrDefault(c, c));
         }
      }

      return out.toString();
   }

   private static void doPurchase(Player p) {
      FileConfiguration root = plugin.getConfigManager().getRootConfig();
      UUID id = p.getUniqueId();
      String shopKey = (String)pendingShop.get(id);
      String itemKey = (String)pendingItem.get(id);
      FileConfiguration shopCfg = plugin.getConfigManager().getShopConfig(shopKey);
      if (shopCfg != null) {
         int currentPage = (Integer)openShopPage.getOrDefault(id, 1);
         String pageKey = currentPage == 1 ? "items" : "page" + currentPage;
         ConfigurationSection itemSec = shopCfg.getConfigurationSection(pageKey + "." + itemKey);
         if (itemSec == null) {
            itemSec = shopCfg.getConfigurationSection("items." + itemKey);
            if (itemSec == null) {
               return;
            }
         }

         int amount = (Integer)pendingAmount.getOrDefault(id, 1);
         double basePrice = itemSec.getDouble("price");
         double total = basePrice * (double)amount;
         String totalStr = Utils.formatNumber(total);
         if (itemSec.getBoolean("giveitem", true)) {
            Material mat = Material.valueOf(itemSec.getString("material"));
            if (!hasInventorySpace(p.getInventory(), mat, amount)) {
               playDeclineSound(p);
               String msg = root.getString("messages.decline-inventory-full", "&cYour inventory is full!");
               Utils.sendMessage(p, msg);
               return;
            }
         }

         String costPlaceholder = shopCfg.getString("money-cost-placeholder", "%price%");
         String withdrawCommand = shopCfg.getString("money-withdraw-command", "").trim();
         if (!withdrawCommand.isEmpty()) {
            String rawBal = PlaceholderAPI.setPlaceholders(p, costPlaceholder);

            double balance;
            try {
               balance = Double.parseDouble(rawBal.replaceAll("[^0-9\\.\\-]", ""));
            } catch (NumberFormatException var34) {
               balance = (double)0.0F;
            }

            if (balance < total) {
               String tmpl = root.getString("messages.not-enough", "&cYou need %price% but have %balance%");
               String msg = Utils.formatColors(tmpl.replace("%price%", totalStr).replace("%balance%", Utils.formatNumber(balance)));
               Utils.sendMessage(p, msg);
               playDeclineSound(p);
               return;
            }

            String rawPrice = total == Math.floor(total) ? String.valueOf((long)total) : String.valueOf(total);
            String cmd = withdrawCommand.replace("%player%", p.getName()).replace("%price%", rawPrice);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
         } else {
            if (!plugin.getEconomy().hasEnough(p, total)) {
               double bal = plugin.getEconomy().econ.getBalance(p);
               String tmpl = root.getString("messages.not-enough", "&cYou need %price% but have %balance%");
               String msg = Utils.formatColors(tmpl.replace("%price%", totalStr).replace("%balance%", Utils.formatNumber(bal)));
               Utils.sendMessage(p, msg);
               playDeclineSound(p);
               return;
            }

            plugin.getEconomy().withdraw(p, total);
         }

         plugin.addToTotalSpent(id, total);
         if (itemSec.getBoolean("giveitem", true)) {
            ItemStack is = makeConfiguredItem(itemSec, amount, itemSec.getBoolean("givelore", true) ? replacePriceInLore(itemSec.getStringList("lore"), basePrice) : Collections.emptyList());
            p.getInventory().addItem(new ItemStack[]{is});
         } else {
            String playerCmd = itemSec.getString("command", "").replace("%player%", p.getName()).replace("%price%", totalStr).replace("%clicked-amount%", String.valueOf(amount));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCmd);
         }

         String niceName = getNiceMaterialName(Material.valueOf(itemSec.getString("material")));
         String rawMsg = root.getString("messages.purchase-success", "&aBought %clicked-amount% %clicked-item-name% for %price%");
         String sent = Utils.formatColors(rawMsg.replace("%clicked-amount%", String.valueOf(amount)).replace("%clicked-item-name%", niceName).replace("%price%", totalStr));
         Utils.sendMessage(p, sent);

         try {
            p.playSound(p.getLocation(), Sound.valueOf(root.getString("sounds.buy-sound", "UI_TOAST_CHALLENGE_COMPLETE")), 1.0F, 1.0F);
         } catch (IllegalArgumentException var33) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
         }

         Inventory top = p.getOpenInventory().getTopInventory();
         if (top != null) {
            ConfigurationSection cm = root.getConfigurationSection("confirm-menu");
            String costToken = shopCfg.getString("money-cost-placeholder", "%price%");
            List<String> newLore = new ArrayList();

            for(String line : cm.getConfigurationSection("item-purchase").getStringList("lore")) {
               String replaced = line.replace(costToken, totalStr).replace("%price%", totalStr).replace("%clicked-amount%", String.valueOf(amount));
               newLore.add(Utils.formatColors(replaced));
            }

            int previewSlot = cm.getConfigurationSection("item-purchase").getInt("slot");
            top.setItem(previewSlot, makeConfiguredItem(itemSec, amount, newLore));
            if (itemSec.getBoolean("giveitem", true)) {
               int maxStack = Material.valueOf(itemSec.getString("material")).getMaxStackSize();

               for(String key : List.of("add1", "add10", "add64", "remove1", "remove10", "remove64")) {
                  if (cm.isConfigurationSection(key)) {
                     ConfigurationSection btn = cm.getConfigurationSection(key);
                     int step = btn.getInt("amount");
                     boolean add = key.startsWith("add");
                     boolean show = add ? amount + step <= maxStack : amount - step >= 1;
                     int s = btn.getInt("slot");
                     if (show) {
                        top.setItem(s, makeItem(btn.getString("material"), step, btn.getString("displayname"), Collections.emptyList()));
                     } else {
                        top.setItem(s, (ItemStack)null);
                     }
                  }
               }
            }

         }
      }
   }

   private static boolean hasInventorySpace(Inventory inv, Material mat, int amount) {
      ItemStack[] storage;
      if (inv instanceof PlayerInventory) {
         storage = ((PlayerInventory)inv).getStorageContents();
      } else {
         storage = inv.getContents();
      }

      int maxStack = mat.getMaxStackSize();
      int free = 0;

      for(ItemStack slot : storage) {
         if (slot == null) {
            free += maxStack;
         } else if (slot.getType() == mat) {
            free += maxStack - slot.getAmount();
         }

         if (free >= amount) {
            return true;
         }
      }

      return false;
   }

   private static void playDeclineSound(Player p) {
      FileConfiguration root = plugin.getConfigManager().getRootConfig();

      Sound decline;
      try {
         decline = Sound.valueOf(root.getString("sounds.decline-sound", "ENTITY_VILLAGER_NO"));
      } catch (IllegalArgumentException var4) {
         decline = Sound.ENTITY_VILLAGER_NO;
      }

      p.playSound(p.getLocation(), decline, 1.0F, 1.0F);
   }

   private static ItemStack makeConfiguredItem(ConfigurationSection sec, int amount, List<String> lore) {
      Material mat = Material.valueOf(sec.getString("material"));
      ItemStack is = new ItemStack(mat, amount);
      ItemMeta im = is.getItemMeta();
      if (im == null) {
         return is;
      } else {
         im.setDisplayName(Utils.formatColors(sec.getString("displayname")));
         if (!lore.isEmpty()) {
            im.setLore(lore);
         }

         if (sec.isSet("enchantments")) {
            for(String enc : sec.getStringList("enchantments")) {
               String[] parts = enc.split(";");
               if (parts.length == 2) {
                  NamespacedKey key = NamespacedKey.minecraft(parts[0]);

                  try {
                     int lvl = Integer.parseInt(parts[1]);
                     if (key != null) {
                        im.addEnchant(Enchantment.getByKey(key), lvl, true);
                     }
                  } catch (NumberFormatException var12) {
                  }
               }
            }
         }

         // ── Enchant glint: hiệu ứng lấp lánh mà không hiện enchant trong lore ──
         if (sec.getBoolean("enchant_glint", false)) {
            im.addEnchant(Enchantment.UNBREAKING, 1, true);
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
         }

         if (sec.isSet("potion") && im instanceof PotionMeta) {
            PotionMeta pm = (PotionMeta)im;

            try {
               PotionType type = PotionType.valueOf(sec.getString("potion"));
               boolean upgraded = sec.getInt("potion-level", 1) >= 2;
               boolean extended = sec.getBoolean("potion-extended", false);
               pm.setBasePotionData(new PotionData(type, extended, upgraded));
               if (sec.getBoolean("potion-splash", false) && mat != Material.SPLASH_POTION) {
                  is.setType(Material.SPLASH_POTION);
               }

               if (sec.getBoolean("potion-lingering", false) && mat != Material.LINGERING_POTION) {
                  is.setType(Material.LINGERING_POTION);
               }

               im = pm;
            } catch (IllegalArgumentException var11) {
            }
         }

         if (sec.getBoolean("hide_attributes", false)) {
            im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         }

         is.setItemMeta(im);
         return is;
      }
   }

   private static List<String> replacePriceInLore(List<String> rawLore, double price) {
      List<String> out = new ArrayList();

      for(String ln : rawLore) {
         out.add(Utils.formatColors(ln.replace("%price%", Utils.formatNumber(price))));
      }

      return out;
   }

   private static ItemStack makeItem(String matName, int amt, String name, List<String> lore) {
      Material mat = Material.valueOf(matName);
      ItemStack is = new ItemStack(mat, amt);
      ItemMeta im = is.getItemMeta();
      if (im != null) {
         im.setDisplayName(Utils.formatColors(name));
         if (!lore.isEmpty()) {
            im.setLore(Utils.formatColors(lore));
         }

         is.setItemMeta(im);
      }

      return is;
   }

   private static String getNiceMaterialName(Material mat) {
      String raw = mat.name().toLowerCase().replace('_', ' ');
      StringBuilder sb = new StringBuilder();

      for(String word : raw.split(" ")) {
         if (word.length() > 0) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
         }
      }

      return sb.toString().trim();
   }

   public static void sendToPlayer(Player p, String rawColored) {
      Utils.sendMessage(p, rawColored);
   }
}
