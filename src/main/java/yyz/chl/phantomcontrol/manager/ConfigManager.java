package yyz.chl.phantomcontrol.manager;

import org.bukkit.ChatColor;
import yyz.chl.phantomcontrol.PhantomControl;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    /** 当前配置文件版本。jar 内默认 config.yml / messages.yml 的 config-version 需与此一致。 */
    private static final int CONFIG_VERSION = 1;

    private final PhantomControl plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private String currentLanguage;
    private final Map<String, FileConfiguration> languageCache = new ConcurrentHashMap<>();

    public ConfigManager(PhantomControl plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        loadConfig();
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

    /**
     * 首次加载配置（仅在插件启动时调用）。
     * 读取磁盘配置后进行版本迁移检查。
     */
    private void loadConfig() {
        reloadConfigFromDisk();
        migrateConfig();
        migrateMessagesConfig();
    }

    /**
     * 热重载配置（不执行迁移，仅从磁盘重新读取）。
     */
    public void reloadConfig() {
        reloadConfigFromDisk();
    }

    /**
     * 从磁盘重新读取配置（不复制默认文件）。
     */
    private void reloadConfigFromDisk() {
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

    // ──────────────────────────────────────────────
    // 配置迁移
    // ──────────────────────────────────────────────

    /**
     * 自动将旧版 config.yml 迁移到当前版本。
     * 使用文本拼接方式将 jar 内新增的完整 section（含注释）追加到用户文件末尾，
     * 同时用 YAML 合并处理已有 section 中的缺失子 key。
     * 只增不改，不覆盖用户已有的任何值。
     */
    private void migrateConfig() {
        // 必须用 isSet 判断用户是否真的有 config-version key，
        // 因为 Bukkit 的 getConfig() 会把 jar 默认配置设为 defaults，
        // getInt 也会查 defaults 导致永远返回 jar 里的版本号
        int userVersion = config.isSet("config-version") ? config.getInt("config-version") : 0;
        if (userVersion >= CONFIG_VERSION) {
            return;
        }

        plugin.getLogger().info("检测到旧版配置 (v" + userVersion + " → v" + CONFIG_VERSION
                + ")，正在补充缺失的配置项...");

        // 读取 jar 内的默认 config.yml
        YamlConfiguration defaults = loadJarDefaults("config.yml");
        if (defaults == null) {
            plugin.getLogger().warning("无法读取 jar 内默认配置，跳过迁移");
            return;
        }

        // 1. 文本方式：追加完整缺失 section（含注释）
        int textAdded = appendMissingSections();

        // 2. 收集缺失的子 key（父段存在但子 key 缺失），用文本方式精确插入
        List<MissingKey> missingSubKeys = new ArrayList<>();
        int yamlAdded = mergeMissingKeys(defaults, config, "", missingSubKeys);
        int subInserted = insertMissingSubKeys(missingSubKeys);

        // 3. 重新从磁盘加载合并后的配置
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 4. 兜底：YAML 方式补充任何文本插入遗漏的 key（内存生效）
        yamlAdded += mergeMissingKeys(defaults, config, "", new ArrayList<>());

        int totalAdded = textAdded + subInserted + yamlAdded;
        if (totalAdded > 0) {
            plugin.getLogger().info("配置迁移完成，新增 " + totalAdded + " 个配置项"
                    + (textAdded > 0 ? "（含注释）" : "") + "。"
                    + "请编辑 config.yml 查看新选项（原有设置均已保留）。");
        }
    }

    /**
     * 自动将旧版消息文件迁移到当前版本。
     * 遍历 jar 默认消息的所有 key，补足用户缺失的消息条目。
     */
    private void migrateMessagesConfig() {
        String[] langFiles = {"messages.yml", "messages_en.yml"};
        for (String langFile : langFiles) {
            File file = new File(plugin.getDataFolder(), langFile);
            if (!file.exists()) continue;

            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
            int userVersion = userConfig.isSet("config-version") ? userConfig.getInt("config-version") : 0;
            if (userVersion >= CONFIG_VERSION) continue;

            YamlConfiguration defaults = loadJarDefaults(langFile);
            if (defaults == null) continue;

            int added = mergeMissingKeys(defaults, userConfig, "", new ArrayList<>());
            if (added > 0) {
                userConfig.set("config-version", CONFIG_VERSION);
                try {
                    userConfig.save(file);
                    plugin.getLogger().info("消息文件 " + langFile + " 迁移完成，新增 " + added + " 条消息");
                } catch (IOException e) {
                    plugin.getLogger().warning("保存迁移后的消息文件失败: " + langFile + " - " + e.getMessage());
                }
            }
        }

        // 清除语言缓存，下次访问时重新加载
        languageCache.clear();
        this.messagesConfig = loadLanguageFile(currentLanguage);
    }

    /**
     * 从插件 jar 中读取默认 YAML 文件（不触碰磁盘文件）。
     */
    private YamlConfiguration loadJarDefaults(String resourceName) {
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("读取 jar 内默认配置失败: " + resourceName + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 jar 默认配置中用户缺失的整个顶级 section（含注释）追加到用户配置文件末尾。
     * 同时处理 config-version 的插入（插入到文件头之后而非追加到末尾）。
     *
     * @return 追加的 section 数量
     */
    private int appendMissingSections() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return 0;

        try {
            String jarText = readJarConfigText("config.yml");
            String userText = readFileText(configFile);
            if (jarText == null || userText == null) return 0;

            // 解析 jar 文本中的 section 块
            List<SectionBlock> jarBlocks = parseSectionBlocks(jarText);
            if (jarBlocks.isEmpty()) return 0;

            // 找出用户文件中缺失的 section（直接查原始文本，避开 Bukkit defaults 干扰）
            StringBuilder appendix = new StringBuilder();
            int added = 0;
            for (SectionBlock block : jarBlocks) {
                if (block.topLevelKeys.isEmpty()) continue;
                boolean allExist = block.topLevelKeys.stream().allMatch(k -> hasTopLevelKey(userText, k));
                if (!allExist) {
                    if (appendix.length() > 0) appendix.append("\n");
                    appendix.append(block.rawText);
                    added++;
                    if (isDebugEnabled()) {
                        plugin.getLogger().info("[配置迁移] 新增 section: " + block.topLevelKeys);
                    }
                }
            }

            String newText = userText;

            // 追加缺失的 section
            if (appendix.length() > 0) {
                if (!newText.endsWith("\n")) newText += "\n";
                newText += "\n" + appendix.toString();
            }

            // 插入或更新 config-version
            if (!hasTopLevelKey(newText, "config-version")) {
                String versionLine = "# 配置文件版本（请勿手动修改，升级插件时会自动更新）\nconfig-version: " + CONFIG_VERSION + "\n";
                // 在第一个 # ━━ 分隔线前插入版本号
                int firstSep = newText.indexOf("\n# ━━");
                if (firstSep >= 0) {
                    newText = newText.substring(0, firstSep) + "\n" + versionLine + newText.substring(firstSep);
                } else {
                    // 用户文件没有分隔符（旧版配置），在头部注释后插入
                    int headerEnd = findHeaderEnd(newText);
                    newText = newText.substring(0, headerEnd) + "\n" + versionLine + newText.substring(headerEnd);
                }
            } else {
                // 已有旧版本号 → 更新为当前版本
                newText = newText.replaceFirst("config-version:\\s*\\d+(\\.\\d+)?", "config-version: " + CONFIG_VERSION);
            }

            // 有变更则写入
            if (!newText.equals(userText)) {
                writeFileText(configFile, newText);
            }

            return added;
        } catch (Exception e) {
            plugin.getLogger().warning("配置文本迁移失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 找到头部注释的结束位置（第一个非注释非空行的行号偏移）。
     * 用于在旧版（无 # ━━ 分隔线）配置中定位 config-version 插入点。
     */
    private int findHeaderEnd(String text) {
        String[] lines = text.split("\n", -1);
        int pos = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                pos += line.length() + 1; // +1 for \n
            } else {
                break;
            }
        }
        return pos;
    }

    private String readJarConfigText(String resourceName) {
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String readFileText(File file) throws Exception {
        return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    private void writeFileText(File file, String text) throws Exception {
        java.nio.file.Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
    }

    /**
     * 解析 jar 默认配置文本，按 "# ━━" 分隔线拆分为 section 块。
     * 跳过头部的文件信息区（第一个分隔线之前的内容）。
     */
    private List<SectionBlock> parseSectionBlocks(String jarText) {
        List<SectionBlock> blocks = new ArrayList<>();
        String[] lines = jarText.split("\n", -1);

        // 收集所有分隔线位置
        List<Integer> separators = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("# ━━")) {
                separators.add(i);
            }
        }

        if (separators.isEmpty()) return blocks;

        // 按分隔线拆分为原始块
        List<SectionBlock> rawBlocks = new ArrayList<>();
        for (int idx = 0; idx < separators.size(); idx++) {
            int start = separators.get(idx);
            int end = (idx + 1 < separators.size()) ? separators.get(idx + 1) : lines.length;

            StringBuilder sectionText = new StringBuilder();
            for (int i = start; i < end; i++) {
                sectionText.append(lines[i]).append('\n');
            }

            SectionBlock block = new SectionBlock();
            block.rawText = sectionText.toString();
            block.topLevelKeys = extractTopLevelKeys(block.rawText);
            rawBlocks.add(block);
        }

        // 合并标题块和内容块：每个 section 有两道 # ━━ 分隔线，
        // 第一道后是标题（无 key），第二道后是内容和 key。
        // 将相邻的无 key 块与有 key 块合并，得到完整的 section。
        for (int i = 0; i < rawBlocks.size(); i++) {
            SectionBlock block = rawBlocks.get(i);
            if (block.topLevelKeys.isEmpty() && i + 1 < rawBlocks.size()) {
                SectionBlock next = rawBlocks.get(i + 1);
                SectionBlock merged = new SectionBlock();
                merged.rawText = block.rawText + next.rawText;
                merged.topLevelKeys = next.topLevelKeys;
                blocks.add(merged);
                i++; // 跳过已被合并的下一个块
            } else if (!block.topLevelKeys.isEmpty()) {
                blocks.add(block);
            }
        }

        return blocks;
    }

    /**
     * 将缺失的子 key 精确插入到用户配置文本中（父 section 已存在时）。
     */
    private int insertMissingSubKeys(List<MissingKey> missingKeys) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists() || missingKeys.isEmpty()) return 0;

        try {
            String text = readFileText(configFile);
            if (text == null) return 0;
            String original = text;
            int inserted = 0;

            for (MissingKey mk : missingKeys) {
                String newText = insertKeyIntoParent(text, mk);
                if (!newText.equals(text)) {
                    text = newText;
                    inserted++;
                }
            }

            if (!text.equals(original)) {
                writeFileText(configFile, text);
            }
            return inserted;
        } catch (Exception e) {
            plugin.getLogger().warning("配置子 key 文本插入失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 在文本中找到 path 的父 section，在父段末尾插入缺失的 key。
     * path 格式如 "settings.new-option"。
     */
    private String insertKeyIntoParent(String text, MissingKey mk) {
        int lastDot = mk.path.lastIndexOf('.');
        String parentPath = lastDot >= 0 ? mk.path.substring(0, lastDot) : "";
        String key = lastDot >= 0 ? mk.path.substring(lastDot + 1) : mk.path;
        int depth = parentPath.isEmpty() ? -1 : countChar(parentPath, '.');
        String indent = "  ".repeat(depth + 1);
        String valueStr = serializeYamlValue(mk.value);
        String commentLine = findKeyCommentInJar(mk.path);
        String insertBlock = (commentLine != null ? commentLine + "\n" : "") + indent + key + ": " + valueStr;

        // 如果已存在，跳过
        for (String line : text.split("\n")) {
            if (line.startsWith(indent + key + ":")) return text;
        }

        // 在 jar 中找该 key 紧前面的同级 key，在用户文本中插在它后面
        int insertAt = findInsertAfterKey(text, indent, key, parentPath);
        if (insertAt < 0) return text;

        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < insertAt; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append(insertBlock).append('\n');
        for (int i = insertAt; i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString().replaceFirst("\\n+$", "\n");
    }

    /**
     * 从 jar 默认配置中提取某个 key 前面的行内注释。
     */
    private String findKeyCommentInJar(String path) {
        String jarText = readJarConfigText("config.yml");
        if (jarText == null) return null;

        String key = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        String[] lines = jarText.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith(key + ":")) {
                if (i > 0 && lines[i - 1].trim().startsWith("#") && !lines[i - 1].trim().startsWith("# ━━")) {
                    return lines[i - 1];
                }
                return null;
            }
        }
        return null;
    }

    /**
     * 在 jar 文本中找 target key 同一缩进级别的前一个 key，
     * 然后在用户文本中定位该 key 的位置，返回其下一行作为插入点。
     */
    private int findInsertAfterKey(String userText, String indent, String key, String parentPath) {
        String jarText = readJarConfigText("config.yml");
        if (jarText == null) return findParentSectionEnd(userText, parentPath);

        String precedingKey = findPrecedingKeyInJar(jarText, indent, key);
        if (precedingKey == null) return findParentSectionEnd(userText, parentPath);

        String[] lines = userText.split("\n", -1);
        String pattern = indent + precedingKey + ":";
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(pattern)) return i + 1;
        }
        // 前一个 key 也不存在 → 退回段尾插入
        return findParentSectionEnd(userText, parentPath);
    }

    /**
     * 在 jar 文本中找与 targetKey 同缩进级别的前一个 key。
     */
    private String findPrecedingKeyInJar(String jarText, String indent, String targetKey) {
        String[] lines = jarText.split("\n", -1);
        String prevKey = null;
        for (String line : lines) {
            if (line.startsWith(indent) && !line.trim().startsWith("#") && line.contains(":")) {
                String key = line.substring(indent.length(), line.indexOf(':')).trim();
                if (key.equals(targetKey)) return prevKey;
                prevKey = key;
            }
        }
        return null;
    }

    /**
     * 找到父 section 在文本中的结束行号（兜底用）。
     */
    private int findParentSectionEnd(String text, String parentPath) {
        String[] lines = text.split("\n", -1);
        if (parentPath.isEmpty()) return lines.length;

        String topKey = parentPath.contains(".") ? parentPath.substring(0, parentPath.indexOf('.')) : parentPath;
        int keyLine = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(topKey + ":")) { keyLine = i; break; }
        }
        if (keyLine < 0) return lines.length;

        for (int i = keyLine + 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;
            char c = line.charAt(0);
            if (c != ' ' && c != '#') return i;
        }
        return lines.length;
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) n++;
        }
        return n;
    }

    private static String serializeYamlValue(Object val) {
        if (val instanceof Number) return val.toString();
        if (val instanceof Boolean) return val.toString();
        if (val instanceof String) return "'" + ((String) val).replace("'", "''") + "'";
        if (val == null) return "null";
        return val.toString();
    }

    /**
     * 检查文本中是否存在指定的顶级 key（行首无缩进的 "key:" 格式）。
     * 直接查原始文本，绕开 Bukkit 的 defaults 机制，避免误判。
     */
    private boolean hasTopLevelKey(String text, String key) {
        String pattern = key + ":";
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(pattern) && !trimmed.startsWith("#")) {
                // 确认是真正的 YAML key（后面是空格、值或行尾），而非另一个 key 的前缀
                String after = trimmed.substring(pattern.length());
                if (after.isEmpty() || after.startsWith(" ")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从 YAML 文本中提取顶级 key（行首无缩进的 key:）。
     */
    private List<String> extractTopLevelKeys(String text) {
        List<String> keys = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.matches("^[a-zA-Z][a-zA-Z0-9_-]*:.*") && !line.trim().startsWith("#")) {
                String key = line.substring(0, line.indexOf(':')).trim();
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * 将 defaults 中有而 target 中没有的 key 补充到 target（内存操作）。
     * 同时收集缺失 key 的路径到 missingKeys 列表，供后续文本插入。
     */
    private int mergeMissingKeys(ConfigurationSection defaults, ConfigurationSection target,
                                  String parentPath, List<MissingKey> missingKeys) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            if (target.isSet(key)) {
                if (defaults.isConfigurationSection(key) && target.isConfigurationSection(key)) {
                    added += mergeMissingKeys(
                            defaults.getConfigurationSection(key),
                            target.getConfigurationSection(key),
                            fullPath, missingKeys);
                }
            } else {
                target.set(key, defaults.get(key));
                added++;
                missingKeys.add(new MissingKey(fullPath, defaults.get(key)));
                if (isDebugEnabled()) {
                    plugin.getLogger().info("[配置迁移] 新增: " + fullPath);
                }
            }
        }
        return added;
    }

    // ──────────────────────────────────────────────
    // 内部类
    // ──────────────────────────────────────────────

    /** 表示一个 section 文本块 */
    private static class SectionBlock {
        String rawText;
        List<String> topLevelKeys;
    }

    /** 存储缺失的 key 路径及默认值，用于文本插入 */
    private static class MissingKey {
        final String path;
        final Object value;
        MissingKey(String path, Object value) { this.path = path; this.value = value; }
    }
}
