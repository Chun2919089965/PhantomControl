package yyz.chl.phantomcontrol.manager;

import org.bukkit.ChatColor;
import yyz.chl.phantomcontrol.PhantomControl;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    
    private final PhantomControl plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private String currentLanguage;
    private final Map<String, FileConfiguration> languageCache = new ConcurrentHashMap<>();
    
    public ConfigManager(PhantomControl plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        reloadConfig();
    }
    
    private void saveDefaultConfig() {
        plugin.saveDefaultConfig();
        saveDefaultMessagesConfig();
    }
    
    private void saveDefaultMessagesConfig() {
        saveLanguageFile("messages.yml");
        saveLanguageFile("messages_en.yml");
    }
    
    private void saveLanguageFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        reloadMessagesConfig();
    }
    
    private void reloadMessagesConfig() {
        languageCache.clear();
        currentLanguage = config.getString("settings.message.language.default", "messages");
        this.messagesConfig = loadLanguageFile(currentLanguage);
        this.messagesFile = new File(plugin.getDataFolder(), currentLanguage + ".yml");
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public boolean switchLanguage(String language) {
        File languageFile = new File(plugin.getDataFolder(), language + ".yml");
        
        if (languageFile.exists() || plugin.getResource(language + ".yml") != null) {
            config.set("settings.message.language.default", language);
            plugin.saveConfig();
            currentLanguage = language;
            languageCache.clear();
            this.messagesConfig = loadLanguageFile(currentLanguage);
            this.messagesFile = new File(plugin.getDataFolder(), currentLanguage + ".yml");
            return true;
        }
        
        return false;
    }
    
    public String getString(String path) {
        return config.getString(path);
    }
    
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    public long getLong(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    public String getDatabaseType() {
        return getString("database.type", "flatfile").toLowerCase();
    }
    
    public String getMySQLAddress() {
        return getString("database.mysql.address");
    }
    
    public String getMySQLHost() {
        String address = getMySQLAddress();
        if (address != null && address.contains(":")) {
            return address.split(":")[0];
        }
        return "localhost";
    }
    
    public int getMySQLPort() {
        String address = getMySQLAddress();
        if (address != null && address.contains(":")) {
            String[] parts = address.split(":");
            if (parts.length > 1) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return 3306;
                }
            }
        }
        return 3306;
    }
    
    public String getMySQLDatabase() {
        return getString("database.mysql.database");
    }
    
    public String getMySQLUsername() {
        return getString("database.mysql.username");
    }
    
    public String getMySQLPassword() {
        return getString("database.mysql.password");
    }
    
    public String getMySQLPrefix() {
        return getString("database.mysql.prefix", "");
    }
    
    public int getAutoSaveInterval() {
        return getInt("database.auto-save-interval");
    }
    
    public String getMessage(String path) {
        String message = messagesConfig.getString(path);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : null;
    }
    
    public String getMessage(String path, String defaultValue) {
        String message = messagesConfig.getString(path, defaultValue);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : defaultValue;
    }
    
    public String getMessage(org.bukkit.entity.Player player, String path) {
        return getPlayerMessage(player, path, null);
    }
    
    public String getMessage(org.bukkit.entity.Player player, String path, String defaultValue) {
        return getPlayerMessage(player, path, defaultValue);
    }
    
    private String getPlayerMessage(org.bukkit.entity.Player player, String path, String defaultValue) {
        FileConfiguration playerConfig = getPlayerLanguageConfig(player);
        if (playerConfig != null) {
            String message = playerConfig.getString(path);
            if (message != null) {
                return ChatColor.translateAlternateColorCodes('&', message);
            }
        }
        return defaultValue != null ? getMessage(path, defaultValue) : getMessage(path);
    }
    
    public String formatMessage(String path, String... placeholderPairs) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', replacePlaceholders(message, placeholderPairs));
    }
    
    public String formatMessage(org.bukkit.entity.Player player, String path, String... placeholderPairs) {
        FileConfiguration playerConfig = getPlayerLanguageConfig(player);
        if (playerConfig != null) {
            String message = playerConfig.getString(path);
            if (message != null) {
                return ChatColor.translateAlternateColorCodes('&', replacePlaceholders(message, placeholderPairs));
            }
        }
        return formatMessage(path, placeholderPairs);
    }
    
    private String replacePlaceholders(String message, String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            message = message.replace(pairs[i], pairs[i + 1] != null ? pairs[i + 1] : "");
        }
        return message;
    }
    
    private FileConfiguration getPlayerLanguageConfig(org.bukkit.entity.Player player) {
        String languageMode = getString("settings.message.language.mode", "auto");
        String languageFile;
        
        switch (languageMode) {
            case "chinese":
                languageFile = "messages";
                break;
            case "english":
                languageFile = "messages_en";
                break;
            case "auto":
            default:
                if (player != null) {
                    String locale = player.getLocale();
                    if (locale != null && (locale.toLowerCase().startsWith("zh_cn") || locale.toLowerCase().startsWith("zh"))) {
                        languageFile = "messages";
                    } else {
                        languageFile = currentLanguage;
                    }
                } else {
                    languageFile = currentLanguage;
                }
                break;
        }
        
        return getCachedLanguageFile(languageFile);
    }
    
    private FileConfiguration getCachedLanguageFile(String languageFile) {
        if (languageCache.containsKey(languageFile)) {
            return languageCache.get(languageFile);
        }
        
        FileConfiguration config = loadLanguageFile(languageFile);
        languageCache.put(languageFile, config);
        return config;
    }
    
    private FileConfiguration loadLanguageFile(String languageFile) {
        File file = new File(plugin.getDataFolder(), languageFile + ".yml");
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        try (InputStream inputStream = plugin.getResource(languageFile + ".yml")) {
            if (inputStream != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载语言文件: " + languageFile + ".yml");
        }
        return new YamlConfiguration();
    }
    
    public boolean isDebugEnabled() {
        return getBoolean("settings.debug.enabled");
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public File getMessagesFile() {
        return messagesFile;
    }
    
    public boolean validateConfig() {
        boolean isValid = true;
        
        String databaseType = getDatabaseType();
        if (!databaseType.equals("flatfile") && !databaseType.equals("mysql")) {
            plugin.getLogger().severe("配置错误: 数据库类型必须是 'flatfile' 或 'mysql'");
            isValid = false;
        }
        
        if (databaseType.equals("mysql")) {
            String host = getMySQLHost();
            String database = getMySQLDatabase();
            String username = getMySQLUsername();
            String password = getMySQLPassword();
            
            if (host == null || host.isEmpty()) {
                plugin.getLogger().severe("配置错误: MySQL 主机地址不能为空");
                isValid = false;
            }
            
            if (database == null || database.isEmpty()) {
                plugin.getLogger().severe("配置错误: MySQL 数据库名称不能为空");
                isValid = false;
            }
            
            if (username == null || username.isEmpty()) {
                plugin.getLogger().severe("配置错误: MySQL 用户名不能为空");
                isValid = false;
            }
            
            if (password == null || password.isEmpty()) {
                plugin.getLogger().severe("配置错误: MySQL 密码不能为空");
                isValid = false;
            }
        }
        
        int autoSaveInterval = getAutoSaveInterval();
        if (autoSaveInterval < 0) {
            plugin.getLogger().severe("配置错误: 自动保存间隔不能为负数");
            isValid = false;
        }
        
        return isValid;
    }
}
