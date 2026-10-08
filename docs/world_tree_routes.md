# 三层地牢与连续通路修复

维度 ID：`sdbf:deep_realm_level_2`。重启游戏后在新生成区块生效，已生成建筑不会自动重建。

## 生成逻辑

- 三类地牢均以三个上下重叠的楼梯枢纽组织楼层，层间高差 24 格。悬冠聚落与沉根观测枢纽向上展开，枯心回廊从树根入口向地下展开。
- 每层拥有自己的加权随机房间链、侧翼与支路；主房间按 2/2/2、3/3/2、3/3/3 分配到三层。研究核心位于末层的独立接口。
- 原版拼图负责模块选择、旋转与碰撞。缺少任何楼层、主房间或研究核心的结果都会被拒绝，保证完整路线和 3/4/5 个箱子。
- 共 108 个地牢模板、45 个拼图池；另有 18 个小型模板。

## 通路修复

- 拼接口补齐三格宽地板并向房间内延伸，清出三格高通路，修复门洞后仍被栏杆阻挡的情况。
- 楼梯采用连续踏步、折返平台与实体侧挡；跨层楼梯的最后两级落在上层模块内，保证楼层接缝没有整格台阶或断层。
- 枯心入口五段折返楼梯、观测站入口和核心上层回廊统一检查；核心楼梯起点避开战斗设施。
- 栅栏、铁栏杆和玻璃板根据相邻方块重新计算连接方向，修复转角和支线接口。断桥本身设计的断口保留。
- 地形接道末端与承托面齐平，按可用地形选择有限长度。上层支柱和锁链遇到其他模块的实体承重面即停止，防止穿透下层房间、路面与刷怪笼。

![实际三层布局](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/stacked_routes.png)

## 验证

- 24 个有效随机布局，三个自然生成实例实际加载至 FULL 区块，并验证保存与重载。
- 玩家碰撞测试使用 0.6 格宽、1.8 格高的玩家，默认 0.6 自动跨步，不飞行、不跳跃；楼梯和地形接道双向走通，验证门口跨越与三个楼层的 X/Z 重叠。
- PLAYER WALK: 2 stair routes both ways, 1 terrain ramps both ways and 78 doorway crossings; 0.6 step, no jump; three overlapping floors at [240, 264, 288]
- PASS canopy_village: random layouts=8, distinct=8, exact chests=3, concealed spawners=3, English plaques=2, dry air blocks=0; natural FULL placement and serialized supports/ports/access verified.
- PLAYER WALK: 4 stair routes both ways, 1 terrain ramps both ways and 100 doorway crossings; 0.6 step, no jump; three overlapping floors at [-23, 1, 25]
- PASS hollow_heart_corridors: random layouts=8, distinct=8, exact chests=4, concealed spawners=4, English plaques=2, dry air blocks=0; natural FULL placement and serialized supports/ports/access verified.
- PLAYER WALK: 4 stair routes both ways, 0 terrain ramps both ways and 134 doorway crossings; 0.6 step, no jump; three overlapping floors at [-34, -10, 14]
- PRESSURE: 64 vanilla fluid update passes; 49753 interior air cells remain dry; active updates=66216; opened-collar positive control admitted water and was restored
- PASS sunken_root_observatory: random layouts=8, distinct=8, exact chests=5, concealed spawners=4, English plaques=2, dry air blocks=49753; natural FULL placement and serialized supports/ports/access verified.
- 126 个模板的方块资产、英文标牌、禁用材料检查通过；42 个独立水密模板静态检查通过，三层观测站另外经过实际组装后的原版水流检查。
- 六类小型结构自然生成检查通过。箱子与刷怪笼 NBT、维度配置及全部战利品表相对本轮备份完全一致。
- 完整离线构建通过。生产 JAR 仅改变地牢结构类及其内部类；地形生成器、注册表、小型结构 Java 类与维度 ID 保持原值。

预览读取实际自然布局、生产 NBT 与安装模型纹理，为静态渲染；未进行客户端画面验收。隔离开发环境对 Quark 铁板/木炭块和少数 KubeJS 物品使用测试占位，生产使用已安装模组方块。

## 安装记录

- SHA-256：`0a4884dbf0f66d3c118c4c53a37809f5063405ff8d432621279ca1d878183466`
- 备份：`backups/world_tree_routes_before/`
- 验证：`local/world_tree_work/verification/routes/`
- 楼层与楼梯模板：`local/world_tree_work/world_tree_routes.py`
