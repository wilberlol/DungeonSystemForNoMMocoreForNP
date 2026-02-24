# 🏰 DungeonSystem 插件完整操作指南

DungeonSystem 是一個功能強大的副本系統，專為 Minecraft 伺服器設計。它支持波次挑戰、組隊進入、鑰匙系統、動態難度縮放以及復活機制。

---

## 📌 目錄
1. [前置需求](#-前置需求)
2. [指令說明](#-指令說明)
3. [權限列表](#-權限列表)
4. [核心設定檔 (config.yml)](#-核心設定檔-configyml)
5. [副本鑰匙設定 (keys.yml)](#-副本鑰匙設定-keysyml)
6. [副本配置教學 (Dungeon 檔案)](#-副本配置教學-dungeon-檔案)
7. [特色功能詳解](#-特色功能詳解)

---

## 🛠 前置需求
本插件需要以下插件作為基礎才能運行：
*   **MythicMobs**: 用於管理副本內的怪物。
*   **DecentHolograms**: 用於顯示副本排行榜全息圖。
*   **PlaceholderAPI**: 用於變數支持。
*   **(選填) MMOCore**: 若要與 MMOCore 隊伍系統對接。

---

## ⌨️ 指令說明

### 1. 副本管理指令 (`/dungeon` 或 `/dg`)
| 指令 | 描述 | 權限要求 |
| :--- | :--- | :--- |
| `/dungeon join <副本ID>` | 使用鑰匙加入指定副本 | `dungeon.join` |
| `/dungeon leave` | 離開當前副本 | 玩家 |
| `/dungeon list` | 列出所有可用的副本及其資訊 | 玩家 |
| `/dungeon rank <副本ID>` | 在玩家位置上方臨時顯示排行榜 | 玩家 |
| `/dungeon key <副本ID> [數量]` | 給予自己副本入場卷 | `dungeonsystem.admin` |
| `/dungeon revive <normal/advanced>`| 給予自己復活裝置 | `dungeonsystem.admin` |
| `/dungeon mob <參數...>` | 在當前位置添加怪物到副本配置 | `dungeonsystem.admin` |
| `/dungeon permrank <子指令>` | 管理永久性排行榜全息圖 | `dungeonsystem.admin` |
| `/dungeon reload` | 重新讀取所有設定檔 | `dungeonsystem.admin` |

**`permrank` 子指令詳解：**
*   `create <副本ID>`: 在目前位置創建該副本的永久排行榜。
*   `remove <副本ID>`: 刪除該副本的永久排行榜。
*   `update <副本ID>`: 手動更新排行榜數據。
*   `list`: 列出所有已設置的永久排行榜及其座標。

---

### 2. 隊伍系統指令 (`/party`)
*本指令僅在 `config.yml` 的 `party.system-type` 設置為 `custom` 時生效。*

| 指令 | 描述 |
| :--- | :--- |
| `/party create` | 創建一個新隊伍 |
| `/party invite <玩家>` | 邀請玩家加入隊伍 |
| `/party accept` | 接受隊伍邀請 |
| `/party decline` | 拒絕隊伍邀請 |
| `/party leave` | 離開當前隊伍 |
| `/party kick <玩家>` | 將隊員踢出隊伍 (僅隊長) |
| `/party info` | 查看隊伍成員資訊 |
| `/party chat <訊息>` | 隊伍頻道聊天 |

---

## 🔑 權限列表
| 權限節點 | 預設對象 | 描述 |
| :--- | :--- | :--- |
| `dungeonsystem.admin` | OP | 允許使用所有管理、重載、給予物品及配置指令。 |
| `dungeon.join` | 玩家 | 允許玩家使用指令加入副本（需搭配鑰匙）。 |

---

## ⚙️ 核心設定檔 (config.yml)

```yaml
key-settings:
  cooldown-seconds: 2          # 鑰匙使用冷卻時間

settings:
  exit-point: "world,0,64,0,0,0" # 離開副本後的傳送點
  revival-system-enabled: true   # 是否啟用復活系統
  debug: false

ranking:
  hologram-update-interval: 5    # 排行榜自動更新間隔（秒）

party:
  system-type: "custom"         # "custom" (自帶) 或 "mmocore" (外部)

wave-dungeon:                   # 波次副本音效設定
  sounds:
    wave-start: "ENTITY_ENDER_DRAGON_GROWL"
    wave-clear: "ENTITY_PLAYER_LEVELUP"

mob-scaling:                    # 動態難度縮放
  normal-multipliers:           # 根據玩家人數增加小怪數量
    "1": 1.0
    "2": 2.0
  boss-level-bonus:             # 根據玩家人數增加 BOSS 等級
    "1": 0
    "2": 5
```

---

## 🎫 副本鑰匙設定 (keys.yml)
您可以為每個副本自定義不同的入場卷：

```yaml
keys:
  dungeon_id:                    # 必須對應副本配置的文件名
    name: "§e副本入場卷"
    lore:
      - "§7右鍵點擊進入副本"
    material: "PAPER"           # 物品材質
    custom_model_data: 1        # 資源包模型編號
    is_wave: false              # 是否為波次副本鑰匙
    use_sound:                  # 使用時的音效
      sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
      volume: 1.0
      pitch: 1.0
```

---

## 📂 副本配置教學 (Dungeon 檔案)
副本配置位於 `plugins/DungeonSystem/Dungeon/` 資料夾中。

### 1. 普通副本 (normal)
目標通常是擊殺指定的 `target-mob`。
```yaml
display-name: "§6新手試煉"
type: "normal"
level-required: 10              # 等級要求
max-players: 4                  # 最大人數
1:                              # 區域/實體編號
  spawn-point: "world,x,y,z,y,p"      # 入場傳送點
  death-waiting-area: "world,x,y,z"   # 死亡後的觀戰區
  target-mob: "boss_mob_id"           # 擊殺此怪物即視為通關
  mobs:
    - id: "my_mythic_mob"             # MythicMobs 的 ID
      location: "world,x,y,z,y,p"
```

### 2. 波次副本 (wave)
玩家需要清除每一波怪物才能進入下一波。
```yaml
display-name: "§b無盡挑戰"
type: "wave"
waves:
  total: 3                      # 總波次數
  wave-1:
    - id: "skeleton_minion"
      location: "world,x,y,z,y,p"
  wave-3:
    - id: "final_boss"
      location: "world,x,y,z,y,p"
```

---

## 🌟 特色功能詳解

### 💀 復活系統
當玩家在副本內死亡時，會被傳送到 `death-waiting-area`。
*   **普通復活裝置**：需右鍵點擊隊友，等待 10 秒後隊友在原地復活。
*   **高級復活裝置**：右鍵點擊後立即原地復活。
*   管理員可透過 `/dungeon revive <type> <amount>` 發放。

### 📈 動態難度縮放 (Mob Scaling)
*   **小怪數量**：系統會將配置中的怪物數量乘以 `normal-multipliers`。例如設置倍率為 2.0 且有 2 名玩家時，原本出現 1 隻小怪的位置會生成 2 隻。
*   **BOSS 等級**：系統會根據 `boss-level-bonus` 提升 MythicMobs 的等級。

### 🏆 排行榜 (Ranking)
插件會記錄玩家通關的最短時間。
*   使用 `/dungeon permrank create` 放置全息圖。
*   支持顯示前 10 名的玩家名稱與通關時間。

---
---
## 備註：
文字顯示都支援MiniMessage，如何使用請參考 https://docs.papermc.io/misc/tools/minimessage-web-editor/
---


*文件更新日期：2026年2月24日*
