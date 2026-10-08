# 区域型地物扩展与过渡带

维度：`sdbf:deep_realm_level_2`。重启游戏后在新生成区块生效。

## 变化

- 地面腐殖苔地区域：核心半径由 5–7 格提高到 14–20 格，增加约 10–13 格渐疏边缘。核心混合苔藓块、苔藓泥炭、腐殖土和灰化土，外围保留原土壤并铺设不连续的苔藓地毯，向外渐变为零散蕨类和原有植被。
- 枝间花园：核心半径 8–12 格，约 7 格过渡带；只覆盖实际树枝承托面，外围由实体土层转为贴木面的苔藓地毯。
- 板根苔阶：沿根延伸的核心半径 14–18 格，约 9 格过渡带；根缝腐殖积物也扩大，并增加地毯状边缘。
- 根系浮洲：核心半径 7–10 格，约 6–8 格过渡带。外围逐渐增加水面空隙，由泥炭与苔藓转为裸露根系、苔藓地毯和散落睡莲。
- 巨莲叶群：核心半径 13–18 格，约 10 格外围，逐渐降低密度并以小型睡莲过渡；相邻睡莲群落与浮洲按候选位置检查空间，巨型睡莲保持完整四片组合。
- 海底草甸：核心半径 24–30 格，约 12 格过渡带；高海带集中于核心，向外降低植被密度与高度，最终衔接原海床。沉木周围的伴生草甸也扩大。

尺寸是候选覆盖范围，实际结果受树木承托、陆海边界和结构保护区限制。轮廓经过旋转、连续噪声扰动和密度渐变，保留不规则缺口。

## 覆盖对照

同一 169 个候选单元中新旧方案的累计水平覆盖列数；按地物分别计数，并非整张地图的去重面积。比例包含较大的方案能够触及可用地面后新增的有效地物。

| 地物 | 旧版 → 新版覆盖列 | 比例 | 新版苔藓地毯数 |
|---|---:|---:|---:|
| 枝间花园 | 422 → 3009 | 7.13× | 370 |
| 腐殖菌毯 / 地面苔藓区 | 1025 → 20453 | 19.95× | 6865 |
| 巨莲叶群 | 125 → 1576 | 12.61× | 0 |
| 板根苔阶 | 550 → 5683 | 10.33× | 829 |
| 根缝积物窝 | 327 → 2423 | 7.41× | 369 |
| 根系浮洲 | 806 → 4796 | 5.95× | 1737 |
| 海底草甸 | 933 → 6776 | 7.26× | 0 |

实际实例：腐殖苔地约 49×45 格，444 块苔藓地毯；海底草甸约 67×60 格；板根覆盖约 27×39 格。以上均来自测试世界已生成区块。

## 性能与衔接

- 延续坐标确定的方案和 256 单元有界缓存；新增按目标区块预分组的方块索引，写入时只遍历本区块的方块。
- 区域覆盖在建筑外扩 12 格的保护空间前停止；大型睡莲、双格植物按完整组合避让。
- 结构引用仅从生成阶段已经可读的区块获取。远端结构起点不可读取时保守留空，避免额外区块加载。
- 原维度 ID、地形、树木、填充原木概率、结构模板、拼图池、Boss 刷怪笼和战利品数据保持原值。

## 验证

```text
REGIONS: BRANCH_GARDEN groups=14 horizontal columns 422 -> 3009 ratio=7.13 carpets=370
REGIONS: HUMUS_MAT groups=22 horizontal columns 1025 -> 20453 ratio=19.95 carpets=6865
REGIONS: LILY_COLONY groups=12 horizontal columns 125 -> 1576 ratio=12.61 carpets=0
REGIONS: MOSS_ROOT_STEPS groups=16 horizontal columns 550 -> 5683 ratio=10.33 carpets=829
REGIONS: ROOT_DETRITUS groups=12 horizontal columns 327 -> 2423 ratio=7.41 carpets=369
REGIONS: ROOT_RAFT groups=15 horizontal columns 806 -> 4796 ratio=5.95 carpets=1737
REGIONS: SEAGRASS_MEADOW groups=4 horizontal columns 933 -> 6776 ratio=7.26 carpets=0
REGIONS: PASS 165 plans / 100900 voxels indexed exactly once; baseline comparison ms=1047.615
Registry: all Biome Makeover + Twilight Forest natural blocks present; Quark glow/hollow=true/true
Distribution: 289 cells, {SHELF_FUNGI=12, BRANCH_GARDEN=22, HANGING_VINES=16, HOLLOW_LOG=26, HUMUS_MAT=29, FOREST_POOL=21, ROOT_RAFT=29, LILY_COLONY=30, DRIFTWOOD=22, SUNKEN_LOG=3, CRYSTAL_REEF=3, SEAGRASS_MEADOW=17, MOSS_ROOT_STEPS=22, SUBMERGED_ROOTLETS=3, ROOT_DETRITUS=18}, planning_ms=1618.5244
PASS deterministic eviction, reverse order and 4 workers (49 cells)
PASS SHELF_FUNGI origin=BlockPos{x=132, y=254, z=-78} placed=72/72 plants=14 seamBlocks=5
REGIONS: NATURAL BRANCH_GARDEN bounds=17x19 carpets=12 outside_radius_15=0
PASS BRANCH_GARDEN origin=BlockPos{x=75, y=262, z=-35} placed=191/191 plants=82 seamBlocks=11
PASS HANGING_VINES origin=BlockPos{x=76, y=266, z=-79} placed=445/445 plants=445 seamBlocks=91
PASS HOLLOW_LOG origin=BlockPos{x=-67, y=68, z=12} placed=395/395 plants=165 seamBlocks=43
REGIONS: NATURAL HUMUS_MAT bounds=49x45 carpets=444 outside_radius_15=825
PASS HUMUS_MAT origin=BlockPos{x=125, y=72, z=111} placed=1891/1891 plants=848 seamBlocks=244
PASS pool rim/floor and 12 actual fluid update passes: 14 source blocks
PASS FOREST_POOL origin=BlockPos{x=74, y=66, z=-120} placed=109/111 plants=10 seamBlocks=0
REGIONS: NATURAL ROOT_RAFT bounds=23x30 carpets=106 outside_radius_15=2
PASS ROOT_RAFT origin=BlockPos{x=29, y=61, z=-31} placed=687/749 plants=248 seamBlocks=81
REGIONS: NATURAL LILY_COLONY bounds=41x40 carpets=0 outside_radius_15=44
PASS complete giant lily groups: 28
PASS LILY_COLONY origin=BlockPos{x=-67, y=62, z=-81} placed=171/171 plants=171 seamBlocks=25
PASS DRIFTWOOD origin=BlockPos{x=14, y=60, z=-67} placed=135/136 plants=2 seamBlocks=24
PASS SUNKEN_LOG origin=BlockPos{x=-323, y=-37, z=-29} placed=840/840 plants=793 seamBlocks=129
PASS CRYSTAL_REEF origin=BlockPos{x=211, y=33, z=-310} placed=95/98 plants=0 seamBlocks=6
REGIONS: NATURAL SEAGRASS_MEADOW bounds=67x60 carpets=0 outside_radius_15=3726
PASS SEAGRASS_MEADOW origin=BlockPos{x=267, y=7, z=66} placed=5381/5604 plants=5269 seamBlocks=694
REGIONS: NATURAL MOSS_ROOT_STEPS bounds=27x39 carpets=97 outside_radius_15=57
PASS MOSS_ROOT_STEPS origin=BlockPos{x=-63, y=86, z=33} placed=772/772 plants=362 seamBlocks=90
PASS SUBMERGED_ROOTLETS origin=BlockPos{x=169, y=73, z=68} placed=44/44 plants=0 seamBlocks=2
REGIONS: NATURAL ROOT_DETRITUS bounds=13x10 carpets=18 outside_radius_15=0
PASS ROOT_DETRITUS origin=BlockPos{x=107, y=62, z=-35} placed=109/111 plants=54 seamBlocks=6
PASS actual portal arrival BlockPos{x=-36, y=69, z=67}
Planning lookup 256 chunks ms=662.0097
PASS: 15 natural feature types, loaded FULL chunks, support, boundaries, registry and structure exclusions
```

15 类地物实际生成、外围苔藓地毯、缓存清理与反序、多线程、区块索引完整性、水洼流体更新、巨型睡莲组合与传送落点检查通过。三类地牢 24 个随机布局、实际楼梯/道路行走、Boss 刷怪笼、水密检查再次通过；完整离线构建通过。

计时为方案计算/查询，不代表完整游戏载图时间。客户端视觉尚待游戏内检查。

## 实际区块预览

![腐殖苔地与边缘](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_ecology/previews/regions/05_humus_mat.png)

[全部地物预览](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_ecology/previews/regions/overview.png)。来自实际区块 NBT 与安装模型纹理的静态渲染；树根和地形在切片边缘被截断，水下部分隐藏水体。

## 安装

- `mods/slashblade_sendims-1.0.22.jar`
- SHA-256：`4a0ef322b12511cf99988523ef703fbaebe27ddd8cffd2f4e2f016e06f8dd9df`
- 备份：`backups/world_tree_ecology_regions_before/`
- 验证：`local/world_tree_work/verification/ecology_regions/`
- 生产数据包 375 个文件哈希一致。JAR 仅修改地物辅助类与清单元数据。
