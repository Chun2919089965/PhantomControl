# PhantomControl

![Version](https://img.shields.io/badge/版本-v2-blue)
![License](https://img.shields.io/badge/开源许可证-GPL3-green)
![Minecraft](https://img.shields.io/badge/Minecraft-1.18%2B-orange)
![Folia](https://img.shields.io/badge/Folia-支持-brightgreen)

PhantomControl 是一款功能强大的 Minecraft 服务器插件，用于管理玩家的幻翼生成，支持 Folia 和 Spigot/Paper 服务器。

## 开发由来

在高版本中，幻翼会攻击长时间未睡觉的玩家，但频繁的幻翼攻击往往会打断玩家的正常游戏体验，尤其是对于建筑等长时间专注任务的玩家。然而，如果服务器直接禁用所有幻翼，又会剥夺需要幻翼的玩家的游戏体验。

为了平衡这两种需求，让玩家能够自主选择是否开启幻翼，我开发了这款插件。它允许玩家根据自己的喜好控制幻翼生成，同时支持管理员进行全局管理。

> 💡 如果有什么建议或者问题，欢迎提出！

## 功能特性

- ✅ **幻翼生成控制** — 允许玩家启用或禁用自己的幻翼生成
- ✅ **管理员命令** — 支持管理员管理其他玩家的幻翼设置，含批量操作
- ✅ **数据库支持** — 支持 FlatFile 和 MySQL 两种数据库存储方式
- ✅ **Folia 兼容** — 完全支持 Folia 服务器，自动检测环境并优化调度
- ✅ **世界白名单/黑名单** — 可配置指定世界是否启用幻翼控制
- ✅ **自动保存机制** — 定期自动保存玩家数据，确保数据安全
- ✅ **配置热重载** — 支持动态重载配置，无需重启服务器
- ✅ **完整的权限系统** — 精细的权限控制
- ✅ **多语言支持** — 支持中文和英文，可自动根据玩家客户端语言调整
- ✅ **GUI 界面** — 提供图形化控制界面，方便玩家操作
- ✅ **PlaceholderAPI 支持** — 提供占位符供其他插件调用
- ✅ **开发者 API** — 提供自定义 Event，方便其他插件监听状态变更
- ✅ **bStats 集成** — 提供插件使用情况统计，可在配置文件中控制开启或关闭
- ✅ **调试模式** — 可配置的详细日志输出，便于排查问题
- ✅ **配置验证** — 自动验证配置文件完整性，配置错误时安全禁用插件

## 安装方法

1. 下载最新版本的 `PhantomControl.jar` 文件
2. 将 jar 文件放入服务器的 `plugins` 文件夹中
3. 重启服务器，插件会自动生成配置文件
4. 根据需要修改配置文件，然后使用 `/phantomreload` 命令重载配置

> 💡 **Paper 用户**：插件会自动通过 Paper Libraries 机制下载 HikariCP 和 MySQL 驱动，无需手动安装。

## 配置说明

插件生成的配置文件位于 `plugins/PhantomControl/config.yml`。

### 数据库设置

```yaml
database:
  type: "flatfile" # 数据库类型，可选值：flatfile, mysql
  auto-save-interval: 300 # 自动保存间隔（秒），0 表示不启用
  cache-timeout-minutes: 60 # 缓存过期时间（分钟），超时后自动清理并保存
  mysql:
    address: "localhost:3306" # MySQL 地址
    username: "username" # MySQL 用户名
    password: "password" # MySQL 密码
    database: "phantom_control" # 数据库名称
    prefix: "phc_" # 表前缀
```

### 世界白名单/黑名单

```yaml
whitelist:
  world-whitelist-enabled: false # 是否启用世界白名单
  world-blacklist-enabled: true # 是否启用世界黑名单
  world-whitelist: [] # 白名单世界列表
  world-blacklist: [] # 黑名单世界列表
```

### 消息与语言设置

```yaml
settings:
  message:
    default-type: "CHAT" # 默认消息显示方式：CHAT, ACTION_BAR, TITLE
    show-title-on-change: false # 状态变更时是否显示标题消息
    show-actionbar-on-change: false # 状态变更时是否显示 ActionBar 消息
    language:
      mode: "auto" # 语言模式：auto（自动）, chinese, english
      default: "messages_en" # 默认语言文件名称（不含 .yml 扩展名）
  bstats:
    enabled: true # 是否启用 bStats 统计功能
```

### GUI 设置

```yaml
settings:
  gui:
    title: "幻翼控制" # GUI 标题
    status-enabled-material: "GREEN_WOOL" # 已启用状态方块材质
    status-disabled-material: "RED_WOOL" # 已禁用状态方块材质
    enable-button-material: "LIME_DYE" # 启用按钮材质
    disable-button-material: "RED_DYE" # 禁用按钮材质
    info-button-material: "BOOK" # 信息按钮材质
    border-material: "GRAY_STAINED_GLASS_PANE" # 边框材质
```

### 命令设置

```yaml
settings:
  commands:
    main-command: "phantomcontrol" # 主命令名称
    main-aliases: ["pc", "phantom"] # 主命令别名
    reload-command: "phantomcontrolreload" # 重载命令名称
    reload-aliases: ["pcr", "phreload"] # 重载命令别名
```

### 调试模式

```yaml
settings:
  debug:
    enabled: false # 是否启用调试模式（启用后会显示详细日志）
```

### 消息配置

消息配置已移至 `messages.yml` 文件，支持多语言设置。

## 命令说明

主命令别名：`/phantomcontrol`、`/pc`、`/phantom`

### 玩家命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/phantomcontrol enable` (或 `on`) | 启用幻翼生成 | 无 |
| `/phantomcontrol disable` (或 `off`) | 禁用幻翼生成 | 无 |
| `/phantomcontrol status` (或 `check`) | 查看当前幻翼状态 | 无 |
| `/phantomcontrol gui` (或 `menu`) | 打开图形化控制界面 | 无 |
| `/phantomcontrol help` | 查看帮助信息 | 无 |

### 管理员命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/phantomcontrol admin enable <玩家名>` | 为指定玩家启用幻翼 | `phantomcontrol.admin` |
| `/phantomcontrol admin disable <玩家名>` | 为指定玩家禁用幻翼 | `phantomcontrol.admin` |
| `/phantomcontrol admin status <玩家名>` | 查看指定玩家的幻翼状态 | `phantomcontrol.admin` |
| `/phantomcontrol admin batch enable <玩家1> <玩家2> ...` | 批量启用幻翼 | `phantomcontrol.admin` |
| `/phantomcontrol admin batch disable <玩家1> <玩家2> ...` | 批量禁用幻翼 | `phantomcontrol.admin` |
| `/phantomcontrol admin server` | 查看服务器幻翼设置统计 | `phantomcontrol.admin` |
| `/phantomreload` (或 `pcr`、`phreload`) | 重载插件配置 | `phantomcontrol.reload` |

## 权限说明

| 权限 | 描述 | 默认 |
|------|------|------|
| `phantomcontrol.use` | 允许使用基本的幻翼控制命令 | 所有玩家 |
| `phantomcontrol.admin` | 允许使用管理员命令管理其他玩家 | OP |
| `phantomcontrol.reload` | 允许重载插件配置 | OP |

## PlaceholderAPI 支持

PhantomControl 支持 PlaceholderAPI。温馨提示：PlaceholderAPI 的 2.11.7 版本开始支持 Folia。

| 占位符 | 描述 | 示例输出 |
|--------|------|----------|
| `%phantomcontrol_enabled%` | 返回玩家幻翼是否启用的布尔值 | `true` / `false` |
| `%phantomcontrol_status%` | 返回玩家幻翼状态的文本 | `已启用` / `已禁用` |

## 开发者 API

PhantomControl v2.0.0 提供自定义 Event，方便其他插件监听幻翼状态变更。

```java
import yyz.chl.phantomcontrol.event.PhantomStatusChangeEvent;

@EventHandler
public void onPhantomChange(PhantomStatusChangeEvent event) {
    Player player = event.getPlayer();
    boolean nowEnabled = event.isEnabled();

    getLogger().info(player.getName() + " 的幻翼" + (nowEnabled ? "已启用" : "已禁用"));
    // 可以在这里做任何联动：记录日志、同步数据等
}
```

| API | 描述 |
|-----|------|
| `PhantomStatusChangeEvent` | 玩家幻翼状态变更时触发，包含玩家对象和当前状态 |

## 常见问题

**Q: 插件支持哪些服务器版本？**
A: 插件支持 Minecraft 1.18+ 版本，包括 Spigot、Paper 和 Folia 服务器。

**Q: 为什么玩家禁用幻翼后仍然有幻翼生成？**
A: 请检查：
1. 玩家所在世界是否在白名单/黑名单中
2. 插件是否有足够的权限
3. 服务器是否有其他插件影响幻翼生成

**Q: 如何查看玩家的 TIME_SINCE_REST 统计值？**
A: 在游戏中的 ESC 菜单 → 统计信息中，可以查看"上次入眠时间"。

**Q: 插件支持哪些语言？**
A: 插件支持中文和英文，可自动根据玩家客户端语言调整，也可在配置文件中强制使用特定语言。

**Q: 如何控制 bStats 统计功能？**
A: 可以在配置文件中的 `settings.bstats.enabled` 选项中控制开启或关闭。

**Q: 如何启用调试模式？**
A: 将 `config.yml` 中的 `settings.debug.enabled` 设置为 `true`，然后重启服务器或重载插件即可在控制台查看详细日志。

## 开发者信息

- **作者**：CHL_chun
- **许可证**：[AGPL3](LICENSE.md)
- **开源地址**：[https://github.com/Chun2919089965/PhantomControl](https://github.com/Chun2919089965/PhantomControl)

欢迎提交 Issue 和 Pull Request，帮助改进插件！

### bStats 统计

![bStats](https://bstats.org/signatures/bukkit/PhantomControl.svg)

*统计数据每 30 分钟更新一次，数据来源：[bStats.org](https://bstats.org/)*

## 联系方式

- 如有问题或建议，请在我发的平台评论中提出
- QQ 交流群：1093090518
- 或通过 QQ 联系：2919089965

---

感谢使用 PhantomControl 插件！如有任何问题，请随时联系我。
