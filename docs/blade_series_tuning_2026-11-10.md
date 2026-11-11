# 拔刀剑系列调整（2026-11-10）

本轮改动分两部分：SenDims 代码（本仓库）和整合包 kubejs 数据（实例目录 `kubejs/`）。服务端要同时更新 jar 和 kubejs 才完整。

## 数值与规则

| 项目 | 规则 | 位置 |
| --- | --- | --- |
| 褪名 | 耐久区间 90% / 50% | `se/NamelessSeries`（`FADED_HIGH_RATIO`、`FADED_LOW_RATIO`） |
| 风暴穿透 | 每级攻击力 +5%，MULTIPLY_TOTAL | `effect/StormPenetrationEffect` |
| 无王三式 | 横扫 ×3、反斩 ×3、刺击 ×1、雷击 ×1、追击 ×4（合计仍 12）；AP 300 + 15% | `sa/NamelessThreefold.RATIOS`；kubejs `server_scripts/miscs/sa_ap_cost.js` |
| 石中剑系列 | 耐久 160 | kubejs `data/slashblade_sendims/slashblade/named_blades/{caliburn,excalibur}.json` |
| 阿瓦隆（誓约胜利之剑） | 蓄力第 15 tick 获得 20 tick 闪避（拔刀剑原生 `Untouchable`）并显示同名药水效果；冷却 120 tick，记在玩家 PersistentData `sbsd.se.avalon.next_ready` | `se/Avalon`、`effect/AvalonEffect` |
| 石中剑蓄力判定 | 使用物品不足 5 tick 不算蓄力，普通挥刀不再触发蓄力效果 | `se/HolySwordEffects.CHARGE_MIN_TICKS` |
| 鸦（天云全系） | 斩击命中时从命中处放出 2 片红枫，追踪离红枫最近的敌人（16 格），造成 `5 + x × 攻击力` 魔法伤害；同一 tick（同一次挥刀 / 同一段 SA）只放一次 | `se/AmagumoCrow`、`entities/EntityMapleLeaf` |
| x | `max × (1 − 0.75^((移速 / 0.1 − 1) × rate))`，移速不超过 0.1 时为 0；鸦 max 20%、rate 1.5；带风 max 40%、rate 2.5 | `AmagumoCrow.bonus` |
| 风 | 无念、速子 | 同上 |
| 攻击累计出血量表 | 加给狱界楼观剑、狱刀似蛭、炼狱刀死念、耀魂似蛭（不含绝念） | kubejs named_blades |
| 出血累计 | 每次命中 `30 + 200% 攻击力`，上限为目标最大生命值 | `compat/lsp/BleedRules`、`mixin/lsp/MixinBleedingCutting` |
| 出血爆发 | `已损失生命 × 15% + 最后攻击者的攻击力`，分 4 段；攻击者不在场时攻击力按 0 | `mixin/lsp/MixinBloodAccumulationEvent` |
| 出血衰减 | 目标 5 秒未受伤后，每秒扣除上限的 1%、2%、4%、8%、16%、16%……；受伤（原版 `lastDamageStamp`）即重新计时 | 同上，`BleedRules.decayPercent` |
| 莉芙系列 | 颂生者、EX·段生铸念不再给抗性提升（力量等保留） | `mixin/sjap_adder/ForLifeMixin` |
| 颂生者 | 额外 10 秒增幅 III，并追加一道 2.0 倍斩击 | `compat/sjap/LifeSeries.afterForLife` |
| EX·段生铸念 | 额外 12 秒增幅 V，并追加一道 4.0 倍斩击 | 同上 |
| 增幅 | 每级攻击力 +5%，MULTIPLY_BASE | `effect/AmplifyEffect` |
| 生露 | 期间照常受伤，按攻击者记录最终伤害（PersistentData `sbsd_life_dew_record`）。效果到期或被移除时结算：攻击者每造成玩家最大生命 5% 的伤害，回敬一次 1.5 倍攻击力的魔法伤害，单个目标最多 8 次。原版"生露期间造成伤害 +30%/级"下调为每级 10%（SA 已提供增幅），且不作用于回敬伤害 | `compat/sjap/LifeSeries`、`mixin/sjap_adder/ForLifesMixin` |

## 已有物品的迁移

拔刀剑的 SE 和耐久在生成时写进物品 NBT，改 JSON 只影响新生成的刀。`se/BladeDefinitionMigration` 在玩家背包内的刀更新时按翻译键补齐：

- 必胜黄金之剑：耐久 +120；誓约胜利之剑：+阿瓦隆、耐久 +120（旧定义未写耐久，默认 40）。
- 天云 4 把：+鸦；无念、速子另 +风。
- 4 把出血刀：+攻击累计出血量表。

处理后写入 `sbsd_def_rev = 1`。按新定义生成的刀在 `SlashBladeDefinition.getBlade` 返回时直接打标记（`mixin/slashblade/SlashBladeDefinitionMixin`），不会被重复处理。以后再调整这些刀时，提高 `REVISION` 并改写规则。

## 注意

- 阿瓦隆复用了原生闪避，`UntouchableMixin` 里"闪避成功时返还马娘 AP"的逻辑对阿瓦隆同样生效。
- 红枫在服务端运行，不写入存档。目标搜索用 `TargetSelector` 的 4 参数版本并显式传入距离：3 参数版本只认玩家近战距离（实体触及 − 1，约 2 格）以内的目标。

## 测试

`gametest/SeriesTuningGameTests`（batch `series_tuning`，11 项）覆盖增幅/风暴穿透的属性计算、阿瓦隆触发与冷却、鸦公式与红枫实际命中、出血的每击数值/爆发/衰减、莉芙系列效果、生露记录与回敬、旧刀迁移。`HolySwordGameTests.windDistanceBoundaryAndCharging` 增加了"刚开始使用不算蓄力"的断言，`NativeSlashGameTests` 的首段横扫改为 3 倍。

运行：`gradlew.bat --offline runGameTestServer -PexcludeGiselleFromServerRuntime`（Giselle 附属在专用服务端加载客户端类会崩溃）。`gradlew build` 中的 `clientPerformanceTest` 需要 JDK 17 作为 `JAVA_HOME`。

## 发布

已并入整合包更新包 `docs/releases/SDBF-Update-2026-11-08.zip`（实例目录，第 7 节）。包内 SenDims JAR 同时包含此前单独发布的 SenDims-Tofu-Crystal，`special_loots.js` 也换成合并版本；安装脚本会清理其他版本的 `slashblade_sendims-*.jar`。
