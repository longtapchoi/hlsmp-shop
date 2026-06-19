package me.clanify.donutShop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

public class MySQLManager {
   private final Plugin plugin;
   private Connection connection;

   public MySQLManager(Plugin plugin) {
      this.plugin = plugin;
   }

   public void connect() {
      try {
         String host = this.plugin.getConfig().getString("mysql.host");
         int port = this.plugin.getConfig().getInt("mysql.port");
         String database = this.plugin.getConfig().getString("mysql.database");
         String username = this.plugin.getConfig().getString("mysql.username");
         String password = this.plugin.getConfig().getString("mysql.password");
         if (host == null || database == null || username == null || password == null) {
            this.plugin.getLogger().severe("[MySQL] Missing credentials in config.yml!");
            return;
         }

         String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
         this.connection = DriverManager.getConnection(jdbcUrl, username, password);
         this.plugin.getLogger().info("[MySQL] Connected to " + host + ":" + port + "/" + database);
         String createTableSQL = "CREATE TABLE IF NOT EXISTS donutshop_spent (\n  uuid VARCHAR(36) NOT NULL,\n  total_spent DOUBLE NOT NULL DEFAULT 0,\n  PRIMARY KEY (uuid)\n);\n";
         Statement stmt = this.connection.createStatement();

         try {
            stmt.executeUpdate(createTableSQL);
            this.plugin.getLogger().info("[MySQL] Ensured table 'donutshop_spent' exists.");
         } catch (Throwable var12) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().severe("[MySQL] Connection failed: " + e.getMessage());
         e.printStackTrace();
      }

   }

   public void close() {
      if (this.connection != null) {
         try {
            this.connection.close();
            this.plugin.getLogger().info("[MySQL] Connection closed.");
         } catch (SQLException e) {
            this.plugin.getLogger().severe("[MySQL] Error while closing connection: " + e.getMessage());
         }
      }

   }

   public double fetchTotalSpent(UUID uuid) {
      if (this.connection == null) {
         return (double)0.0F;
      } else {
         String sql = "SELECT total_spent FROM donutshop_spent WHERE uuid = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(sql);

            double var5;
            label85: {
               try {
                  ps.setString(1, uuid.toString());
                  ResultSet rs = ps.executeQuery();

                  label87: {
                     try {
                        if (!rs.next()) {
                           var5 = (double)0.0F;
                           break label87;
                        }

                        var5 = rs.getDouble("total_spent");
                     } catch (Throwable var9) {
                        if (rs != null) {
                           try {
                              rs.close();
                           } catch (Throwable var8) {
                              var9.addSuppressed(var8);
                           }
                        }

                        throw var9;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                     break label85;
                  }

                  if (rs != null) {
                     rs.close();
                  }
               } catch (Throwable var10) {
                  if (ps != null) {
                     try {
                        ps.close();
                     } catch (Throwable var7) {
                        var10.addSuppressed(var7);
                     }
                  }

                  throw var10;
               }

               if (ps != null) {
                  ps.close();
               }

               return var5;
            }

            if (ps != null) {
               ps.close();
            }

            return var5;
         } catch (SQLException e) {
            this.plugin.getLogger().severe("[MySQL] fetchTotalSpent error: " + e.getMessage());
            return (double)0.0F;
         }
      }
   }

   public void incrementTotalSpent(UUID uuid, double amount) {
      if (this.connection != null) {
         String sql = "INSERT INTO donutshop_spent (uuid, total_spent) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_spent = total_spent + ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(sql);

            try {
               ps.setString(1, uuid.toString());
               ps.setDouble(2, amount);
               ps.setDouble(3, amount);
               ps.executeUpdate();
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            this.plugin.getLogger().severe("[MySQL] incrementTotalSpent error: " + e.getMessage());
         }

      }
   }
}
