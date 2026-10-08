# 深境二层：中大型拼图结构

## 三层地牢与连续通路（2026-10-05）

当前版本采用上下堆叠的三个楼层，每层有随机房间与支路，层间高差 24 格。修复楼梯、道路、栏杆接口及支柱穿楼问题。最新结果：[修复与验证记录](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/routes.md)。下方早期设计记录以本节为准。

## 屋顶、立体布局与水密修复（2026-10-05）

屋顶加厚并加入斗拱；枯心入口扩大屋檐、缩小井道；观察站密封接缝并增加科技外饰。三类地牢均有三层主路线，研究核心增加上层回廊。信息牌由 64 减至 12，移除绳圈；断桥选择低枝并检查叶片净空。

详情与新版预览：[建筑修复记录](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/architecture.md)。

## 自然木色与室内装饰更新（2026-10-05）

红色框边活木板/苔活木板已从所有模板移除，改为深褐色古橡木与们瑞欧木板的连贯木框。加入木板嵌条、编织垫、石质边带、苔痕、壁架、盆栽、附墙藤蔓和实验室管线面板；实验室铁板外壳、箱子、英文警句板及战斗参数保留。九类结构自然生成和通行/实际出怪检查通过。

详情与新版剖面：[室内装饰更新](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/interiors.md)。

维度 ID：`sdbf:deep_realm_level_2`。

已建造三类地牢，共 99 个实际 NBT 模块、36 个原版加权拼图池。入口、主路、转角、支廊、样品间与研究核心通过 Minecraft `JigsawPlacement` 随机拼接；缺失核心、路线不连通、超出范围或缺少承重支撑的布局会被拒绝。相邻且对齐的侧门会连成额外捷径，未连接的门口会封闭。

![三类结构总览](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/overview.png)

## 结构与预览

这些预览从实际自然生成的布局读取生产 NBT，再使用当前安装的方块模型和纹理渲染。不是客户端截图；宿主世界树、土壤和海水未显示。长支撑在图中只显示前 12 格，实际生成延伸至树杈或海底。剖面会移除顶板，生产模板保留完整外壳。箱子、流体等内置动态渲染对象使用简化显示。

### 悬冠聚落

粗树杈上的木屋、栈道与下挂研究室。隐藏舱口通向铁板包围的树脂实验间。

- 本轮图中实例 16 个模块；支路数量随拼接碰撞变化。
- 每处严格 3 个战利品箱，放在入口或主路补给点、研究室与核心中；随机支路不额外增加箱子。
- 自然定位命令：`/locate structure sdbf:world_tree_dungeons/canopy_village`。

![悬冠聚落外观、剖面与随机布局](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/canopy_village.png)

### 枯心回廊

根部井道连通三层地下旧廊，苔石与木梁之间藏有树脂析晶研究室。

- 本轮图中实例 22 个模块。
- 每处严格 4 个战利品箱，放在入口或主路补给点、研究室与核心中；随机支路不额外增加箱子。
- 自然定位命令：`/locate structure sdbf:world_tree_dungeons/hollow_heart_corridors`。

![枯心回廊外观、剖面与随机布局](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/hollow_heart_corridors.png)

### 沉根观测枢纽

深海中的密封铁板枢纽，包含观察廊、环行样品间、压力核心和双舱口入口。

- 本轮图中实例 27 个模块；三层主路和支路随机组合。
- 每处严格 5 个战利品箱，放在入口或主路补给点、研究室与核心中；随机支路不额外增加箱子。
- 自然定位命令：`/locate structure sdbf:world_tree_dungeons/sunken_root_observatory`。

![沉根观测枢纽外观、剖面与随机布局](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/sunken_root_observatory.png)

## 装饰、入口与战斗更新（2026-10-05）

- 采用批准清单中保留的 20 种装饰方块：古橡木、芦苇茅草、锁链与纸灯、暮色森林地牢砖/扭曲石柱、工厂装饰管道与通风板、工业灯、Quark 铁柱/框架玻璃和白色小瓷砖。没有使用 `undergarden:` 方块。
- 全部 117 个模板中的告示牌均替换为 `pneumaticcraft:aphorism_tile`，现为 12 处；提示、研究标识和日志统一为英文。实验室外壳仍以 Quark 铁板为主。
- 悬冠聚落增加三格宽接枝栈道；枯心回廊增加五格宽地面引道。高度按真实地形或实木树杈计算，尽端落到支撑面，路线头顶留空，无法合理衔接的候选位置被拒绝。
- 枯心入口使用五段两格宽真实楼梯和宽转角平台；地牢内部下层楼梯同样改为楼梯方块。沉根入口设登舱平台、两段三格宽铁板楼梯和短气闸检修梯。
- 30 个模板刷怪笼全部加遮蔽；实际自然生成的三类地牢和四个战斗点已验证可以出怪且保留增强后的生命值。

![入口与楼梯间](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/entrances.png)

完整变更与验证：[本轮记录](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/refinement.md)。

## 生成与玩法

- 悬冠聚落从真实树杈定位，木柱或吊链连接实际树木；连续平台只允许短距离悬挑。至少四个模块有两处以上实际锚点。
- 枯心回廊入口位于粗根附近的稳定地面，五段阶梯向下通到旧廊，再分两次下行形成三层回廊。地下房间必须被地面覆盖。
- 沉根观测枢纽避开主干密集区，整体抬到覆盖范围内采样海底的最高处，铁柱补齐落差；舱顶保留在海面下。入口有两道铁舱口、两侧按钮和夹层梯子。入舱时关闭上舱口，用桶排空夹层后再打开下舱口。
- 实验室外围为 `quark:iron_plate`，配观察窗、铜管、树脂样品罐和研究日志。没有添加高产自动化设备。
- 刷怪笼藏在完整木柜、苔砖根座或设备箱内。普通房间间隔 160–320 tick、每轮尝试 3 只、附近上限 8 只；核心间隔 100–200 tick、每轮尝试 4 只、附近上限 10 只。混合蜘蛛/骷髅、骷髅/僵尸或溺尸/骷髅；基础生命值分别为 36/48，配护甲和零掉率武器。有效激活距离 20 格，出生范围 5 格，实际数量受空间和附近实体数量限制。
- 起点间距：聚落/回廊 36 区块、枢纽 40 区块，并排除靠近对应小型结构候选点的区域。只有通过位置与布局检查的候选点才生成。现有自然结构和世界树地形逻辑保持原样。
- 支撑与模板写入均裁剪到当前区块；每个候选区块最多三个定位点、每点最多四次有限拼接，避免无上限重试。布局的水平范围限制在 Minecraft 结构引用可覆盖的八区块范围内。

## 战利品

沿用先前批准的旅行、采样、水下与研究箱。核心箱新增独立主题表，含普通们瑞欧晶体、空白变量卡、逻辑线缆、红石或木炭块，并保证一本具有三页正文的研究日志。

核心材料奖励每箱抽取一次：

| 权重 | 材料 | 数量 |
| --- | --- | --- |
| 35 | 核电工艺钢锭 + 破碎的耀魂 | 钢锭 3–6，耀魂 2–5 |
| 30 | 伽马粉尘 | 1–3 |
| 13 | 青铜锭 | 3–6 |
| 12 | 铍粉 | 1–2 |
| 10 | 干燥盆 | 1 |

所有新表无附魔书。核心箱有 30% 概率抽取一次现有碱晶体样品池；神秘碱晶体仍为该池权重 1/131，固定一枚，约占核心箱的 0.229%。其余主题箱和自然方块掉落表没有改动。

## 验证与安装

- 99 模块逐一检查方块 ID/属性、刷怪实体和资源引用；静态通路检查覆盖全部拼图端口、箱子、井道转角和隐藏研究室梯子。
- 独立 Forge 世界中验证 24 个有效随机布局，三类各有八个不同布局。三处自然起点完成 FULL 区块生成，实际门洞、门下地板、连续支撑、箱子内容、刷怪笼、标记清除、保存/重载与维度范围检查通过。
- 箱子数精确为 3/4/5。海底实例 20,784 个空气方块通过流体更新检查，保持无水。三类核心表各抽取 3,000 次，神秘碱晶体各出现八次，每次一枚。
- 隔离 Forge 使用真实 PneumaticCraft 与新增装饰模组，已验证警句板实际方块实体及英文文本保存。开发环境仅为 Quark 铁板/木炭块和生产 KubeJS 物品保留测试占位；生产使用已安装的真实方块。预览直接读取真实模型纹理，动态警句板文字未在静态图中绘制。尚未进行客户端画面验收。
- `gradlew build --offline` 及项目既有 9,000 列地形、接缝、缓存顺序、四线程一致性等检查通过。
- 本轮生产 JAR 更新地牢与小型结构两个类；世界树生成器、注册表、维度配置和全部战利品表保持原样。

重启游戏后，在新生成区块寻找这些结构。生产模组仍为 `mods/slashblade_sendims-1.0.22.jar`，SHA-256：`67c3b0448c24bdfc8a7bb8ce0330d72bb8250ae59c1348ec553e443432531a35`。本轮备份：`backups/world_tree_architecture_before/`。

## 文件

- 模板：`kubejs/data/sdbf/structures/world_tree_dungeons/`。
- 拼图池：`kubejs/data/sdbf/worldgen/template_pool/world_tree_dungeons/`。
- 结构与分布：`kubejs/data/sdbf/worldgen/structure/world_tree_dungeons/`、`structure_set/world_tree_dungeons/`。
- 核心战利品：`kubejs/data/sdbf/loot_tables/chests/world_tree_dungeons/`。
- Java：`local/world_tree_work/java/WorldTreeDungeonStructure.java`，正式源码同步至授权的 SlashBlade-SenDims 项目。
- 可复现构建/预览脚本：`local/world_tree_work/build_world_tree_dungeons.py`、`render_world_tree_dungeons.py`。
- 详细报告与实际布局导出：`local/world_tree_work/verification/dungeons/`。
