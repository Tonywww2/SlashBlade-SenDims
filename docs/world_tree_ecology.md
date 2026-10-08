# 深境二层 · 自然地物

维度 ID：`sdbf:deep_realm_level_2`。共 15 类，重启游戏后在新生成区块生效。

## 已实现地物

| 编号 | 地物 | 代码标识 | 测试世界实例坐标 |
|---|---|---|---|
| 1 | 层叠树菌棚 | `shelf_fungi` | `[132, 254, -78]` |
| 2 | 枝间花园 | `branch_garden` | `[75, 262, -35]` |
| 3 | 悬垂藤幕 | `hanging_vines` | `[76, 266, -79]` |
| 4 | 中空朽木 | `hollow_log` | `[-19, 68, 24]` |
| 5 | 腐殖菌毯 | `humus_mat` | `[125, 72, 111]` |
| 6 | 林间浅水洼 | `forest_pool` | `[74, 66, -120]` |
| 7 | 根系浮洲 | `root_raft` | `[29, 61, -31]` |
| 8 | 巨莲叶群 | `lily_colony` | `[-67, 62, -81]` |
| 9 | 漂木汇集带 | `driftwood` | `[14, 60, -67]` |
| 10 | 沉木生态丘 | `sunken_log` | `[-323, -37, -29]` |
| 11 | 碱晶簇礁 | `crystal_reef` | `[211, 33, -310]` |
| 12 | 海底草甸 | `seagrass_meadow` | `[267, 7, 66]` |
| 13 | 板根苔阶 | `moss_root_steps` | `[-63, 86, 33]` |
| 14 | 入水根须丛 | `submerged_rootlets` | `[169, 73, 68]` |
| 15 | 根缝积物窝 | `root_detritus` | `[107, 62, -35]` |

实例坐标来自固定布局参数、种子 20261004 的隔离测试世界；其他存档的结构避让结果可能不同。

## 生成方式

- 48 格生态单元，地面/水面/海床一组候选，粗根与高处树枝另有独立候选。候选还要通过地形与承托检查，不代表每个单元都会生成。
- 地物形状含不规则边缘、不同尺度与局部空隙。倒木带断口、弯折和短残枝；苔土与菌类混生；水下沉木附近有草甸。
- 树干菌棚贴住树皮，树枝花园与根部苔阶寻找木质表面，藤幕从枝底悬垂，根须沿弯曲路径连接到原有粗根。
- 水洼检查四周封闭边缘和底床；巨型睡莲一次设计完整四片组合；地表替换后清理失去承托的旧植被。
- 碱晶簇礁约占深水候选的 12%，每簇至多新增 5 个普通碱晶簇方块。未增加战利品箱或神秘碱晶体物品。
- 所有现有自然结构使用边界外扩 12 格的保护空间。相交的整组地物跳过，以保留道路、入口、楼梯、桥面与水密舱室。
- 使用 Integrated Dynamics、Biome Makeover、Quark、暮色森林及原版自然方块。没有使用深暗之园、绳圈，填充原木分布公式保持原值。

## 性能与一致性

- 坐标决定地物布局；每次仅写当前区块，不写入邻接区块。
- 复用世界树空间索引与列缓存；地物方案使用容量 256 的有限缓存，不进行整维度或整柱扫描。
- 结构检查读取 FEATURES 阶段已有的结构引用；不主动加载额外完整区块。
- 植被清理仅覆盖本次变更的列，生成时不批量触发邻居或流体更新。

## 验证

```text
Registry: all Biome Makeover + Twilight Forest natural blocks present; Quark glow/hollow=true/true
Distribution: 289 cells, {SHELF_FUNGI=12, BRANCH_GARDEN=22, HANGING_VINES=16, HOLLOW_LOG=24, HUMUS_MAT=19, FOREST_POOL=21, ROOT_RAFT=29, LILY_COLONY=27, DRIFTWOOD=22, SUNKEN_LOG=3, CRYSTAL_REEF=3, SEAGRASS_MEADOW=17, MOSS_ROOT_STEPS=22, SUBMERGED_ROOTLETS=3, ROOT_DETRITUS=18}, planning_ms=308.5569
PASS deterministic eviction, reverse order and 4 workers (49 cells)
PASS SHELF_FUNGI origin=BlockPos{x=132, y=254, z=-78} placed=72/72 plants=14 seamBlocks=5
PASS BRANCH_GARDEN origin=BlockPos{x=75, y=262, z=-35} placed=47/47 plants=17 seamBlocks=1
PASS HANGING_VINES origin=BlockPos{x=76, y=266, z=-79} placed=445/445 plants=445 seamBlocks=91
PASS HOLLOW_LOG origin=BlockPos{x=-19, y=68, z=24} placed=142/142 plants=22 seamBlocks=2
PASS HUMUS_MAT origin=BlockPos{x=125, y=72, z=111} placed=139/139 plants=57 seamBlocks=26
PASS pool rim/floor and 12 actual fluid update passes: 14 source blocks
PASS FOREST_POOL origin=BlockPos{x=74, y=66, z=-120} placed=109/111 plants=10 seamBlocks=0
PASS ROOT_RAFT origin=BlockPos{x=29, y=61, z=-31} placed=75/85 plants=22 seamBlocks=23
PASS complete giant lily groups: 3
PASS LILY_COLONY origin=BlockPos{x=-67, y=62, z=-81} placed=16/16 plants=16 seamBlocks=3
PASS DRIFTWOOD origin=BlockPos{x=14, y=60, z=-67} placed=135/136 plants=2 seamBlocks=24
PASS SUNKEN_LOG origin=BlockPos{x=-323, y=-37, z=-29} placed=309/309 plants=264 seamBlocks=61
PASS CRYSTAL_REEF origin=BlockPos{x=211, y=33, z=-310} placed=95/98 plants=0 seamBlocks=6
PASS SEAGRASS_MEADOW origin=BlockPos{x=267, y=7, z=66} placed=1032/1032 plants=997 seamBlocks=135
PASS MOSS_ROOT_STEPS origin=BlockPos{x=-63, y=86, z=33} placed=67/67 plants=24 seamBlocks=20
PASS SUBMERGED_ROOTLETS origin=BlockPos{x=169, y=73, z=68} placed=44/44 plants=0 seamBlocks=2
PASS ROOT_DETRITUS origin=BlockPos{x=107, y=62, z=-35} placed=17/17 plants=6 seamBlocks=0
PASS actual portal arrival BlockPos{x=-36, y=69, z=67}
Planning lookup 256 chunks ms=60.3865
PASS: 15 natural feature types, loaded FULL chunks, support, boundaries, registry and structure exclusions
```

方案查询计时只覆盖地物计算与缓存访问，不代表完整载图时间。三类现有地牢在新增地物开启后再次通过实际楼梯/道路碰撞、拼图、Boss 刷怪笼及水密检查；完整离线构建通过。

现有 375 个 `kubejs/data/sdbf` 文件逐项哈希一致，包含维度、群系、结构模板、拼图池和战利品表。生产 JAR 的变更限定在地物辅助类、世界树索引辅助方法和生成器接入。

## 实际方块预览

![自然地物预览](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_ecology/previews/overview.png)

预览来自实际 FULL 区块导出的 NBT 与整合包模型纹理，是静态渲染。为展示水下地物隐藏了水体，仅保留周围少量地形；不是客户端截图，尚未进行游戏内视觉验收。

## 安装

- 模组：`mods/slashblade_sendims-1.0.22.jar`
- SHA-256：`f8a1d2aa05aba386c47de4241c5b5e0cf830e9cde9084108da24e187e7f416e5`
- 备份：`backups/world_tree_ecology_before/`
- Java 源码：`local/world_tree_work/java/WorldTreeEcology.java`，已同步到指定模组工作区。
- 验证与真实区块导出：`local/world_tree_work/verification/ecology/`
