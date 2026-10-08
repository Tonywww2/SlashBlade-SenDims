# Worldgen Record

本文档用于记录 `SlashBlade_SenDims` 项目中 worldgen 相关内容的现状、已确认方案、后续决策和待办事项。

后续在本对话中，只要 worldgen 方案、代码结构、数据资源或实现状态发生变化，都应同步更新本文档。

## 当前范围

当前 worldgen 主要围绕一个自定义维度展开：

- 维度 ID：`slashblade_sendims:saturn_ring`
- Minecraft 版本：`1.20.1`
- Forge 版本：`47.4.0`
- Mod ID：`slashblade_sendims`
- 主要 Java 包：`com.tonywww.slashblade_sendims.worldgen`
- 主要资源路径：`src/main/resources/data/slashblade_sendims`

## 注册链路

worldgen 的注册入口在主 Mod 类中：

- `src/main/java/com/tonywww/slashblade_sendims/SenDims.java`
  - 构造器中调用 `SaturnRingWorldGenRegistry.register(modEventBus)`。

worldgen codec 注册集中在：

- `src/main/java/com/tonywww/slashblade_sendims/worldgen/SaturnRingWorldGenRegistry.java`
  - 注册 `Registries.CHUNK_GENERATOR`
    - ID：`slashblade_sendims:saturn_ring_chunk_generator`
    - Codec：`SaturnRingChunkGenerator.CODEC`
  - 注册 `Registries.BIOME_SOURCE`
    - ID：`slashblade_sendims:saturn_ring_biome_source`
    - Codec：`SaturnRingBiomeSource.CODEC`

资源侧维度声明：

- `src/main/resources/data/slashblade_sendims/dimension/saturn_ring.json`
  - `type`：`slashblade_sendims:saturn_ring_type`
  - `generator.type`：`slashblade_sendims:saturn_ring_chunk_generator`
  - `generator.biome_source.type`：`slashblade_sendims:saturn_ring_biome_source`

维度类型声明：

- `src/main/resources/data/slashblade_sendims/dimension_type/saturn_ring_type.json`
  - `fixed_time`：`18000`
  - `has_skylight`：`true`
  - `has_ceiling`：`false`
  - `natural`：`false`
  - `bed_works`：`false`
  - `respawn_anchor_works`：`false`
  - `min_y`：`0`
  - `height`：`192`
  - `logical_height`：`192`
  - `effects`：`minecraft:the_end`
  - `monster_spawn_light_level`：`0`
  - `monster_spawn_block_light_limit`：`0`

## Java 文件职责

### `SaturnRingBiomes.java`

定义土星环维度使用的 biome `ResourceKey<Biome>`：

- `VOID_RING`
- `INNER_FADING_RING`
- `INNER_SPARSE_RING`
- `BASE_RING`
- `HIGH_DENSITY_RING`
- `TRANSITION_WALL_RING`
- `OUTER_SPARSE_RING`

这些 key 只负责标识资源，不直接创建 biome 内容。实际 biome 内容由资源 JSON 提供。

### `SaturnRingBiomeSource.java`

自定义 `BiomeSource`，负责根据坐标选择对应 biome。

核心逻辑：

- Minecraft biome noise 坐标会换算为方块坐标：`blockX = x << 2`，`blockZ = z << 2`。
- 使用多层边界扰动调整 Z 轴边界：
  - `ribbonWave = Math.sin(blockX * 0.0005) * 130.0`
  - `macroWarp = boundaryWarpNoise.getValue(blockX * 0.0007, blockZ * 0.0007) * 260.0`
  - `detailWarp = boundaryWarpNoise.getValue(blockX * 0.0020 + 300.0, blockZ * 0.0020 - 300.0) * 60.0`
  - `warpedZ = blockZ + ribbonWave + macroWarp + detailWarp`
- 在若干边界附近生成过渡墙 biome：
  - 边界：`-6000`、`-2000`、`2000`、`6000`、`10000`
  - 判定宽度：距离边界小于等于 `32` 方块时返回 `transition_wall_ring`

Biome 分带：

| warpedZ 范围 | Biome |
| --- | --- |
| `< -10000` | `void_ring` |
| `[-10000, -6000)` | `inner_fading_ring` |
| `[-6000, -2000)` | `inner_sparse_ring` |
| `[-2000, 2000]` | `base_ring` |
| `(2000, 6000]` | `high_density_ring` |
| `(6000, 10000]` | `base_ring` |
| `(10000, 14000]` | `outer_sparse_ring` |
| `> 14000` | `void_ring` |

### `SaturnRingBiomeMetrics.java`

记录每个 biome 对应的地形参数：

- `thickness`：期望环带厚度。
- `weightModifier`：跨 biome 平滑时的权重修正。
- `roughness`：顶部和底部起伏强度。

当前参数：

| Biome | thickness | weightModifier | roughness |
| --- | ---: | ---: | ---: |
| `void_ring` | `0.0` | `1.0` | `0.0` |
| `inner_fading_ring` | `1.5` | `0.7` | `0.75` |
| `inner_sparse_ring` | `3.0` | `1.0` | `1.5` |
| `base_ring` | `7.0` | `1.0` | `2.5` |
| `high_density_ring` | `12.0` | `1.4` | `5.0` |
| `transition_wall_ring` | `28.0` | `0.0` | `0.0` |
| `outer_sparse_ring` | `2.0` | `0.8` | `1.0` |

`transition_wall_ring` 的厚度参数已统一到 `SaturnRingBiomeMetrics`，`SaturnRingTerrainProfile` 会从 metrics 读取 `28.0`，不再保留 generator 内独立的厚度常量。

当前该厚度只作为小行星带垂直扫描范围使用，不再代表整墙式实体地形。

### `SaturnRingTerrainSampler.java`

负责在某个 X/Z 坐标周围采样多个 biome，并计算平滑后的地形厚度和粗糙度。

当前采样设置：

- 采样半径：`RADIUS = 5`
- 采样表尺寸：`11 x 11`
- 权重表使用近似高斯衰减：
  - `weight = exp(-distanceSq / (maxDistanceSq * 0.4f))`
- 周围采样步长：
  - noise 坐标偏移为 `xOffset * 6`、`zOffset * 6`
  - 对应大约 `24` 方块步长

返回值：

- `SampledTerrain.thickness()`
- `SampledTerrain.roughness()`

### `SaturnRingTerrainProfile.java`

统一描述单个 X/Z 列的理论地形形体。

当前职责：

- 记录当前列的 `biomeHolder`。
- 记录当前列的 `expectedThickness`。
- 记录当前列的 `topY`。
- 记录当前列的 `bottomY`。
- 提供 `hasTerrain()` 判断该列是否应有实体地形。
- 提供 `containsY(int y)` 判断某个 Y 是否处于理论地形范围内。
- 使用 `thicknessNoise` 对非过渡墙区域进行厚度扰动。

当前厚度扰动强度：

| Biome | 厚度扰动系数 |
| --- | ---: |
| `inner_fading_ring` | `0.55` |
| `inner_sparse_ring` | `0.40` |
| `outer_sparse_ring` | `0.40` |
| `high_density_ring` | `0.30` |
| 其他实体 biome | `0.25` |

当前调用方：

- `SaturnRingChunkGenerator.fillFromNoise`
- `SaturnRingChunkGenerator.getBaseHeight`
- `SaturnRingChunkGenerator.getBaseColumn`

这样实际生成、基础高度查询和垂直列查询使用同一套顶部/底部计算，避免三套逻辑继续分裂。

### `SaturnRingAsteroidField.java`

负责 `transition_wall_ring` 的小行星带实体判定。

当前职责：

- 将世界坐标切分为确定性的 3D cell。
- 每个 cell 基于坐标 hash 决定是否生成候选小行星。
- 小行星使用椭球体形状。
- 每个椭球体有独立中心点和 X/Y/Z 半径。
- 使用 `asteroidShapeNoise` 对边缘进行削切，避免完全规则的椭球。
- 使用 `isInsideTransitionBridge(...)` 在 `RING_Y_CENTER` 附近生成破碎桥接层，让相邻环带和小行星带之间更平滑。
- 只返回“某个方块坐标是否位于小行星内部”，不直接放置方块。

当前实现参数：

- 水平 cell 尺寸：`32`
- 垂直 cell 尺寸：`32`
- 小行星 X/Z 半径：约 `3.5~12.0`
- 小行星 Y 半径：约 `2.5~9.5`
- 每个 cell 约 `45%` 概率生成候选小行星。
- 破碎桥接层范围：`RING_Y_CENTER ± 12`

### `SaturnRingBlocks.java`

集中管理土星环生成用方块。

当前方块：

| 用途 | BlockState |
| --- | --- |
| 基础环表面 | `saturn_sandstone` |
| 基础环次表面 | `saturn_stone` |
| 基础环核心 | `saturn_stone` |
| 高密度区表面 | `saturn_cobbled_deepslate` |
| 高密度区核心 | `saturn_deepslate` |
| 过渡墙表面 | `saturn_cobbled_deepslate` |
| 过渡墙核心 | `saturn_cobblestone` |
| 稀疏区表面 | `porous_saturn_stone` |
| 稀疏区核心 | `saturn_stone` |
| 消散区表面 | `porous_saturn_stone` |
| 消散区核心 | `porous_saturn_stone` |

当前已切换为自定义方块调色板，并支持材质噪声参数：

- `getSurfaceState(Holder<Biome> biomeHolder)`
- `getSurfaceState(Holder<Biome> biomeHolder, double materialNoise)`
- `getCoreState(Holder<Biome> biomeHolder)`
- `getCoreState(Holder<Biome> biomeHolder, double materialNoise)`

第一版材质噪声用于在同一 biome 内切换少量相邻材质，避免整片区域只有单一纯色方块。

当前土星方块贴图采样色：

| 方块 | 平均色 | MapColor |
| --- | --- | --- |
| `saturn_stone` | `#C88E67` | `MapColor.COLOR_ORANGE` |
| `saturn_cobblestone` | `#C78A5D` | `MapColor.COLOR_ORANGE` |
| `saturn_deepslate` | `#A87757` | `MapColor.DIRT` |
| `saturn_cobbled_deepslate` | `#AD8162` | `MapColor.DIRT` |
| `saturn_sandstone` | `#C29064` | `MapColor.COLOR_ORANGE` |
| `porous_saturn_stone` | `#A2A8A5` | 仍继承 `Blocks.TUFF`，等待正式贴图 |

`SBSDBlocks` 中除 `porous_saturn_stone` 外的土星方块已显式设置 `mapColor(...)`，避免继续继承原版石头、砂岩和深板岩的地图色。

### `SaturnRingChunkGenerator.java`

实际负责 chunk 填充、孔洞生成、表面替换和高度查询。

核心设置：

- 环带中心高度：`RING_Y_CENTER = 96`
- 生成深度：`getGenDepth() = 192`
- 最低 Y：`getMinY() = 0`
- 海平面：`getSeaLevel() = 0`
- 不应用 vanilla carver。
- 不通过 `spawnOriginalMobs` 生成原版生物。

使用多个固定种子的噪声：

- `porosityNoise`
  - 种子：`114514L`
  - 用途：孔洞、裂隙、小行星碎片化。
- `heightNoise`
  - 种子：`223344L`
  - 用途：顶部和底部起伏。
- `thicknessNoise`
  - 种子：`880301L`
  - 用途：同一 biome 内的环带厚度变化。
- `ridgeNoise`
  - 种子：`550771L`
  - 用途：高密度区长条裂谷。
- `materialNoise`
  - 种子：`910021L`
  - 用途：核心和表面方块材质斑块。
- `asteroidShapeNoise`
  - 种子：`730021L`
  - 用途：小行星边缘削切、过渡桥接层破碎判定。

注意：当前噪声种子写死，因此不同世界 seed 下土星环地形形态会保持一致。

#### `fillFromNoise`

逐列填充 chunk 中的方块。

流程：

1. 遍历 chunk 内 `16 x 16` 的 X/Z 列。
2. 获取当前列 biome。
3. 如果是 `transition_wall_ring`：
   - 使用 `SaturnRingTerrainProfile` 提供的垂直扫描范围。
   - 实体方块由 `SaturnRingAsteroidField.isInsideAsteroid(...)` 判断。
   - 不再生成整片厚墙。
4. 否则调用 `SaturnRingTerrainSampler.sampleTerrain(...)` 获取厚度和粗糙度。
5. 使用 `heightNoise` 计算顶部和底部偏移。
6. 根据 `RING_Y_CENTER`、厚度和偏移计算 `topY`、`bottomY`。
7. 在 `bottomY..topY` 范围内按 biome 选择核心方块。
8. 非过渡墙 biome 调用 `isPorous(...)` 判断是否被挖空。
9. 过渡墙 biome 调用小行星场判断是否存在实体。

核心方块选择：

| Biome | 核心方块 |
| --- | --- |
| `high_density_ring` | `saturn_deepslate` / `saturn_cobbled_deepslate` |
| `transition_wall_ring` | `saturn_cobblestone` / `saturn_cobbled_deepslate` |
| `inner_sparse_ring` | `saturn_stone` / `porous_saturn_stone` |
| `outer_sparse_ring` | `saturn_stone` / `porous_saturn_stone` |
| `inner_fading_ring` | `porous_saturn_stone` / 少量 `saturn_stone` |
| 其他 | `saturn_stone` / 少量 `saturn_sandstone` 或 `porous_saturn_stone` |

#### `buildSurface`

在 `fillFromNoise` 之后对每个 X/Z 列最高方块进行表面替换。

表面替换完成后，会调用 `decorateChunk(...)` 生成第一版近景地物装饰。装饰逻辑在表面替换之后执行，避免岩刺、碎石和发光点被表面替换覆盖。

表面方块选择：

| Biome | 表面方块 |
| --- | --- |
| `high_density_ring` | `saturn_cobbled_deepslate` |
| `transition_wall_ring` | `saturn_cobbled_deepslate` / `saturn_cobblestone` |
| `inner_sparse_ring` | `porous_saturn_stone` / `saturn_stone` |
| `outer_sparse_ring` | `porous_saturn_stone` / `saturn_stone` |
| `void_ring` | 空气 |
| `inner_fading_ring` | `porous_saturn_stone` / `saturn_stone` |
| 其他 | `saturn_sandstone` / `saturn_stone` |

#### `decorateChunk`

第一版近景地物装饰，直接在 `SaturnRingChunkGenerator` 内执行，暂不注册正式 Feature。

当前装饰类型：

- 表面碎石：
  - 适用于非虚空区域。
  - 低概率在表面上方放置 1 个方块。
  - 偶尔向旁边扩展 1 个碎石块。
- 短岩刺：
  - 高密度区概率更高。
  - 稀疏区有中等概率。
  - 消散区和过渡区概率较低。
  - 高度约 `2~4` 格。
- 发光占位点：
  - 当前使用 `Blocks.SEA_LANTERN` 作为临时占位。
  - 只在 `high_density_ring` 和 `transition_wall_ring` 极低概率生成。
  - 后续可替换为自定义发光矿物或晶体方块。
- 漂浮碎块：
  - 适用于 `transition_wall_ring`、内外稀疏区和消散区。
  - 小型 3D 碎块，半径约 `1~2`。
  - 过渡区生成在自身扫描范围内。
  - 稀疏/消散区生成在环带顶面上方。

#### `isPorous`

根据 biome 和 3D simplex noise 决定某个方块是否被挖空。

当前规则：

- `inner_fading_ring`
  - 越靠近 `Z = -10000` 越容易变空。
  - 使用 `fadeRatio = (abs(z) - 6000.0) / 4000.0`。
  - 阈值从 `-0.8` 逐渐过渡到 `0.8`。
- `inner_sparse_ring`
  - 小型孔洞。
  - 噪声采样：`x * 0.05, y * 0.1, z * 0.05`
  - `noiseVal < -0.4` 时挖空。
- `high_density_ring`
  - 少量深裂缝。
  - 噪声采样：`x * 0.01, y * 0.01, z * 0.05`
  - `crackNoise > 0.6` 时挖空。
- `transition_wall_ring`
  - 不再使用 `isPorous` 的整墙挖空逻辑。
  - 实体由 `SaturnRingAsteroidField` 判断。
  - 视觉目标从“厚墙噪声碎片”改为“可穿行小行星带”。
- 其他 biome 默认不额外挖空。

#### `getBaseHeight`

用于结构、寻路等系统查询地形高度。

当前逻辑：

- 使用 `SaturnRingTerrainProfile` 计算当前列理论顶部和底部。
- 如果当前列没有地形，返回 `getMinY()`。
- 从理论顶部向下扫描到理论底部。
- 使用与 `fillFromNoise` 一致的孔洞判定筛掉空气。
- 返回最高的实际实体方块 Y。
- 如果整列都被孔洞逻辑挖空，返回 `getMinY()`。

#### `getBaseColumn`

用于返回指定 X/Z 的完整垂直方块列。

当前实现：

- 使用 `SaturnRingTerrainProfile` 计算当前列理论顶部和底部。
- 对每个 Y 使用与 `fillFromNoise` 一致的孔洞判定。
- 实体位置填入 `SaturnRingBlocks.getCoreState(profile.biomeHolder())`。
- 空气位置填入 `Blocks.AIR`。
- 当前已与 `fillFromNoise` 共用理论形体、孔洞判定和核心材质选择。

## 资源文件现状

当前 worldgen 相关资源文件：

- `src/main/resources/data/slashblade_sendims/dimension/saturn_ring.json`
- `src/main/resources/data/slashblade_sendims/dimension_type/saturn_ring_type.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/base_ring.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/high_density_ring.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/inner_fading_ring.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/inner_sparse_ring.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/outer_sparse_ring.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/transition_wall_ring.json`
- `src/main/resources/data/slashblade_sendims/worldgen/biome/void_ring.json`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_sky.png`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_stars.png`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_nebula_1.png`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_nebula_2.png`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_planet.png`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_ring_sharp.png`
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_horizon_dust.png`

7 个 biome JSON 当前内容基本一致：

- 均无降水。
- 降雨量均为 `0.0`。
- `spawners` 为空。
- `spawn_costs` 为空。
- `carvers` 为空。
- `features` 为空。
- 已区分 temperature、fog、sky、water、particle 和 mood sound。

当前 biome 氛围：

| Biome | 氛围方向 | 粒子 |
| --- | --- | --- |
| `void_ring` | 极暗暖褐深空，最低粒子密度 | `minecraft:ash`，`0.001` |
| `inner_fading_ring` | 多孔灰石与暖岩混合的消散感 | `minecraft:ash`，`0.003` |
| `inner_sparse_ring` | 灰多孔石与暖橙岩混合的碎石区 | `minecraft:white_ash`，`0.0025` |
| `base_ring` | 暖砂岩/土星岩主环 | `minecraft:white_ash`，`0.0015` |
| `high_density_ring` | 暗暖褐深板岩，高压迫感 | `minecraft:ash`，`0.004` |
| `transition_wall_ring` | 暖褐圆石与深板岩小行星带 | `minecraft:ash`，`0.007` |
| `outer_sparse_ring` | 稀薄灰暖外环 | `minecraft:white_ash`，`0.002` |

Biome 颜色已根据土星方块贴图采样色重新计算，并使用暗化后的暖岩色作为 `fog_color`、`sky_color`、`water_color` 和 `water_fog_color`，避免环境色继续停留在旧的冷灰/暗蓝灰方案。

当前所有 biome 都使用 `minecraft:ambient.cave` 作为 mood sound，占位提供低频环境压迫感。后续可替换为专用太空/尘埃环境音。

## 其他相关代码

### `HorizontalWorldBorderRenderer.java`

文件位置：

- `src/main/java/com/tonywww/slashblade_sendims/client/renderer/HorizontalWorldBorderRenderer.java`

该类会根据 `SBSDValues.HEIGHT_BOARDER_Y` 渲染水平世界边界。

当前检查结果：

- `SBSDValues.HEIGHT_BOARDER_Y` 只登记了 `Level.OVERWORLD.location()` 对应 `280`。
- 暂未接入 `slashblade_sendims:saturn_ring`。

### `SaturnRingSkyRenderer.java`

文件位置：

- `src/main/java/com/tonywww/slashblade_sendims/client/renderer/SaturnRingSkyRenderer.java`

该类负责 `slashblade_sendims:saturn_ring` 维度的第一版天空盒渲染。

当前实现：

- 订阅 Forge 客户端 `RenderLevelStageEvent`。
- 在 `RenderLevelStageEvent.Stage.AFTER_SKY` 阶段执行，确保只覆盖天空层，不参与世界生成逻辑。
- 只在当前维度 ID 等于 `slashblade_sendims:saturn_ring` 时渲染。
- 参考 EdenRing 的环境渲染结构，已从单张天空盒升级为多层天空系统。
- 当前层级包括：深空背景、星点、两层星云、地平线尘雾、后景软环、远景行星、前景锐环。
- 天空层直接使用 `event.getPoseStack()`，与 EdenRing 使用 `WorldRenderContext.matrixStack()` 的思路一致；不再额外把相机旋转乘入 `RenderSystem.getModelViewStack()`，避免远景行星和环带的视角移动幅度被放大。
- 环带角度已对齐 EdenRing 的表现：后景环和前景环都位于 `Z=-100` 的远景平面，尺寸为 `130`，X 轴只使用基于玩家 Y 坐标的 `[-0.03, 0.03]` 弧度级小偏移，不再使用固定 `74` 度大倾角。
- 当前环带只引用 `saturn_ring_sharp.png`。此前后景层引用缺失的 `saturn_ring_soft.png`，会导致 Minecraft 显示紫黑 missing texture；现已改为同一张半透明 ring 贴图渲染前后两层，使用不同 alpha 区分层次。
- 当前所有新增天空资源仍为占位贴图，但已经拆分为可独立替换的正式资源路径。
- 后续可以继续加入类似 EdenRing `CloudRenderer` / `WeatherRenderer` 的近场 sprite grid，用于空间尘埃、远处碎屑和闪电/能量脉冲。

### `NoiseBasedChunkGeneratorMixin.java`

文件位置：

- `src/main/java/com/tonywww/slashblade_sendims/mixin/minecraft/NoiseBasedChunkGeneratorMixin.java`

该 Mixin 修改 vanilla `NoiseBasedChunkGenerator.createFluidPicker`。

当前判断：

- 它作用于 `NoiseBasedChunkGenerator`。
- `SaturnRingChunkGenerator` 继承的是 `ChunkGenerator`，不是 `NoiseBasedChunkGenerator`。
- 因此它不直接影响当前土星环自定义生成器。

## 当前已完成内容

- 已建立 `saturn_ring` 自定义维度的数据资源。
- 已建立自定义 `BiomeSource` codec，并能从维度 JSON 读取 7 个 biome holder。
- 已建立自定义 `ChunkGenerator` codec，并能从维度 JSON 读取 biome source。
- 已通过 Forge `DeferredRegister` 注册 biome source 和 chunk generator codec。
- 已实现按 Z 轴分带的土星环 biome 分布。
- 已实现 X 轴低频扰动，让环带边界不完全笔直。
- 已实现边界附近的 `transition_wall_ring` 小行星带 biome。
- 已实现按 biome 平滑采样地形厚度和粗糙度。
- 已实现以 Y=96 为中心的悬浮环带方块填充。
- 已实现不同 biome 下的核心方块选择。
- 已实现顶面表面方块替换。
- 已实现稀疏区、消散区、高密度区和过渡墙的孔洞/裂隙/碎片化逻辑。
- 已实现基础高度查询 `getBaseHeight`。
- 已实现基础垂直列查询 `getBaseColumn`，并已让它与实际生成共用理论形体、孔洞判定和核心材质选择。
- 已新增第一批土星环专用基础方块，并完成注册、方块物品、纯色贴图、模型、掉落、挖掘标签、语言和创造栏入口。
- 已将土星环生成材质切换到第一批自定义方块调色板，当前地形形体和噪声逻辑保持不变。
- 已新增 `SaturnRingTerrainProfile`，用于统一每个 X/Z 列的 biome、厚度、顶部和底部计算。
- 已将土星环维度高度提升到 `192`，环带中心高度调整为 `Y=96`，并重调基础厚度和粗糙度参数。
- 已统一 `transition_wall_ring` 厚度参数来源，当前标准值为 `28.0`。
- 已新增第一版土星环天空盒渲染器，并接入纯色占位自定义天空贴图。
- 已加入多层噪声第一版：
  - `SaturnRingBiomeSource` 使用 `boundaryWarpNoise` 增强边界宏观扰动。
  - `SaturnRingTerrainProfile` 使用 `thicknessNoise` 增加厚度变化。
  - `SaturnRingChunkGenerator` 使用 `ridgeNoise` 增加高密度区长条裂谷。
  - `SaturnRingChunkGenerator` 和 `SaturnRingBlocks` 使用 `materialNoise` 增加材质斑块。
- 已新增 `SaturnRingAsteroidField`，并将 `transition_wall_ring` 从整墙式挖空改为确定性椭球小行星带。
- 已将过渡区域垂直扫描厚度从 `120.0` 收紧到 `56.0`，后续又按整体减半方案降到 `28.0`。
- 已加入中心破碎桥接层，缓解普通环带和小行星带之间的突兀断裂。
- 已加入第一版近景地物装饰：
  - 表面碎石。
  - 短岩刺。
  - 漂浮碎块。
  - 少量发光占位点。
- 已区分 7 个 biome JSON 的温度、雾色、天空色、水色、粒子和 mood sound。

## 当前缺口和风险

### 代码维护性

- 多个 Java 文件中的中文注释出现乱码。
- 逻辑仍可阅读，但长期维护成本较高。
- 后续建议统一改成正常 UTF-8 中文注释或英文注释。

### 维度进入方式

- 当前未发现进入 `slashblade_sendims:saturn_ring` 的传送门、指令封装、物品交互或事件逻辑。
- 维度资源存在，但玩家正常流程可能无法进入。

### Biome 内容

- 7 个 biome JSON 目前几乎完全一致。
- 暂无生物生成。
- 暂无 feature。
- 暂无结构。
- 暂无矿物、植被、流体、装饰物。

### 世界种子

- `porosityNoise`、`heightNoise`、`thicknessNoise`、`ridgeNoise`、`materialNoise`、`asteroidShapeNoise`、`boundaryWarpNoise` 使用固定种子。
- 这会导致所有世界中的土星环地形形态一致。
- 如果期望不同世界有不同土星环，应改为从世界 seed 或 `RandomState` 派生。

### 地形查询一致性

- `getBaseColumn` 已与 `fillFromNoise` 共用 `SaturnRingTerrainProfile`、孔洞判定和核心材质选择。
- `getBaseHeight` 已改为返回最高实际实体方块，而不是理论顶部。
- 后续如果新增小行星带、地物或多层材质，需要继续保持这些查询方法与实际生成同步。

### 过渡墙参数不一致

- 已解决。
- 当前 `transition_wall_ring.thickness = 28.0`，并由 `SaturnRingTerrainProfile` 从 `SaturnRingBiomeMetrics` 读取。
- 小行星带阶段已替换整墙式生成。
- 当前 `28.0` 作为小行星带垂直扫描范围，而不是实体墙厚度。

### Y 边界安全

- 当前环带中心为 `96`，维度高度为 `0..191`。
- 过渡墙 `28` 厚时理论范围为 `82..110`，在范围内。
- 当前高密度区厚度 `12`、roughness `5`，理论主体仍远离上下边界。
- 后续如果继续增加 roughness、加入小行星或修改中心高度，需要继续检查上下边界。

## 后续建议方向

优先级建议：

1. 修复 worldgen 包内乱码注释，降低维护成本。
2. 明确 `saturn_ring` 的进入方式。
3. 统一 `transition_wall_ring` 厚度参数来源。
4. 让 `getBaseColumn` 与 `fillFromNoise` 的材质和孔洞逻辑保持一致。
5. 决定噪声是否应跟随世界 seed。
6. 为 7 个 biome 分别补充不同视觉效果、怪物生成、feature 或结构。
7. 如需正式玩法化，补充维度内资源、挑战、奖励和返回机制。

## 土星环视觉优化计划

本节记录 2026-06-20 确定的土星环视觉优化方向。目标是把 `saturn_ring` 从当前较单一的悬浮石质地形，优化成具有明确远景、分区材质、碎石带、地物装饰和太空氛围的可探索维度。

### 总体目标

- 远景具备太空感：深空、星点、巨型行星、远处环带尘埃。
- 中景具备环带结构：连续但破碎，有厚薄变化、裂谷、空洞和小行星带。
- 近景具备可探索细节：不同 biome 有不同方块材质、表面碎屑、晶体、岩刺、漂浮岩块。
- 分区具备辨识度：玩家沿 Z 轴穿过不同 ring biome 时，应能明显感受到地形、材质、雾效和装饰差异。

### 新增基础方块方案

为避免长期依赖原版砂岩、深板岩、末地石、凝灰岩等方块，计划新增一组土星环专用基础方块，用于地形主体、表层、碎石、地物和后续结构。

第一批建议新增方块：

| 方块 ID 建议 | 中文名 | 用途 |
| --- | --- | --- |
| `saturn_stone` | 土星岩 | 基础环和稀疏区的通用岩体 |
| `saturn_cobblestone` | 土星岩圆石 | 表层破碎块、碎石地物、小行星外壳 |
| `saturn_deepslate` | 土星深板岩 | 高密度区主体岩体 |
| `saturn_cobbled_deepslate` | 土星深板岩圆石 | 高密度区裂缝边缘、碎裂表层 |
| `saturn_sandstone` | 土星砂岩 | 基础环表层、浅色沉积层 |
| `porous_saturn_stone` | 土星孔洞岩 | 消散区、稀疏区、洞穴边缘、风化表层 |

第一版贴图策略：

- 可以先使用纯色或极简色块贴图，优先跑通注册、模型、掉落、创造栏和 worldgen 替换流程。
- 每个方块先使用 `block/cube_all` 模型。
- 对应 item model 使用常规 `item/generated` 或 block item 默认模型。
- loot table 第一版直接掉落自身。
- 后续视觉稳定后，再把纯色贴图替换为正式纹理。

建议色彩方向：

| 方块 | 纯色贴图方向 |
| --- | --- |
| `saturn_stone` | 冷灰偏浅，作为主环基础色 |
| `saturn_cobblestone` | 比 `saturn_stone` 略暗，强调破碎表面 |
| `saturn_deepslate` | 深蓝灰或深紫灰，用于高密度区 |
| `saturn_cobbled_deepslate` | 更暗、更粗糙的深灰色 |
| `saturn_sandstone` | 低饱和灰黄色或灰白色，不要过度接近原版砂岩 |
| `porous_saturn_stone` | 浅灰带暗孔洞感；第一版可用纯色，后续补孔洞纹理 |

实现文件预期：

- 注册类：
  - 可新增 `SBSDBlocks.java`，集中注册 Block。
  - 可新增 `SBSDBlockItems.java` 或在现有 `SBSDItems` 中注册 BlockItem。
- 资源：
  - `assets/slashblade_sendims/blockstates/*.json`
  - `assets/slashblade_sendims/models/block/*.json`
  - `assets/slashblade_sendims/models/item/*.json`
  - `assets/slashblade_sendims/textures/block/*.png`
  - `data/slashblade_sendims/loot_tables/blocks/*.json`
- 语言：
  - `assets/slashblade_sendims/lang/zh_cn.json`
  - `assets/slashblade_sendims/lang/en_us.json`

接入 worldgen 后，`SaturnRingBlocks` 应从直接引用原版方块，改为优先引用这些自定义方块。原版方块只作为临时 fallback 或少量混合材料。

### 噪声优化

当前噪声层次较少，建议拆成多种职责明确的噪声：

- `macroWarpNoise`
  - 控制环带边界大尺度弯曲。
  - 与当前 X 轴正弦扰动叠加，避免边界过于规则。
- `thicknessNoise`
  - 控制同一 biome 内的厚薄变化。
  - 让基础环、高密度区和稀疏区不再是固定厚度。
- `ridgeNoise`
  - 生成长条裂谷、脊线和沟壑。
  - 重点服务 `high_density_ring`。
- `porosityNoise`
  - 保留局部孔洞、风化洞、小裂隙职责。
  - 不再单独主导整体形体。
- `materialNoise`
  - 控制同一 biome 内的材质斑块。
  - 用于在土星岩、土星砂岩、土星孔洞岩等自定义方块之间切换。

建议新增 `SaturnRingTerrainProfile` 或类似结构，将某列地形的 biome、厚度、顶部高度、底部高度、roughness、材质噪声值、孔洞判定等集中计算。`fillFromNoise`、`getBaseHeight`、`getBaseColumn` 应共用同一套 profile，避免查询结果和实际生成结果继续分裂。

### 地形形体优化

建议把维度高度从 `128` 提升到 `192`：

- `dimension_type.height = 192`
- `logical_height = 192`
- `SaturnRingChunkGenerator.getGenDepth() = 192`
- `RING_Y_CENTER = 96`

这样上方和下方空域更充足，环带悬浮感会更强，也给天空盒和远景渲染留下空间。

建议厚度目标：

| Biome | 当前厚度 | 优化目标 |
| --- | ---: | --- |
| `void_ring` | `0` | 保持虚空 |
| `inner_fading_ring` | `1.5` | 强消散薄层 |
| `inner_sparse_ring` | `3.0` | 破碎薄层 |
| `base_ring` | `7.0` | 主环薄层 |
| `high_density_ring` | `12.0` | 较厚但不再臃肿的裂谷区 |
| `transition_wall_ring` | `28.0` 扫描范围 | 已改为小行星群，不再整墙填充 |
| `outer_sparse_ring` | `2.0` | 稀疏外环 |

### 过渡墙改造

`transition_wall_ring` 已从接近整层厚度的高频噪声墙改为小行星带：

- 不再生成一整片厚墙。
- 垂直扫描范围已收紧为 `28`，约 `Y=82..110`。
- 使用确定性 3D cell 生成 asteroid center。
- 每个小行星用椭球体生成，半径约 `3~12`。
- 使用噪声削边，使边缘不规则。
- 在 `RING_Y_CENTER ± 12` 内生成破碎桥接层，作为普通环带和小行星带之间的局部过渡。
- 小行星材质通过现有 transition 调色板和 material noise 混合：
  - `saturn_cobblestone`
  - `saturn_cobbled_deepslate`
  - `saturn_deepslate`
- 小行星之间保留大量空隙，让玩家能穿行。

### 方块材质调色板

优化后每个 biome 应有自己的自定义方块调色板。

| Biome | 主体材质 | 表层材质 | 特殊材质 |
| --- | --- | --- | --- |
| `base_ring` | `saturn_stone` | `saturn_sandstone` | 少量 `porous_saturn_stone` |
| `high_density_ring` | `saturn_deepslate` | `saturn_cobbled_deepslate` | 裂隙处少量深色或发光材料 |
| `inner_sparse_ring` | `saturn_stone` | `porous_saturn_stone` | 少量 `saturn_cobblestone` |
| `inner_fading_ring` | `porous_saturn_stone` | `saturn_stone` | 大量空洞和孤立碎块 |
| `transition_wall_ring` | `saturn_cobblestone` | `saturn_cobbled_deepslate` | 小行星内部混入 `saturn_deepslate` |
| `outer_sparse_ring` | `saturn_stone` | `porous_saturn_stone` | 稀疏碎块 |
| `void_ring` | 不生成主体地形 | 不生成主体地形 | 可保留极少远景碎片 |

`SaturnRingBlocks` 应重构为方法式选择：

- `selectCoreBlock(biome, x, y, z, profile)`
- `selectSurfaceBlock(biome, x, y, z, profile)`
- `selectAsteroidBlock(x, y, z, asteroidProfile)`

表层不应只替换最高一层，建议形成 `1~3` 层 crust，以免破洞或侧面暴露时过于单调。

### 地物与装饰

第一阶段先在 `SaturnRingChunkGenerator` 内做轻量装饰，避免过早引入完整 Feature 注册复杂度。

建议地物：

- 表面碎石斑块。
- 小型岩刺。
- 漂浮碎块。
- 少量发光晶体或发光矿点。
- 高密度区裂缝边缘暗色岩块。
- 稀疏区薄片状残骸。

后续如果视觉稳定，再升级为正式 worldgen feature：

- `RingBoulderFeature`
- `RingCrystalFeature`
- `RingDebrisFeature`
- `RingRiftFeature`

并通过 `configured_feature`、`placed_feature` 和 biome JSON 的 `features` 挂载。

### 天空盒与环境

当前使用 `minecraft:the_end` 维度效果，土星环辨识度不足。已决定参考 EdenRing 的环境系统，把土星环天空从单张天空盒升级为多层远景渲染。

参考对象：

- `EdenSkyRenderer`：背景色、星空、星云、太阳、行星、环带、地平线雾。
- `EdenCloudRenderer` / `SpriteGrid`：基于 chunk 的近场 sprite 云雾。
- `EdenWeatherRenderer`：额外天气/闪电层。

当前适配策略：

- 不直接移植 Fabric `DimensionRenderingRegistry`，当前 Forge 项目继续使用 `RenderLevelStageEvent.Stage.AFTER_SKY`。
- 优先完成 `SaturnRingSkyRenderer` 的多层天空主视觉。
- 近场 sprite grid 和天气层放到后续阶段，避免一次性引入过多状态缓存和渲染排序复杂度。

天空内容：

- 深空背景。
- 稀疏星点。
- 巨型行星远景。
- 半透明远处环带。
- 空间尘埃或星云雾。

资源路径：

- `assets/slashblade_sendims/textures/environment/saturn_planet.png`
- `assets/slashblade_sendims/textures/environment/saturn_ring_sharp.png`
- `assets/slashblade_sendims/textures/environment/saturn_stars.png`
- `assets/slashblade_sendims/textures/environment/saturn_nebula_1.png`
- `assets/slashblade_sendims/textures/environment/saturn_nebula_2.png`
- `assets/slashblade_sendims/textures/environment/saturn_horizon_dust.png`
- `assets/slashblade_sendims/textures/environment/saturn_sky.png`

第一版实施结果：

- 已新增 `client/renderer/SaturnRingSkyRenderer.java`。
- 已使用 `RenderLevelStageEvent.Stage.AFTER_SKY` 绘制土星环维度专用天空盒。
- 已新增 `assets/slashblade_sendims/textures/environment/saturn_sky.png`。
- 当前 `saturn_sky.png` 是纯色占位贴图，满足“先使用一张自定义贴图，后续替换正式图”的方案。
- 第一版先把同一张贴图铺到远处天空立方体六个面，保持实现简单、资源替换路径稳定。

后续优化方向：

- 将纯色占位贴图替换为正式星空/行星/环带合成贴图。
- 或拆分为多层资源：深空背景、星点、巨型行星、远处环带、空间尘埃。
- 如需更强方向性，可把单贴图天空盒升级为六面贴图或程序绘制叠加层。

EdenRing 参考后的修订计划：

1. 多层天空主视觉
   - 状态：已完成第一版。
   - 已将 `SaturnRingSkyRenderer` 重构为分层渲染。
   - 渲染顺序为：深空背景 -> 星点 -> 星云 -> 地平线尘雾 -> 后景软环 -> 行星 -> 前景锐环。
   - 所有贴图先使用占位资源，路径保持稳定，便于后续替换正式美术。
   - 已通过 `compileJava` 和 `processResources` 验证。
2. 近场空间尘埃和碎屑 sprite
   - 状态：待办。
   - 参考 EdenRing `SpriteGrid`，按 chunk 确定性生成远处尘埃片、碎屑闪烁和微光。
   - 需要处理透明排序、frustum 裁剪和性能预算。
3. 天气/特殊事件层
   - 状态：待办。
   - 可选实现能量闪光、远处雷暴或环带电弧。
   - 暂不影响基础地形和 biome。

### Biome JSON 氛围调整

7 个 biome JSON 已不再完全一致，已调整：

- `fog_color`
- `sky_color`
- `water_color`
- `water_fog_color`
- `particle`
- `mood_sound`

当前方向：

| Biome | 氛围方向 |
| --- | --- |
| `void_ring` | 极暗，星空清晰，尘埃极少 |
| `inner_fading_ring` | 淡雾，环带逐渐消失 |
| `inner_sparse_ring` | 冷灰雾，碎块可见 |
| `base_ring` | 清晰冷灰，适合作为主探索区 |
| `high_density_ring` | 暗蓝灰或紫灰，压迫感更强 |
| `transition_wall_ring` | 黑灰雾，尘埃粒子更多 |
| `outer_sparse_ring` | 稀薄远环感，亮度略低 |

### 实施阶段

建议按以下顺序推进：

1. 基础方块阶段
   - 状态：已完成第一版。
   - 已新增土星环专用基础方块。
   - 已添加纯色贴图、blockstate、block model、item model、loot table、语言文件。
   - 已注册 Block 和 BlockItem。
   - 已加入创造栏，便于游戏内检查。
2. 结构整理阶段
   - 状态：已完成第一版地形 profile 统一。
   - 已修复 `SaturnRingChunkGenerator` 和 `SaturnRingBlocks` 中的乱码注释。
   - 已新增 `SaturnRingTerrainProfile`。
   - 已统一 `fillFromNoise`、`getBaseHeight`、`getBaseColumn` 的理论地形查询。
   - `getBaseHeight` 和 `getBaseColumn` 已复用与实际生成一致的孔洞判定。
3. 地形形体阶段
   - 状态：已完成第一版高度和基础 metrics 调整。
   - 维度高度已提升到 `192`。
   - `RING_Y_CENTER` 已调整到 `96`。
   - 已调整 biome 固定厚度和粗糙度参数。
   - `transition_wall_ring` 厚度参数已统一。
   - `thicknessNoise`、`ridgeNoise`、`materialNoise` 已在任务 5 中实现第一版。
4. 方块接入阶段
   - 状态：已完成第一版 core/surface 接入。
   - `SaturnRingBlocks` 已改为优先使用自定义方块。
   - 已实现 core/surface 的基础材质选择。
   - 已支持基于 `materialNoise` 的材质斑块选择。
   - crust/asteroid 材质选择将在后续地形 profile 和小行星带阶段继续扩展。
   - 已减少当前土星环主体生成中大面积原版方块。
5. 过渡墙改造阶段
   - 状态：已完成第一版小行星带。
   - 已把 `transition_wall_ring` 从整墙改为小行星带。
   - 已实现确定性椭球小行星。
   - 已使用 `asteroidShapeNoise` 做边缘削切。
   - 已将垂直扫描范围收紧到 `28`。
   - 已加入中心破碎桥接层作为局部连接过渡。
   - 碎石云和更细的局部装饰将在任务 7 中继续扩展。
6. 近景地物阶段
   - 状态：已完成第一版 generator 内轻量装饰。
   - 已在 `buildSurface` 表面替换之后调用 `decorateChunk(...)`。
   - 已加入表面碎石。
   - 已加入短岩刺。
   - 已加入漂浮碎块。
   - 已加入少量发光占位点。
   - 当前发光点使用 `Blocks.SEA_LANTERN` 临时占位，后续可替换为自定义发光方块。
7. 环境氛围阶段
   - 状态：已完成 biome JSON 第一版氛围区分，并完成第一版 EdenRing 式多层天空主视觉。
   - 已修改 biome JSON 的颜色、粒子和 mood sound。
   - 已新增 `SaturnRingSkyRenderer`。
   - 已新增深空背景、星点、星云、地平线尘雾、行星、软环和锐环占位贴图。
   - 行星、环带、星空正式美术仍待后续替换。
   - 近场空间尘埃 sprite grid 和天气/特殊事件层仍待后续实现。

### 验证点

每个阶段至少在以下区域检查：

- `Z = -12000`：虚空区。
- `Z = -8000`：内消散区。
- `Z = -4000`：内稀疏区。
- `Z = 0`：基础环。
- `Z = 4000`：高密度区。
- `Z = 6000 / 10000` 附近：过渡墙/小行星带。
- `Z = 12000`：外稀疏区。

检查项：

- 远景是否有层次。
- 方块是否仍显得单色。
- 地形是否过平、过碎或过噪。
- 小行星带是否能穿行。
- 新方块贴图和模型是否正常。
- `getBaseHeight` 与实际地形是否明显不一致。
- chunk 生成是否明显卡顿。
- 玩家移动和战斗空间是否足够。

## 记录更新规则

后续对话中，如果确定了新的 worldgen 方案，应更新以下部分：

- 新增或删除文件时，更新“资源文件现状”或“Java 文件职责”。
- 修改生成算法时，更新对应类的小节。
- 确定设计决策时，加入“决策记录”。
- 完成待办时，移动或修改“当前缺口和风险”。
- 新发现问题时，追加到“当前缺口和风险”。

## 决策记录

### 2026-06-20

- 确认当前 worldgen 范围主要是 `slashblade_sendims:saturn_ring` 自定义维度。
- 确认需要用本文档持续记录 worldgen 方案、实现现状和后续变更。
- 确认土星环视觉优化方向：先新增一批土星环专用基础方块，并允许第一版使用纯色贴图；后续 worldgen 应优先使用这些自定义方块构建材质调色板。
- 确认视觉优化范围应覆盖噪声、地形形体、方块材质、地物装饰、biome 氛围和天空盒。
- 完成任务 1：新增 `saturn_stone`、`saturn_cobblestone`、`saturn_deepslate`、`saturn_cobbled_deepslate`、`saturn_sandstone`、`porous_saturn_stone` 六个基础方块。第一版贴图为纯色占位，已通过 `compileJava` 和 `processResources` 验证。
- 完成任务 2：`SaturnRingBlocks` 已切换为自定义方块调色板，并提供 `getSurfaceState`、`getCoreState` 两个选择方法；`SaturnRingChunkGenerator` 已使用这些方法生成表面和核心材质，现阶段不改变地形形体、分带和孔洞噪声。
- 完成任务 3：新增 `SaturnRingTerrainProfile`，统一每列 biome、厚度、顶部和底部计算；`fillFromNoise`、`getBaseHeight`、`getBaseColumn` 已共用 profile，且 `getBaseHeight/getBaseColumn` 已复用实际生成的孔洞判定。已通过 `compileJava` 验证。
- 完成任务 4：`saturn_ring_type` 的 `height/logical_height` 已提升到 `192`，`SaturnRingChunkGenerator.RING_Y_CENTER` 已调整为 `96`，`getGenDepth()` 已调整为 `192`。`SaturnRingBiomeMetrics` 已重调厚度和粗糙度，过渡墙厚度统一为 `120.0` 并由 `SaturnRingTerrainProfile` 读取。已通过 `compileJava` 和 `processResources` 验证。
- 完成任务 5：加入多层噪声第一版。`SaturnRingBiomeSource` 已加入 `boundaryWarpNoise`，`SaturnRingTerrainProfile` 已加入 `thicknessNoise`，`SaturnRingChunkGenerator` 已加入 `ridgeNoise` 和 `materialNoise`，`SaturnRingBlocks` 已支持带材质噪声的方块选择。已通过 `compileJava` 验证。
- 完成任务 6：新增 `SaturnRingAsteroidField`，`transition_wall_ring` 已从整墙式高频噪声挖空改为确定性椭球小行星带。`fillFromNoise`、`getBaseHeight`、`getBaseColumn` 继续通过统一的 `isSolidTerrain` 判定保持一致。已通过 `compileJava` 验证。
- 完成任务 6 修订：`transition_wall_ring.thickness` 已从 `120.0` 收紧为 `56.0`，理论范围约 `Y=68..124`；`SaturnRingAsteroidField` 新增 `isInsideTransitionBridge(...)`，在 `RING_Y_CENTER ± 12` 生成破碎桥接层，缓解普通环带和小行星带之间的突兀连接。已通过 `compileJava` 验证。
- 完成任务 7：`SaturnRingChunkGenerator` 已加入第一版 `decorateChunk(...)` 近景装饰，包含表面碎石、短岩刺、漂浮碎块和少量发光占位点。装饰在 `buildSurface` 表面替换之后执行。已通过 `compileJava` 验证。
- 完成任务 8：7 个 biome JSON 已区分 temperature、fog_color、sky_color、water_color、water_fog_color、particle 和 mood_sound。JSON 语法检查通过，已通过 `processResources` 验证。
- 完成任务 9：新增 `SaturnRingSkyRenderer`，在 `slashblade_sendims:saturn_ring` 维度的 `AFTER_SKY` 阶段渲染第一版天空盒；新增 `textures/environment/saturn_sky.png` 作为单张自定义天空贴图，当前为纯色占位。已通过 `compileJava` 和 `processResources` 验证。
- 完成任务 10：参考 EdenRing 的 `EdenSkyRenderer`，将 `SaturnRingSkyRenderer` 从单贴图天空盒升级为多层主天空系统。当前包含深空背景、确定性星点、两层星云、地平线尘雾、后景环、远景行星和前景锐环；新增 `saturn_stars.png`、`saturn_nebula_1.png`、`saturn_nebula_2.png`、`saturn_planet.png`、`saturn_ring_sharp.png`、`saturn_horizon_dust.png` 等占位资源。已通过 `compileJava` 和 `processResources` 验证。
- 完成任务 10 修订：修复天空远景层视角移动幅度过大的问题。`SaturnRingSkyRenderer` 现在参考 EdenRing 的透视处理方式，直接使用 Forge `RenderLevelStageEvent` 传入的 `event.getPoseStack()` 渲染天空层，不再额外叠加相机旋转；背景天空盒也改为显式使用该 pose matrix。已通过 `compileJava` 验证。
- 完成任务 10 环带角度修订：参考 EdenRing `EdenSkyRenderer` 的 ring 渲染逻辑，将土星环天空盒 ring 从固定 `74` 度大倾角改为 `Axis.XP.rotation(angle)` 的弧度级小偏移；`angle = clamp((playerY - 96) * 0.0006, -0.03, 0.03)`，后景环和前景锐环都使用 `Z=-100`、尺寸 `130`。已通过 `compileJava` 验证。
- 完成任务 10 贴图引用修订：修复半透明土星环贴图出现紫黑块的问题。原因是 `SaturnRingSkyRenderer` 后景层仍引用不存在的 `textures/environment/saturn_ring_soft.png`；当前已改为前后两层都绑定存在的 `textures/environment/saturn_ring_sharp.png`，并用不同 alpha 控制层次。已通过 `compileJava` 和 `processResources` 验证。
- 完成任务 11：读取新土星方块贴图主色，并同步方块 map color 与 biome 氛围色。`saturn_stone`、`saturn_cobblestone`、`saturn_sandstone` 使用 `MapColor.COLOR_ORANGE`，`saturn_deepslate` 和 `saturn_cobbled_deepslate` 使用 `MapColor.DIRT`；7 个 biome JSON 的 `fog_color`、`sky_color`、`water_color`、`water_fog_color` 已改为基于新贴图采样的暖岩暗化色。`porous_saturn_stone` 因尚无新贴图，暂时保留 `Blocks.TUFF` 继承色。已通过 `compileJava`、`processResources` 和 JSON 解析验证。
- 完成任务 12：按要求将整个土星环维度的地形厚度降低一半。`SaturnRingBiomeMetrics` 中所有非虚空 biome 的 `thickness` 已乘以 `0.5`，用于上下表面起伏的 `roughness` 也同步乘以 `0.5`，以保证视觉厚度也减半；`transition_wall_ring` 垂直扫描范围从 `56.0` 降到 `28.0`。已通过 `compileJava` 验证。

## 第二层世界树与海（2026-10-04）

- 维度 ID 保持 `sdbf:deep_realm_level_2`，由实例 KubeJS 数据包声明。
- 新生成器与群系源 Codec 均为 `slashblade_sendims:world_tree_sea`。
- 垂直范围 `-64..319`，总高 384；海平面 64；深海海底约 -61..-60。
- 三个活动群系：`sdbf:world_tree_roots`、`sdbf:world_tree_shallows`、`sdbf:world_tree_depths`。
- `WorldTreeField` 是不依赖 Minecraft 的确定性几何场，包含岛岸、主干、树洞、Bezier 根枝和叶簇。
- `WorldTreeSeaBiomeSource` 与 `WorldTreeSeaChunkGenerator` 共用几何场；所有写入限于当前区块。
- 参数与木材、树叶、土壤、晶体材料由维度 JSON 配置，布局种子被序列化，不使用全局可变世界状态。
- 沉没之城在完整拼装后平移所有结构件，将边界底部对齐海底；水生结构在该生成器中检查范围是否有海底高地或树根穿插。紫晶蟹巢穴同样按树根岛的真实地面调整，并避开树干。
- 常规高度图保留真实树冠高度，水面结构 JSON 移除树冠高度投影并采用海面锚点。
- 本次不迁移旧区块；使用新世界验收。

### 验证与实例配置

- `gradlew worldTreeGeometryTest`：9000 个采样柱、负坐标、区块边界候选一致性、缓存重建顺序、Y=319 无截顶、海底 -61/-60、三群系与树冠覆盖检查。
- 初版参数 `layout_seed=20261004, sea_level=64, tree_spacing=360, crown_radius=165`。采样树冠覆盖：树根区 98.9%、浅滩区 88.0%、深水区 14.2%；树叶最高 Y=317，木材最高 Y=301。
- 默认布局种子由维度 JSON 固定并序列化，所有世界使用同一坐标布局；可通过 JSON 改变新世界布局。
- 实例木材/树叶采用 Integrated Dynamics 的 menril_wood / menril_leaves；土壤包括 Botania 蕴魔草地、灰化土、砂土、缠根泥土；深海生成水含碱晶体与海带，浅滩生成海草与睡莲。
- 现有三个船结构与海怪船采用固定水面锚点，移除树冠高度投影；常规高度图仍返回真实最高方块。
- 实例入口脚本经 Java `findArrival` 寻找干燥、平坦、无根干阻挡的岛面；保留目标维度 ID 与原有传送门生成逻辑。
- 实例 `local/world_tree_work/runtime.init.gradle` 提供隔离 Forge 服务器检查：Codec 往返、真实 FULL 区块、群系与高度、材料、现有结构的模板放置和自然生成候选。
- 无头环境不覆盖客户端光影/天空视觉验收；剖面图和覆盖图来自实际几何场采样。

- 每个 2×2 树单元组有 50% 概率保留一个无树的深海盆地（总体约 12.5% 单元），为沉没之城等大型结构保留自然生成空间。碰撞检查按实际结构件边界进行，避免用外接矩形的空白角落误判。

### 世界树形状与海岛修订（2026-10-04）

- 默认树冠半径由 165 改为 289，Codec 接受 120..400；枝条伸展与叶簇横向同步放大。
- 岛体基础半径改为 112..162，拉伸范围 0.70..1.42；多尺度坐标扭曲、湾口/半岛和岸线扰动让树根区成为可以相连的大型不规则海岛。
- 海底下降区由约 20 格增至 88..112 格；根区到浅滩连续插值，保留少量自然坡度变化。
- 树干增加随高度弯曲的中心线、节瘤、根部膨大、螺旋沟槽与相关噪声表面；根枝使用扰动曲线和密集短段，子枝锚定实际父枝曲线。
- 无树海盆集中为 4×4 组内的 2×2 单元海盆（50% 组概率，总占比 12.5%），给更大的岛体间保留海域和大型结构空间。
- 候选树范围随放大的冠幅与岛体范围扩展，生成仍只写当前区块。
- 最新固定种子 9000 柱采样树冠覆盖 99.1% / 96.9% / 74.5%（根区/浅滩/深海）；树叶最高 Y=316；海底 -61/-60。
- 12000 随机坐标斜坡采样：相邻高度落差 p99 由 12 降为 6，最大采样落差由 15 降为 9；1800×1800 地图内最大根区连通岛体估算面积由 25992 增至 357300 平方格。
- 完整 build、纯几何接缝/缓存检查、新隔离 Forge 世界的 25 FULL 区块、三个实际群系、传送落点、六类模板实际放置及六类自然生成起点均通过。
- 维度 ID 保持 sdbf:deep_realm_level_2，不处理旧区块迁移。生成采样图不是客户端截图。

### 性能、叶簇与粗树干修订（2026-10-04）

- 显示名称：深境二层·深水区、深境二层·树区、深境二层·浅滩区。维度与群系 ID 不变；其他翻译条目保持原样。
- 根枝建立 16×16 空间索引，每个方块柱只遍历相关区段；完整树形按需生成并同步发布，群系/海岸高度查询只创建轻量树参数。
- 方块柱缓存上限 16384；树参数/几何缓存上限 512，分批清理。缓存键混合散列，减轻坐标规律造成的冲突。
- 地形按原版噪声生成方式锁定并直接写入区段调色板，完成后统一构建高度图，避免每个方块重复更新高度图。
- 树冠由大小、水平位置、垂直位置不同的叶簇组成；大尺度边缘噪声与 3×2×3 格簇状扰动形成凹凸和缺口。冠幅参数仍为 289。
- 树干基础半径由 19..35 改为 28..44，渐细后的基准半径下限为 14，保留扭转、节瘤和树洞。
- 固定位置 X=6..13、Z=9..16 的 64 个全新连续区块，单次隔离 Forge 测量：完整加载从 17.20s 降至 3.90s，耗时减少 77.3%，约 4.40 倍速度。这不是整个模组包客户端载入时间或 FPS 的保证。
- 纯性能优化阶段的 6291456 个方块采样校验值与优化前完全一致；之后按用户要求独立改变叶形和干径，海岛地面校验值仍一致。
- 最新树冠覆盖采样：树区 97.5%、浅滩区 95.2%、深水区 71.1%；树叶最高 Y=314。
- 9000 柱、接缝、缓存顺序和 4 工作者并发发布检查通过；实际 NOISE 方块/高度图与基准柱一致；25 FULL 区块、传送落点、六类模板实际放置和六类自然生成通过；完整 build 与重混淆通过。
- 当前生产 JAR SHA-256：`606c835d30f0ebb201769307f8ec41312cff896d18e2ff4fe19eafcf0cb7a633`。运行检查器与性能探针未打包。
- 图与日志：`local/world_tree_work/verification/performance-revision/after/`；本轮修改前备份：`backups/world_tree_performance_before/`。采样图为生成器实际几何，不是客户端截图。

### 植被、红树林与海底修订（2026-10-04）

- 维度 ID 仍为 `sdbf:deep_realm_level_2`；海面 64、高度 384、树距 360、冠幅 289、三个群系 ID 与显示名称保持原样。数据包仅新增 `filled_log` 方块状态配置；兼容逻辑仅作用于世界树生成器。
- 地表植被随连续噪声成片变化，包含草、蕨、高草、大型蕨、兰花、铃兰和小型野花。水下增加海草、高海草及 7–28 格海带丛，浅滩增加成片睡莲；碱晶体保留。
- 浅滩偶尔生成红树林，有分叉树干、多个叶团、支撑根、含水树根和悬挂胎生苗。几何按坐标确定，写入裁剪到当前区块，并参与基准方块柱与高度图计算。
- 开阔海底基准抬至 Y=-43，叠加宽缓海脊、次级沙丘和细部起伏；本轮开阔海盆采样范围 Y=-59..-29。浅滩到深海仍连续过渡，相邻柱落差采样 p99=5、最大 7。
- 沉没之城整组按照实际结构片段下方的最高采样海底平移，避免起伏海床掩埋下层房间；仍避开浅水与世界树粗根。手动实际放置顶部 Y=94，海面 Y=64，采样到 842 个建筑方块露出水面；自然起点示例顶部 Y=91。其他维度的沉没之城定位逻辑保持原样。
- 主根起始半径从 11..19 提高为树干基础半径的 0.68 倍再加 0..7 格，接入点更靠近树干内部且提高 5 格。干基膨大由半径×0.22/19 格高度尺度改为半径×0.48/30 格高度尺度，保留树洞与粗糙曲面。
- 配置 `integrateddynamics:menril_log_filled`，在 Y=74..230 的树干暴露表面稀疏出现，填充纹理朝向暴露的一侧；保持纵向木纹。25 个 FULL 区块检出 20 个填充原木，朝向检查通过。
- 实际 25 FULL 区块检出地表植物 1100 个、水生植物 2182 个；可生长地面覆盖为 994/1974（约 50.4%）。单株红树林实测有 56 个木材、111 个树根（其中 28 个含水）、1064 个树叶与 6 个垂挂胎生苗。
- 保留空间索引、按需几何、有限缓存、区段批量写入等性能优化。固定 64 个全新连续区块的隔离 Forge 单次测量：NOISE 2.10s，随后 FULL 2.26s，合计 4.36s；上轮同一探针为 3.90s，增加了植被和树形后本轮约增加 0.45s。不是整个模组包客户端载入时间或 FPS。
- 全新隔离世界：配置 Codec、基准柱/实际 NOISE 方块与高度图、安全传送落点、三群系、植被与含水红树根、蕴门原木、六类模板实际放置及六类自然起点通过；9000 柱、接缝、缓存顺序和 4 工作者并发检查通过；完整 build/重混淆与原有项目检查通过。尚未进行客户端画面与光影验收。
- 已安装 `mods/slashblade_sendims-1.0.22.jar`，SHA-256：`d088e6d82d881251e17fc81548829536c1d84bb89be2f463b6b9a08076654bd5`；开发运行探针未打包。备份：`backups/world_tree_ecology_before/`；本轮报告：`local/world_tree_work/verification/ecology-revision/`。

### 蕴门原木改为主要位于树洞内壁（2026-10-04）

- `integrateddynamics:menril_log_filled` 优先生成在中央树洞的内壁，填充面朝向洞内空气，保持纵向木纹；避免填充面被土壤、粗根、树枝或树叶遮挡。
- 内壁概率 1/24，外壁降为 1/3072，保留少量外壁原木。几何、维度 ID、群系 ID、地形、植被与结构定位不变。
- 全新的隔离 Forge 世界，在两株空心树与两株实心树周围检查 676 个 NOISE 区块：洞内原木 363 个，外壁 30 个，洞内占比 92.37%。实际朝向的相邻方块为空气，木纹轴为 Y；实际方块与 `getBaseColumn` 一致。
- 完整 `gradlew build --offline`、9000 柱几何/接缝/缓存顺序与并发检查及项目原有检查通过。未再次运行无关结构模板检查；本轮没有改变结构和树形几何。
- 已安装 `mods/slashblade_sendims-1.0.22.jar`，SHA-256：`9142bceffe6c7fc513f4da2ecf68ace6022262e59450dea87bc0fdbb0078b1ba`。备份：`backups/world_tree_hollow_resin_before/`；报告：`local/world_tree_work/verification/hollow-resin-revision/`。重启后仅新生成区块使用新分布。

## 六类小型结构与铁板研究室（2026-10-05）

- 维度 ID 仍为 `sdbf:deep_realm_level_2`；原有维度 JSON、三个群系、树形生成源码与蕴门原木概率均保持原样。
- 六类小型结构：树脂采集站、悬枝瞭望亭、根隙圣龛、苇海旧渡口、沉水采样舱、断裂蔓桥。每类三个不同变体，共 18 个 KubeJS 数据包 NBT，每处只有一个战利品箱；圣龛和渡口的部分变体有隐藏研究室。
- 实验室外壳采用 `quark:iron_plate`；保留深色玻璃、铜制采样管和树脂设备。三个采样舱模板的初始密封检查通过；双层舱口需要依照日志排空夹层后再进入。
- 稀疏随机分布由自定义 `slashblade_sendims:world_tree_small` 类型定位：根旁、干湿岸线、真实粗枝或起伏海底。每个候选区块只扫描固定数量位置，柱脚和模板均裁剪到当前区块。生成类型拒绝其他生成器；既有结构兼容 Mixin 没有更改。
- 新箱子移除附魔书，部分主题箱含破碎的耀魂、核电工艺钢锭、Quark 木炭块、逻辑线缆及碱晶体粉末。研究箱附带可阅读日志。神秘碱晶体在样品池中的权重为 1/131，固定 1 枚，样品池只在部分箱子里抽取；20,000 次真实抽取检出 148 枚。自然碱晶体方块掉落表不变。
- 实际隔离 Forge 世界验证：18 模板的方块状态和一箱约束；六类随机分布自然起点与 FULL 区块放置；实际支撑、箱子开箱、保存/重载、维度范围、干燥舱室和战利品检查通过。开发环境 Quark 的 SRG 影子字段与命名映射冲突，使用同 ID 的完整立方体测试占位方块；实际安装的 Quark ID、模型和铁板启用配置另外核对。占位脚本只在独立测试目录内，不进入生产包。
- 完整构建及原有几何/并发/末地等检查通过；只增加结构类及注册表，安装包中没有开发验证器。生产 JAR SHA-256：`235b776656abd0abcf8a354a4bbdc7a54e0551fc871fa1033d28c8dc31a086de`。
- 实际 NBT/模型/纹理预览与剖面：`docs/world_tree_small_structures/index.md`。这是模板静态渲染，尚未进行客户端光影画面验收。
- 本阶段只建造小型结构；三类已批准的拼图地牢留待下一阶段。重启游戏后，新生成区块使用本轮结构。备份：`backups/world_tree_small_structures_before/`；报告：`local/world_tree_work/verification/small_structures/`。

## 三类中大型拼图地牢（2026-10-05）

已建造悬冠聚落、枯心回廊、沉根观测枢纽，分别设 3/4/5 个战利品箱。99 个 NBT 模块通过 36 个原版加权拼图池组装，并检查主线可达、树杈承重、海底起伏、地下覆盖和结构引用范围。隐藏研究室外围为 Quark 铁板，研究主题为们瑞欧树脂采样、析晶与压力试验；核心奖励无附魔书，神秘碱晶体沿用样品池权重 1/131、每次一枚。

独立 Forge 世界的三处自然结构完成 FULL 放置，24 种不同布局、实际门洞/地板/支撑、精确箱数、存档重载和 9,000 次核心奖励抽取通过。开发环境使用 Quark 同 ID 立方体占位，生产使用真实 Quark；预览来自真实 NBT 和当前模型纹理，尚未做客户端画面验收。

维度 ID 仍为 `sdbf:deep_realm_level_2`。41 个受保护的维度/既有小型结构/生成器文件保持原样，生产 JAR 除新结构类和注册表外未改变既有类。构建与既有地形/并发/兼容校验通过。JAR SHA-256：`6d2f46e5d5670f935ad6e9aa08cf4135a78e9cd5be93687980e544f493b59cc2`。

详情与预览：`docs/world_tree_dungeons/index.md`。验证：`local/world_tree_work/verification/dungeons/`。备份：`backups/world_tree_dungeons_before/`。重启后于新生成区块生效。

## 结构装饰、隐藏战斗与入口改造（2026-10-05）

按批准清单应用 22 种装饰方块，排除深暗之园；117 模板中的 64 处标识统一为英文 PneumaticCraft 警句板，研究书籍同步英文。30 个模板刷怪笼加全向遮蔽，普通间隔 160–320 tick、每次尝试 3 只，核心 100–200 tick、每次尝试 4 只，基础生命 36/48，混合守卫装备零掉率。

三类地牢入口改造完成：树冠接枝栈道、根部自适应地面引道和宽折返楼梯、深水登舱平台与铁板楼梯。接道数据可保存重载，放置仍按区块裁剪。维度 ID 仍为 `sdbf:deep_realm_level_2`；18 个受保护文件及世界树生成器保持原样。

已通过实际隔离 Forge 的 24 种布局、9 类自然结构 FULL 放置、真实警句板文本、4 处隐藏刷怪笼实际出怪、楼梯/气闸通路、箱子与战利品检查。Quark 铁板/木炭块及 KubeJS 物品在开发环境使用占位，生产使用真实资源；客户端画面待游戏验收。完整构建和既有几何/并发检查通过。JAR SHA-256：`44635a7f7461958aa830c77f8554c60afc8c16f4fdbfba2efc617207bba4a24e`。本轮备份：`backups/world_tree_refinement_before/`。

详情与入口预览：`docs/world_tree_dungeons/refinement.md`。重启后在新生成区块生效。

## 自然木色与室内装饰更新（2026-10-05）

移除 117 模板中的两种红色活木饰面，用古橡木墙脚、木框和嵌条统一木屋配色；新增地面拼花、低矮地垫、苔痕、壁架、盆栽、墙面藤蔓和研究室管线/格栅/样品台。实验室 Quark 铁板外壳及所有方块实体 NBT 精确保留。

99 地牢模块通路检查、24 种布局、九类自然结构 FULL 放置、四处隐藏刷怪笼实际出怪与真实英文警句板读取均通过。71 个保护文件哈希一致，维度 ID 仍为 `sdbf:deep_realm_level_2`；生成器 JAR、战斗与战利品参数不变。开发测试沿用既有 Quark/KubeJS 局部占位，客户端画面待验收。

详情与 NBT 剖面：`docs/world_tree_dungeons/interiors.md`。备份：`backups/world_tree_interiors_before/`。重启后于新生成区块生效。

## 屋顶、立体布局与水密修复（2026-10-05）

117 模板已更新：双层屋顶和斗拱、紧凑枯心井道、三层拼图主路、上层核心回廊、连续观察站密封框与科技外饰。信息牌 64 → 12，绳圈移除；断桥改用低枝和叶片净空检查。24 布局、六类小型自然生成、真实水流及漏水对照、通路、战斗与完整构建通过。维度 ID 仍为 sdbf:deep_realm_level_2。旧 JAR/模板备份 world_tree_architecture_before；新 JAR SHA-256：67c3b0448c24bdfc8a7bb8ce0330d72bb8250ae59c1348ec553e443432531a35。详情 docs/world_tree_dungeons/architecture.md，重启后新生成区块生效。

## 三层地牢与连续通路（2026-10-05）

三类地牢改为真正上下堆叠的三层拼图布局，每层有独立随机房间与支路；层间高差 24 格。108 地牢模板、45 拼图池。连续踏步和平台、三格拼接口、栅栏连接与支撑柱止于其他楼层承重面。24 布局、真实玩家双向碰撞行走、水密、战斗、六类小型自然结构及完整构建通过。维度仍为 sdbf:deep_realm_level_2，重启后新生成区块生效。备份 world_tree_routes_before，JAR SHA-256：0a4884dbf0f66d3c118c4c53a37809f5063405ff8d432621279ca1d878183466。详情 docs/world_tree_dungeons/routes.md。

## 自然地物（2026-10-05）

新增树上、地面、水面、水下与树根共 15 类自然地物，采用坐标确定方案、局部空间索引、有界缓存和当前区块写入，并避让结构外扩 12 格空间。15 类真实生成、并发一致性、水洼流体、巨型睡莲组合、地牢通路/水密和完整构建通过。维度 ID 与填充原木概率保持原值。详情 docs/world_tree_ecology/index.md；JAR SHA-256：f8a1d2aa05aba386c47de4241c5b5e0cf830e9cde9084108da24e187e7f416e5。

## 区域扩展与渐疏边缘（2026-10-05）

扩大 7 类区域型地物，加入核心到苔藓地毯/稀疏植物的连续边缘，海带高度与密度向外递减；采用区块方块索引及局部结构保护。新旧覆盖对照、15 类真实地物、地牢通路/水密及完整构建通过。JAR SHA-256：4a0ef322b12511cf99988523ef703fbaebe27ddd8cffd2f4e2f016e06f8dd9df。详情 docs/world_tree_ecology/regions.md。
