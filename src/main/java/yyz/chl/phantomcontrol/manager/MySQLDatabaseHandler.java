package yyz.chl.phantomcontrol.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import yyz.chl.phantomcontrol.PhantomControl;
import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class MySQLDatabaseHandler implements DatabaseHandler {
    
    private final PhantomControl plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    
    public MySQLDatabaseHandler(PhantomControl plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    @Override
    public void connect() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/%s",
            configManager.getMySQLHost(),
            configManager.getMySQLPort(),
            configManager.getMySQLDatabase()
        ));
        hikariConfig.setUsername(configManager.getMySQLUsername());
        hikariConfig.setPassword(configManager.getMySQLPassword());
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useSSL", "false");
        hikariConfig.addDataSourceProperty("characterEncoding", "utf8");
        hikariConfig.addDataSourceProperty("serverTimezone", "UTC");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        
        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            try (Connection conn = dataSource.getConnection()) {
                plugin.getLogger().info("成功初始化MySQL连接池！");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("无法初始化MySQL连接池: " + e.getMessage());
        }
    }
    
    @Override
    public void initialize() {
        String tablePrefix = configManager.getMySQLPrefix();
        
        String createTableSQL = String.format(
            "CREATE TABLE IF NOT EXISTS %splayerdata ("
            + "player_id VARCHAR(36) PRIMARY KEY,"
            + "phantoms_enabled BOOLEAN DEFAULT TRUE,"
            + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
            + ");",
            tablePrefix
        );
        
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSQL);
            plugin.getLogger().info("成功初始化MySQL数据库表！");
        } catch (SQLException e) {
            plugin.getLogger().severe("无法创建数据库表: " + e.getMessage());
        }
    }
    
    @Override
    public boolean loadPlayerData(UUID playerId) {
        String tablePrefix = configManager.getMySQLPrefix();
        String playerIdStr = playerId.toString();
        
        String query = String.format(
            "SELECT phantoms_enabled FROM %splayerdata WHERE player_id = ?",
            tablePrefix
        );
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerIdStr);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean("phantoms_enabled");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("无法加载玩家数据: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public void savePlayerData(UUID playerId, boolean phantomsEnabled) {
        String tablePrefix = configManager.getMySQLPrefix();
        String playerIdStr = playerId.toString();
        
        String query = String.format(
            "INSERT INTO %splayerdata (player_id, phantoms_enabled) VALUES (?, ?) ON DUPLICATE KEY UPDATE phantoms_enabled = ?",
            tablePrefix
        );
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerIdStr);
            statement.setBoolean(2, phantomsEnabled);
            statement.setBoolean(3, phantomsEnabled);
            
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("无法保存玩家数据: " + e.getMessage());
        }
    }
    
    @Override
    public void saveAllData(Map<UUID, Boolean> playerDataMap) {
        String tablePrefix = configManager.getMySQLPrefix();
        
        String query = String.format(
            "INSERT INTO %splayerdata (player_id, phantoms_enabled) VALUES (?, ?) ON DUPLICATE KEY UPDATE phantoms_enabled = VALUES(phantoms_enabled)",
            tablePrefix
        );
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            
            for (Map.Entry<UUID, Boolean> entry : playerDataMap.entrySet()) {
                UUID playerId = entry.getKey();
                boolean enabled = entry.getValue();
                
                statement.setString(1, playerId.toString());
                statement.setBoolean(2, enabled);
                
                statement.addBatch();
            }
            
            statement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("无法批量保存玩家数据: " + e.getMessage());
        }
    }
    
    @Override
    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        plugin.getLogger().info("MySQL连接池已关闭");
    }
    
    @Override
    public void reloadDatabase() {
        closeConnection();
        connect();
        initialize();
    }
}