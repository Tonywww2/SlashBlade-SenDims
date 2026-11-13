# 暮刃守卫（Twilight Blade Warden）

主线第一个首领，之后放在暮色森林终焉城堡中心由玩家召唤（召唤方式与掉落在整合包中配置）。实体 ID `slashblade_sendims:twilight_blade_warden`，刷怪蛋 `slashblade_sendims:twilight_blade_warden_spawn_egg`。

## 外观

当前为方案 A 的占位渲染（`client/renderer/entity/TwilightBladeWardenRenderer.java`）：

- 核心：悬浮、缓慢旋转的暮色森林紫色城堡符文砖（`twilightforest:violet_castle_rune_brick`，缺失时用哭泣的黑曜石）。
- 武器：环绕核心的「符文终焉剑」，第一阶段 5 把，每进一个阶段多 1 把；前摇时剑阵收紧并加速旋转。剑为独立的实体模型 `models/entity/rune_finale_sword.json`（`item/generated`），贴图 `textures/item/rune_finale_sword.png`（32×32 占位），不是物品。
- 渲染器不是 `LivingEntityRenderer`，头顶的「危」「晕」由渲染器末尾调用 `LeaderStatusCharacterRenderer.renderStatusCharacter` 绘制；换正式模型时要保留这一调用。
- 正式模型完成后只需替换渲染器与贴图。曾讨论过的备选：刃角暮鹿（鹿角由刀组成的灵体巨鹿）、萤林刃蛾（刀形翅膀的巨蛾）。

## 基础数据

| 属性 | 数值 |
| --- | --- |
| 最大生命 | 200（首个服务端 tick 加 Leader 血量修饰 `sbsd.leader.health`，×6 后为 1200） |
| 攻击力 | 10 |
| 护甲 / 韧性 | 10 / 4 |
| 移速 / 追踪距离 | 0.28 / 48 |
| 击退抗性 | 1.0；另在 `#forge:bosses` 中，免疫拔刀剑击退 |
| 其他 | 免疫火焰与摔落伤害，不可推动，不会因距离消失，碰撞箱 1.6 × 2.8，经验 200 |

整合包会重新配置这些数值。所有招式伤害都是"当前攻击力 × 倍率"：中 = 1.0、高 = 1.2、中偏低 = 0.75，走普通怪物攻击伤害，不穿透无敌帧（闪避有效）。

## Leader 接入

- 在 common setup 中注册为 `EXTERNAL` Leader（`events/BossSetupEvents.java`），首个服务端 tick 调用 `SBSDLeader.initializeLeader` 获得与 Leader 相同的血量修饰与 `sbsd_leaders` 队伍。
- 蓝刀：前摇期间每 tick 显示 Leader 的蓝火（灵魂火焰）预警环，不可招架。
- 红刀：不显示蓝火预警环（那是不可招架的信号），与 MANAGED Leader 特殊攻击前一致；出招前 `PARRY_TICK`（20 tick）打开招架窗口（持续 22 tick，到出招后 1 tick），窗口内由 Leader 核心显示「危」、白→暗红粒子环并响钟（`client/boss/WardenClientSetup.java` 注册进度提供器）。
- 被招架：破防（受到伤害 ×5）与眩晕都为 80 tick（4 秒，`WardenEvents` 改写 `LeaderParryAttemptEvent`），当前招式与后续连段全部取消，头顶显示「晕」。玩家照常获得反击斩、治疗与行动力（需装备赛马娘魂）。
- 详细的标准特效与调用位置见 `leader_api.md` 的「标准前摇特效与调用位置」。

## 阶段与锁血

| 阶段 | 生命范围 | 红刀概率 | 出招间隔 | 血条颜色 | 剑数 |
| --- | --- | --- | --- | --- | --- |
| 一 | 100% ~ 70% | 15% | 22 tick | 蓝 | 5 |
| 二 | 70% ~ 40% | 25% | 16 tick | 紫 | 6 |
| 三 | 40% ~ 0 | 35% | 12 tick | 红 | 7 |

- 生命将要低于 70% 或 40% 时停在锁血线上（单次伤害再高也一样），进入下一阶段，打断当前招式，2 秒无敌（期间不出招），播放转阶段特效。
- `/kill`、虚空等无视无敌的伤害不受锁血和无敌限制。
- 阶段与剩余无敌时间写入存档，读档时先恢复阶段再读生命值。

## 出招选择

按到目标的水平距离筛选可用招式；先以当前阶段的红刀概率决定红/蓝，没有符合距离的红刀时出蓝刀（反之亦然），在符合的招式里等概率挑选，同一招不连续出两次。目标超出所有招式距离时走近目标。

前摇：短 10、中 20、蓝刀长 30、红刀中 26、红刀长 36、三重符阵末段 40、七时雨子前摇 6（tick）。范围：小 3.5、中 5.5、大 9、极大 14（格，按目标碰撞箱水平距离计算）。

### 蓝刀（不可招架）

| 招式 | ID | 距离 | 流程 |
| --- | --- | --- | --- |
| 圆月 | `engetsu` | ≤ 5 | 短前摇 → 自身周围小范围，中伤害 |
| 扇返 | `ougigaeshi` | ≤ 5 | 中前摇 → 前方 120°、半径 6 扇形，中伤害 → 短前摇 → 自身周围中范围，中伤害 |
| 缩地 | `shukuchi` | 3 ~ 11 | 中前摇，过半时记下目标位置 → 以 1.4 格/tick 冲到该位置前 2 格，冲刺中碰到的敌人各吃一次中伤害 → 随机接圆月或扇返 |
| 七时雨 | `nanatsu_shigure` | ≤ 11 | 中前摇 → 连斩 7 次（前方 90°、半径 4 扇形，中偏低伤害）；每斩后以 0.45 格/tick 追向目标（最多 12 tick，贴近 2.5 格即停），再 6 tick 短前摇锁定方向出斩 |
| 流星坠 | `ryusei_otoshi` | 11 ~ 32 | 中前摇 → 14 tick 跃至双方中点上空约 7 格 → 悬空长前摇 30 tick → 以 1.8 格/tick 朝目标俯冲，撞到目标或落地时中范围中伤害 |

### 红刀（可招架）

| 招式 | ID | 距离 | 流程 |
| --- | --- | --- | --- |
| 终焉一文字 | `shuen_ichimonji` | ≤ 9 | 长前摇 → 前方 150°、半径 9 扇形加自身周围小范围，高伤害 → 8 tick 后同范围中伤害（只有第一击可招架） |
| 升龙闪 | `shoryusen` | 3 ~ 11 | 长前摇 → 以 1.6 格/tick 追着目标冲刺（最多 18 tick）→ 撞到目标：中伤害并把它击飞到空中 → 10 tick 后在目标处补一刀高伤害；撞击被闪避时不触发后续 |
| 三重符阵 | `sanju_fujin` | ≤ 13 | 红刀中前摇 → 半径 5，中伤害 → 红刀中前摇 → 半径 9，中伤害 → 前摇 40 → 半径 14，高伤害；每段各有招架窗口，任一段被招架即中断 |

出招画面使用拔刀剑的斩击刀光（`entities/EntityVisualSlashEffect.java`，只有画面不判定伤害），蓝刀蓝色 `0x3FA9FF`、红刀红色 `0xFF3B3B`，范围攻击另在地面画出半径。

## 招架教程

- 每次开始一个招式（连段的后续段除外）时，向 32 格内玩家的动作栏显示一句不含招式名的简短提示：蓝刀为蓝色「不可招架：闪避或拉开距离」，红刀为红色「可招架：头顶出现「危」时用迅冲斩或上斩命中它」。
- 玩家用石英右键守卫后永久关闭（玩家持久数据 `PlayerPersisted` 中的 `slashblade_sendims.parry_tutorial_disabled`，死亡不丢），用钻石右键重新开启；两种物品都不消耗，聊天栏回一条灰色确认。
- 文本在语言文件的 `message.slashblade_sendims.parry_tutorial.*`。

## 代码位置

| 文件 | 内容 |
| --- | --- |
| `entities/boss/TwilightBladeWarden.java` | 实体、属性、状态机、阶段与锁血、前摇同步、血条、存档 |
| `entities/boss/WardenAttacks.java` | 招式定义（距离、前摇、范围、倍率常量）与 8 个招式脚本 |
| `entities/boss/WardenCombat.java` | 伤害判定（圆形、扇形、接触）与出招画面音效 |
| `entities/boss/ParryTutorial.java` | 动作栏教程与石英/钻石开关 |
| `entities/boss/WardenEvents.java` | 招架后 4 秒窗口 |
| `events/BossSetupEvents.java` | 属性注册、Leader 类型注册 |
| `client/boss/WardenClientSetup.java` | 剑模型注册、「危」进度提供器 |
| `client/renderer/entity/TwilightBladeWardenRenderer.java` | 占位渲染 |
| `data/forge/tags/entity_types/bosses.json` | 加入 `#forge:bosses` |

## 待办

- 正式模型与贴图（核心与符文终焉剑）。
- 终焉城堡中心的召唤方式、掉落物与整合包内的属性配置。
