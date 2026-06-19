package me.clanify.donutShop;

import java.util.regex.Pattern;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryListener implements Listener {
   @EventHandler
   public void onInventoryClick(InventoryClickEvent e) {
      String title = e.getView().getTitle();
      if (MenuManager.isShopInventory(title)) {
         int topSize = e.getView().getTopInventory().getSize();
         int rawSlot = e.getRawSlot();
         if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            e.setCancelled(true);
         } else if (rawSlot < topSize) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player) {
               Player p = (Player)e.getWhoClicked();
               if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
                  FileConfiguration root = DonutShop.get().getConfigManager().getRootConfig();
                  String rawTitle = root.getString("confirm-menu.title", "");
                  boolean small = root.getBoolean("confirm-menu.use-small-font-title", false);
                  String placeholder = "{item-name}";
                  String rawPrefix = rawTitle.contains(placeholder) ? rawTitle.split(Pattern.quote(placeholder))[0] : rawTitle;
                  if (small) {
                     rawPrefix = MenuManager.applySmallFont(rawPrefix);
                  }

                  String confirmPrefix = Utils.formatColors(rawPrefix);
                  if (title.startsWith(confirmPrefix)) {
                     MenuManager.handleMenuClick(p, title, rawSlot);
                  } else {
                     String clickName = root.getString("sounds.click-sound", "UI_BUTTON_CLICK");

                     Sound clickSound;
                     try {
                        clickSound = Sound.valueOf(clickName);
                     } catch (IllegalArgumentException var15) {
                        clickSound = Sound.UI_BUTTON_CLICK;
                     }

                     p.playSound(p.getLocation(), clickSound, 1.0F, 1.0F);
                     MenuManager.handleMenuClick(p, title, rawSlot);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent e) {
      String title = e.getView().getTitle();
      if (MenuManager.isShopInventory(title)) {
         int topSize = e.getView().getTopInventory().getSize();

         for(int slot : e.getRawSlots()) {
            if (slot < topSize) {
               e.setCancelled(true);
               return;
            }
         }

      }
   }
}
