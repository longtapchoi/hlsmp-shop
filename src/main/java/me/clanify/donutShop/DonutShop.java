package me.clanify.donutShop;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutShop extends JavaPlugin {
   private static DonutShop instance;
   private ConfigManager configManager;
   private EconomyProvider econ;
   private boolean useMySQL;
   private MySQLManager mysql;
   private File savesFile;
   private FileConfiguration savesConfig;

   public void onEnable() {
      instance = this;
      this.configManager = new ConfigManager(this);
      this.useMySQL = this.configManager.getRootConfig().getBoolean("mysql.enabled", false);
      if (this.useMySQL) {
         this.mysql = new MySQLManager(this);
         this.mysql.connect();
      }

      this.loadSavesConfig();
      this.econ = new EconomyProvider(this);

      try {
         if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new DonutShopPlaceholder(this)).register();
            this.getLogger().info("Registered DonutShopPlaceholder with PlaceholderAPI.");
         } else {
            this.getLogger().warning("PlaceholderAPI not found—%donutshop_totalspent% will be unavailable.");
         }
      } catch (NoClassDefFoundError e) {
         this.getLogger().severe("DonutShopPlaceholder class not found! PlaceholderAPI support disabled.");
         e.printStackTrace();
      }

      this.getCommand("shop").setExecutor(new ShopCommand());
      DonutShopCommand admin = new DonutShopCommand();
      this.getCommand("donutshop").setExecutor(admin);
      this.getCommand("donutshop").setTabCompleter(admin);
      this.getCommand("toggleshopmessages").setExecutor(new ToggleShopMessagesCommand());
      this.getServer().getPluginManager().registerEvents(new InventoryListener(), this);
   }

   public void onDisable() {
      if (this.useMySQL && this.mysql != null) {
         this.mysql.close();
      }

      this.saveSavesConfig();
   }

   public static DonutShop get() {
      return instance;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public EconomyProvider getEconomy() {
      return this.econ;
   }

   private void loadSavesConfig() {
      this.savesFile = new File(this.getDataFolder(), "saves.yml");
      if (!this.savesFile.exists()) {
         this.savesFile.getParentFile().mkdirs();

         try {
            this.savesFile.createNewFile();
         } catch (IOException e) {
            this.getLogger().severe("Could not create saves.yml: " + e.getMessage());
         }
      }

      this.savesConfig = YamlConfiguration.loadConfiguration(this.savesFile);
      if (!this.savesConfig.isConfigurationSection("shop-messages")) {
         this.savesConfig.createSection("shop-messages");
      }

      if (!this.savesConfig.isConfigurationSection("total-spent")) {
         this.savesConfig.createSection("total-spent");
      }

      this.saveSavesConfig();
   }

   private void saveSavesConfig() {
      if (this.savesConfig != null && this.savesFile != null) {
         try {
            this.savesConfig.save(this.savesFile);
         } catch (IOException e) {
            this.getLogger().severe("Could not save saves.yml: " + e.getMessage());
         }

      }
   }

   public boolean isShopMessagesEnabled(UUID uuid) {
      return this.savesConfig.getBoolean("shop-messages." + uuid.toString(), true);
   }

   public void setShopMessagesEnabled(UUID uuid, boolean enabled) {
      this.savesConfig.set("shop-messages." + uuid.toString(), enabled);
      this.saveSavesConfig();
   }

   public double getTotalSpent(UUID uuid) {
      return this.useMySQL && this.mysql != null ? this.mysql.fetchTotalSpent(uuid) : this.savesConfig.getDouble("total-spent." + uuid.toString(), (double)0.0F);
   }

   public void addToTotalSpent(UUID uuid, double amount) {
      if (this.useMySQL && this.mysql != null) {
         this.mysql.incrementTotalSpent(uuid, amount);
      } else {
         double current = this.savesConfig.getDouble("total-spent." + uuid.toString(), (double)0.0F);
         this.savesConfig.set("total-spent." + uuid.toString(), current + amount);
         this.saveSavesConfig();
      }

   }
}
