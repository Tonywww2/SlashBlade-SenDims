# 深境二层：小型结构首版

维度 ID：`sdbf:deep_realm_level_2`。

六类结构各有完好、残损、茂盛三种变体，共 18 个实际 NBT 模板。每个结构只有一个战利品箱；圣龛和旧渡口的茂盛变体将箱子放在隐藏研究室内。

实验室外墙、顶板和外侧底板使用 `quark:iron_plate`，保留深色观察窗、铜制采样管、树脂样品罐和研究记录。沉水采样舱在初始生成时密封且干燥；进入时需要先关闭上舱口、用桶排空夹层积水，再开启下舱口。

## 总览

![六类小型结构](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/overview.png)

## 单张预览与剖面

这些图直接读取实际生成用的 NBT 和当前安装的方块模型、纹理；属于静态模板模型渲染，不是游戏内光影截图。箱子、流体、告示牌等 Minecraft 内置动态渲染对象采用简化显示。去顶剖面仅用于检查，实际模板保留屋顶及舱壁。支撑桩在世界生成时继续向下接到真实地形或粗树杈。

### 01 树脂采集站

15 × 17 格，粗根附近的小型采集棚，包含自然渗出树脂槽、沉淀盆、试管台和采样记录。

![树脂采集站](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/01_resin_collection_station.png)

### 02 悬枝瞭望亭

13 × 13 格，四角木桩接入真实粗枝，平台设有风向记录、木梯和阁楼旅行物资箱。

![悬枝瞭望亭](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/02_canopy_lookout.png)

### 03 根隙圣龛

17 × 21 格，苔石拱、根系、微光祭坛和台阶。部分变体在祭坛后面藏有检修口，可进入地下树脂研究室。

![根隙圣龛](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/03_root_shrine.png)

### 04 苇海旧渡口

21 × 27 格，岸侧仓棚、木栈桥和方块搭建的搁浅小舟。部分变体在仓棚下面隐藏研究室和旧运输记录。

![苇海旧渡口](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/04_old_reed_landing.png)

### 05 沉水采样舱

15 × 19 格，海底铁板玻璃舱，保留铜制采样管、双层舱口和树脂压力实验记录。不同变体有修补观察窗、损坏外部管线或水生附着植物，舱壁保持密封。

![沉水采样舱](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/05_submerged_sampling_capsule.png)

### 06 断裂蔓桥

35 × 9 格，两端接入同一冠层的粗枝，链索和桥面略有下垂。残损变体的中央缺口更大，桥头只有一个物资箱。

![断裂蔓桥](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_small_structures/previews/06_broken_vine_bridge.png)

## 战利品

- 旅行箱：食物、照明、箭、线、们瑞欧浆果和少量晶体等。
- 采样箱：树脂桶、空瓶、纸、铜锭、普通空白变量卡和少量材料。
- 水下箱：鱼、海晶碎片、海灯、鳞甲和实验样品等。
- 研究箱：们瑞欧晶体、少量晶体块、红石、青铜和带正文的研究日志。
- 部分箱子抽取破碎的耀魂、`nuclearcraft:steel_ingot` 钢锭、Quark 木炭块或逻辑线缆；碱晶体各种粉末只在研究/水下主题箱中出现。
- 全部新增战利品表均无附魔书。
- 神秘碱晶体：碱晶体样品池权重 **1 / 131**，每次固定 **1 枚**；样品池又只在部分箱子中抽取。其余粉末数量 1–3。此调整作用于新结构的箱子战利品。

## 数据与源码

- 模板：`kubejs/data/sdbf/structures/world_tree/`。
- 结构定义：`kubejs/data/sdbf/worldgen/structure/world_tree/`。
- 稀疏随机分布：`kubejs/data/sdbf/worldgen/structure_set/world_tree/`，地表/水域区域间距 14 区块，冠层间距 16 区块；候选点仍须通过实际地形、树杈与空间检查。
- 战利品：`kubejs/data/sdbf/loot_tables/chests/world_tree/`。
- Java：`local/world_tree_work/java/WorldTreeSmallStructure.java`，正式源码在已授权的 SlashBlade-SenDims 项目中。
- 可复现模板和图片：`local/world_tree_work/build_small_structures.py`、`render_small_structures.py`。
- 验证报告：`local/world_tree_work/verification/small_structures/`。

生成起点只接受世界树海生成器和本维度的三个群系。新增结构不使用地表最高叶片作为定位高度；候选点扫描次数有固定上限，支撑与模板写入裁剪到当前区块。原有维度配置、树形、蕴门原木概率和原有自然结构定位逻辑保留。

本阶段建造六类小型结构。已批准的悬冠聚落、枯心回廊和沉根观测枢纽继续保留为后续拼图地牢设计。
