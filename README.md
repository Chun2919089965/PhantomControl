# PhantomControl 插件文档

![Version](https://img.shields.io/badge/版本-v2.1.0-blue)
![License](https://img.shields.io/badge/开源许可证-AGPL3-green)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%2B-orange)
![Folia](https://img.shields.io/badge/Folia-支持-brightgreen)


**PhantomControl** 是一款适配 Paper、Folia 服务端的轻量幻翼管理插件，用于解决高版本 Minecraft 中幻翼干扰玩家游戏的问题，支持玩家自主开关幻翼生成、管理员统一管理，兼顾个性化体验与服务器管理需求。

## 📖 开发初衷

Minecraft 高版本机制中，长时间未睡觉的玩家会被幻翼攻击，容易影响建筑、生存、红石等专注玩法的游戏体验；但全局禁用幻翼又会剥夺玩家获取幻翼膜的正常玩法。

本插件用于平衡两种需求：**玩家自主控制个人幻翼生成状态，管理员可统一管控、批量管理**，适配各类生存、创造服务器场景。

## ✨ 核心功能特性

- **个人幻翼管控**：玩家可自由开启/关闭自身幻翼生成，自主调节游戏体验

- **管理员管理**：支持单独/批量修改玩家幻翼状态、查看当前在线玩家状态统计

- **双数据库适配**：支持 FlatFile 本地文件、MySQL 远程数据库两种存储方案

- **服务端兼容**：专为 Paper API 开发，适配 Paper、Folia 服务端，不支持 Spigot

- **世界黑白名单**：可自定义指定世界是否启用幻翼管控，精准适配多世界场景

- **数据保存机制**：支持定时自动保存、缓存过期清理，降低玩家数据丢失风险

- **热重载配置**：消息、GUI、世界规则和数据库设置支持重载；命令名称与别名修改后需重启服务器

- **精细化权限系统**：区分普通玩家、管理员权限，权限分配清晰可控

- **双语自动适配**：支持中文/英文双语言，自动跟随玩家客户端语言切换

- **可视化 GUI 界面**：图形化操作面板，无需记忆命令，新手友好

- **PlaceholderAPI 支持**：提供专属占位符，支持拓展计分板、公告等联动玩法

- **开发者 API**：提供异步读写、自定义事件，支持第三方插件联动

- **数据统计功能**：集成 bStats 开源统计，可自由开关数据上报

- **调试与校验机制**：可开启详细日志排查问题，自动校验配置文件完整性，异常安全禁用

## 📥 安装教程

### 适配要求

支持 Minecraft **1.20.1+** 版本，仅兼容 **Paper / Folia** 服务端，不支持 Spigot、Bukkit。

### 安装步骤

1. 下载最新版本 `PhantomControl-2.1.0.jar` 插件文件

2. 将插件放入服务器 `plugins` 文件夹

3. 重启服务器，插件自动生成默认配置文件

4. 按需修改配置后，执行重载命令即可生效

> **💡 专属适配说明**：Paper/Folia 服务端可通过自带 Libraries 自动下载 HikariCP、MySQL 驱动，无需手动安装依赖！

## ⚙️ 核心配置说明

插件主配置文件路径：`plugins/PhantomControl/config.yml`，多语言消息配置独立存放于 `messages.yml`。

### 1. 数据库配置

```yaml
database:
  type: "flatfile" # 存储类型：flatfile(本地文件) / mysql(远程数据库)
  auto-save-interval: 300 # 自动保存间隔(秒)，0=关闭自动保存
  cache-timeout-minutes: 60 # 数据缓存过期时间(分钟)
  mysql:
    address: "localhost:3306" # MySQL地址端口
    username: "username" # 数据库账号
    password: "password" # 数据库密码
    database: "phantom_control" # 数据库名称
    prefix: "phc_" # 数据表前缀
```

### 2. 世界黑白名单配置

```yaml
whitelist:
  world-whitelist-enabled: false # 是否开启世界白名单
  world-blacklist-enabled: true # 是否开启世界黑名单
  world-whitelist: [] # 白名单世界列表（仅列表内世界生效）
  world-blacklist: [] # 黑名单世界列表（列表内世界不生效）
```

### 3. 消息与统计配置

```yaml
settings:
  message:
    default-type: "CHAT" # 消息展示方式：CHAT/ACTION_BAR/TITLE
    show-title-on-change: false # 状态变更是否弹出标题提示
    show-actionbar-on-change: false # 状态变更是否弹出ActionBar提示
    language:
      mode: "auto" # 语言模式：auto自动/chinese中文/english英文
      default: "messages_en" # 默认语言文件
  bstats:
    enabled: true # 是否开启bStats数据统计
```

### 4. GUI 界面自定义配置

```yaml
settings:
  gui:
    status-enabled-material: "GREEN_WOOL" # 已启用状态展示方块
    status-disabled-material: "RED_WOOL" # 已禁用状态展示方块
    enable-button-material: "LIME_DYE" # 开启幻翼按钮材质
    disable-button-material: "RED_DYE" # 关闭幻翼按钮材质
    info-button-material: "BOOK" # 信息按钮材质
    border-material: "GRAY_STAINED_GLASS_PANE" # GUI边框材质
```

### 5. 命令别名配置

命令名称和别名在服务器启动时注册。修改这部分配置后，重载命令会提示重启服务器；其他配置仍会立即生效。

```yaml
settings:
  commands:
    main-command: "phantomcontrol" # 主命令
    main-aliases: ["pc", "phantom"] # 主命令别名
    reload-command: "phantomcontrolreload" # 配置重载命令
    reload-aliases: ["pcr", "phreload"] # 重载命令别名
```

### 6. 调试模式配置

```yaml
settings:
  debug:
    enabled: false # 开启后输出详细控制台日志，用于排查问题
```

## 📝 全部命令详解

主命令别名：`/phantomcontrol`、`/pc`、`/phantom`

### 普通玩家命令（默认全员可用）

| 命令 | 功能描述 | 所需权限 |
| --- | --- | --- |
| `/pc enable/on` | 开启个人幻翼生成 | `phantomcontrol.use`（默认全员） |
| `/pc disable/off` | 关闭个人幻翼生成 | `phantomcontrol.use`（默认全员） |
| `/pc status/check` | 查看自身幻翼开关状态 | `phantomcontrol.use`（默认全员） |
| `/pc gui/menu` | 打开可视化控制界面 | `phantomcontrol.use`（默认全员） |
| `/pc help` | 查看插件帮助文档 | `phantomcontrol.use`（默认全员） |

### 管理员命令（仅OP/授权用户）

| 命令 | 功能描述 | 所需权限 |
| --- | --- | --- |
| `/pc admin enable 玩家名` | 为指定玩家开启幻翼生成 | `phantomcontrol.admin` |
| `/pc admin disable 玩家名` | 为指定玩家关闭幻翼生成 | `phantomcontrol.admin` |
| `/pc admin status 玩家名` | 查询指定玩家幻翼状态 | `phantomcontrol.admin` |
| `/pc admin batch enable 玩家1 玩家2` | 批量开启多名玩家幻翼 | `phantomcontrol.admin` |
| `/pc admin batch disable 玩家1 玩家2` | 批量关闭多名玩家幻翼 | `phantomcontrol.admin` |
| `/pc admin server` | 查看当前在线玩家的幻翼状态统计 | `phantomcontrol.admin` |
| `/phantomcontrolreload` / `/pcr` / `/phreload` | 重载插件全部配置 | `phantomcontrol.reload` |

## 🔐 权限节点说明

| 权限节点 | 权限描述 | 默认权限 |
| --- | --- | --- |
| `phantomcontrol.use` | 使用玩家全部基础控制命令 | 全员拥有 |
| `phantomcontrol.admin` | 使用管理员管控命令 | OP |
| `phantomcontrol.reload` | 重载插件配置权限 | OP |

## 🔗 PlaceholderAPI 占位符

插件支持 PlaceholderAPI，建议使用 2.11.7+ 版本以适配 Folia 环境。

| 占位符 | 功能描述 | 返回示例 |
| --- | --- | --- |
| `%phantomcontrol_enabled%` | 返回玩家幻翼启用状态（布尔值） | `true` / `false` |
| `%phantomcontrol_status%` | 返回玩家幻翼状态文本 | `已启用` / `已禁用` |

## 💻 开发者 API 文档

v2.1.0 提供公开 API 与自定义事件，通过 Bukkit ServicesManager 注册，支持第三方插件联动，包含**同步查询、异步读写、状态监听、拦截监听**能力。

### 1. 获取 API 实例

```java
import org.bukkit.Bukkit;
import yyz.chl.phantomcontrol.api.PhantomControlAPI;

// 安全获取 API 实例
PhantomControlAPI api = Bukkit.getServicesManager().load(PhantomControlAPI.class);
if (api == null) return;
```

### 2. 在线玩家状态操作（同步）

```java
// 查询玩家幻翼开启状态
boolean isEnabled = api.arePhantomsEnabled(player);

// 开启/关闭玩家幻翼
api.enablePhantoms(player);
api.disablePhantoms(player);
api.setPhantomsEnabled(player, false);
```

### 3. 离线玩家 UUID 操作（异步，推荐）

```java
// 异步查询 UUID 对应玩家幻翼状态
api.arePhantomsEnabled(uuid).thenAccept(status -> {
    Bukkit.getLogger().info("玩家幻翼状态：" + status);
});

// 异步修改并保存离线玩家状态
api.setPhantomsEnabled(uuid, false).thenAccept(success -> {
    Bukkit.getLogger().info("数据保存成功：" + success);
});
```

### 4. 自定义事件监听

```java
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import yyz.chl.phantomcontrol.event.PhantomStatusChangeEvent;
import yyz.chl.phantomcontrol.event.PhantomStatusPreChangeEvent;
import yyz.chl.phantomcontrol.event.PhantomSpawnBlockedEvent;

public class PhantomListener implements Listener {

    // 状态变更前置事件（可取消）
    @EventHandler
    public void onPreChange(PhantomStatusPreChangeEvent event) {
        // 拦截指定玩家关闭幻翼操作
        if (!event.willBeEnabled() && event.getPlayer().hasPermission("server.need.phantom")) {
            event.setCancelled(true);
        }
    }

    // 状态变更后置事件（可获取变更记录）
    @EventHandler
    public void onPostChange(PhantomStatusChangeEvent event) {
        Bukkit.getLogger().info(
            event.getPlayer().getName() + " 幻翼状态变更："
            + event.wasEnabled() + " -> " + event.isEnabled()
            + " 操作来源：" + event.getSource()
        );
    }

    // 幻翼生成被拦截事件
    @EventHandler
    public void onPhantomBlock(PhantomSpawnBlockedEvent event) {
        Bukkit.getLogger().info("已拦截玩家 " + event.getPlayer().getName() + " 的幻翼生成");
    }
}
```

### 5. API 与事件说明

| 类 / 事件 | 核心功能 |
| --- | --- |
| `PhantomControlAPI` | API 入口，支持玩家状态读写、权限与世界校验 |
| `PhantomStatusChangeSource` | 记录操作来源：命令、GUI、管理员、API、权限强制、插件内部 |
| `PhantomStatusPreChangeEvent` | 状态变更前触发，支持取消操作 |
| `PhantomStatusChangeEvent` | 状态变更后触发，记录完整变更信息 |
| `PhantomSpawnBlockedEvent` | 幻翼生成被插件拦截时触发 |

## ❓ 常见问题FAQ

**Q1：插件支持哪些服务端和版本？**

仅支持 **Minecraft 1.20.1+ Paper/Folia**，不兼容 Spigot、Bukkit 服务端。

**Q2：玩家关闭幻翼后仍会生成幻翼怎么办？**

依次排查：① 当前世界是否在插件黑白名单中；② 服务器是否有其他幻翼管控插件冲突；③ 插件权限是否正常加载。

**Q3：插件会修改玩家睡觉统计数据吗？**

不会。插件通过 Paper 原生事件**拦截幻翼生成行为**，不会修改玩家 `TIME_SINCE_REST` 睡眠统计数值。

**Q4：如何关闭bStats数据统计？**

修改配置文件中 `settings.bstats.enabled: false`，重载配置即可关闭。

**Q5：如何开启调试日志？**

将 `settings.debug.enabled` 改为 `true`，重载配置后控制台将输出详细运行日志，便于排查问题。

## 📊 bStats 数据统计

PhantomControl 集成 **bStats 服务器统计**，用于匿名统计插件装机量、服务端版本等公开数据，帮助作者迭代优化插件。该功能不主动收集玩家隐私数据，正常情况下不会明显影响服务器性能。

![bStats](https://bstats.org/signatures/bukkit/PhantomControl.svg)

## 📌 开发者与开源信息

- **作者**：CHL_chun

- **当前版本**：v2.1.0

- **开源协议**：[GNU AGPL v3.0](https://github.com/Chun2919089965/PhantomControl/blob/main/LICENSE)

- **开源地址**：[https://github.com/Chun2919089965/PhantomControl](https://github.com/Chun2919089965/PhantomControl)

## 📞 联系方式

- 问题反馈、功能建议：可在开源仓库提交 Issue

- QQ交流群：1093090518

- 作者QQ：2919089965

**感谢使用 PhantomControl！** 欢迎Star、Fork，助力插件持续优化迭代✨
