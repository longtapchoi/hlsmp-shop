package me.clanify.donutShop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
   private final JavaPlugin plugin;
   private FileConfiguration rootConfig;
   private final Map<String, FileConfiguration> shopConfigs = new HashMap();
   private final File shopsFolder;

   public ConfigManager(JavaPlugin plugin) {
      this.plugin = plugin;
      plugin.saveDefaultConfig();
      this.rootConfig = plugin.getConfig();
      this.shopsFolder = new File(plugin.getDataFolder(), "shops");
      if (!this.shopsFolder.exists()) {
         this.shopsFolder.mkdirs();
      }

      this.extractDefaultShopsFromJar();
      this.loadAllShops();
   }

   public FileConfiguration getRootConfig() {
      return this.rootConfig;
   }

   public void reload() {
      this.plugin.reloadConfig();
      this.rootConfig = this.plugin.getConfig();
      this.shopConfigs.clear();
      File[] existing = this.shopsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
      if (existing == null || existing.length == 0) {
         this.extractDefaultShopsFromJar();
      }

      this.loadAllShops();
   }

   private void loadAllShops() {
      File[] files = this.shopsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
      if (files != null) {
         for(File f : files) {
            String fileName = f.getName();
            String shopName = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            this.shopConfigs.put(shopName, cfg);
            this.plugin.getLogger().info("Loaded shop file: " + fileName);
         }

      }
   }

   public Set<String> getShopKeys() {
      return Collections.unmodifiableSet(new TreeSet(this.shopConfigs.keySet()));
   }

   public FileConfiguration getShopConfig(String shopKey) {
      return (FileConfiguration)this.shopConfigs.get(shopKey.toLowerCase(Locale.ROOT));
   }

   public void saveShopConfig(String shopKey) {
      shopKey = shopKey.toLowerCase(Locale.ROOT);
      FileConfiguration cfg = (FileConfiguration)this.shopConfigs.get(shopKey);
      if (cfg == null) {
         this.plugin.getLogger().severe("Tried to save nonexistent shop config: " + shopKey);
      } else {
         File outFile = new File(this.shopsFolder, shopKey + ".yml");

         try {
            cfg.save(outFile);
            this.plugin.getLogger().info("Saved shop file: " + shopKey + ".yml");
         } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save shop file " + shopKey + ".yml: " + e.getMessage());
         }

      }
   }

   public boolean createNewShop(String shopName) {
      shopName = shopName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
      if (!shopName.isEmpty() && !this.shopConfigs.containsKey(shopName)) {
         File newShopFile = new File(this.shopsFolder, shopName + ".yml");
         YamlConfiguration cfg = new YamlConfiguration();
         String var10002 = shopName.replace('_', ' ');
         cfg.set("title", "&6" + var10002.replace('-', ' ').toUpperCase() + " Shop");
         cfg.set("rows", 3);
         cfg.set("money-cost-placeholder", "%price%");
         cfg.createSection("back-button");
         cfg.set("back-button.slot", 18);
         cfg.set("back-button.material", "RED_STAINED_GLASS_PANE");
         cfg.set("back-button.displayname", "&#e83c1dʙᴀᴄᴋ");
         cfg.set("back-button.lore", List.of("&fClick to go back"));
         cfg.createSection("items");
         String base = "items.stick";
         cfg.set(base + ".price", (double)1.0F);
         cfg.set(base + ".slot", 10);
         cfg.set(base + ".giveitem", true);
         cfg.set(base + ".givelore", false);
         cfg.set(base + ".material", "STICK");
         cfg.set(base + ".displayname", "&eTEST &fStick");
         cfg.set(base + ".amount", 1);
         cfg.set(base + ".lore", List.of("&7This is a default item. Change me!"));

         try {
            cfg.save(newShopFile);
         } catch (IOException e) {
            this.plugin.getLogger().severe("Could not create new shop file: " + e.getMessage());
            return false;
         }

         this.shopConfigs.put(shopName, cfg);
         this.plugin.getLogger().info("Created new shop: " + shopName + ".yml (with default stick item)");
         return true;
      } else {
         return false;
      }
   }

   public boolean removeShopCategory(String shopName) {
      shopName = shopName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
      if (shopName.isEmpty()) {
         return false;
      } else {
         File target = new File(this.shopsFolder, shopName + ".yml");
         boolean existed = target.exists();
         boolean deleted = !existed || target.delete();
         if (this.shopConfigs.remove(shopName) != null) {
            this.plugin.getLogger().info("Unloaded shop category: " + shopName);
         }

         if (existed && deleted) {
            this.plugin.getLogger().info("Deleted shop file: " + target.getName());
            return true;
         } else {
            return !existed && deleted;
         }
      }
   }

   private void extractDefaultShopsFromJar() {
      File[] existing = this.shopsFolder.listFiles((dir, namex) -> namex.toLowerCase().endsWith(".yml"));
      if (existing == null || existing.length <= 0) {
         this.plugin.getLogger().info("No shops found in data folder; extracting defaults from JAR...");

         try {
            CodeSource src = this.plugin.getClass().getProtectionDomain().getCodeSource();
            if (src == null) {
               this.plugin.getLogger().warning("Unable to access plugin JAR location; skipping default shops extraction.");
               return;
            }

            URL jarUrl = src.getLocation();
            File jarFile = new File(jarUrl.toURI());
            if (!jarFile.exists() || !jarFile.getName().toLowerCase().endsWith(".jar")) {
               this.plugin.getLogger().warning("Plugin JAR not found or not a .jar; skipping default shops extraction.");
               return;
            }

            JarFile jar = new JarFile(jarFile);

            try {
               Enumeration<JarEntry> entries = jar.entries();

               while(entries.hasMoreElements()) {
                  JarEntry entry = (JarEntry)entries.nextElement();
                  String name = entry.getName();
                  if (!entry.isDirectory() && name.startsWith("shops/") && name.toLowerCase().endsWith(".yml")) {
                     String fileName = name.substring("shops/".length());
                     if (!fileName.isBlank()) {
                        File outFile = new File(this.shopsFolder, fileName);
                        if (!outFile.exists()) {
                           try {
                              InputStream in = this.plugin.getResource(name);

                              label131: {
                                 try {
                                    OutputStream out;
                                    label156: {
                                       out = new FileOutputStream(outFile);

                                       try {
                                          if (in == null) {
                                             this.plugin.getLogger().warning("Resource not found in JAR: " + name);
                                             break label156;
                                          }

                                          byte[] buffer = new byte[4096];

                                          int len;
                                          while((len = in.read(buffer)) != -1) {
                                             out.write(buffer, 0, len);
                                          }

                                          this.plugin.getLogger().info("Extracted default shop file: " + fileName);
                                       } catch (Throwable var18) {
                                          try {
                                             out.close();
                                          } catch (Throwable var17) {
                                             var18.addSuppressed(var17);
                                          }

                                          throw var18;
                                       }

                                       out.close();
                                       break label131;
                                    }

                                    out.close();
                                 } catch (Throwable var19) {
                                    if (in != null) {
                                       try {
                                          in.close();
                                       } catch (Throwable var16) {
                                          var19.addSuppressed(var16);
                                       }
                                    }

                                    throw var19;
                                 }

                                 if (in != null) {
                                    in.close();
                                 }
                                 continue;
                              }

                              if (in != null) {
                                 in.close();
                              }
                           } catch (IOException ioe) {
                              this.plugin.getLogger().severe("Failed to write default shop “" + fileName + "”: " + ioe.getMessage());
                           }
                        }
                     }
                  }
               }
            } catch (Throwable var21) {
               try {
                  jar.close();
               } catch (Throwable var15) {
                  var21.addSuppressed(var15);
               }

               throw var21;
            }

            jar.close();
         } catch (IOException | URISyntaxException e) {
            this.plugin.getLogger().severe("Error extracting default shops from JAR: " + ((Exception)e).getMessage());
         }

      }
   }
}
