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
| `inner_fading_ring` | `3.0` | `0.7` | `1.5` |
| `inner_sparse_ring` | `6.0` | `1.0` | `3.0` |
| `base_ring` | `14.0` | `1.0` | `5.0` |
| `high_density_ring` | `24.0` | `1.4` | `10.0` |
| `transition_wall_ring` | `56.0` | `0.0` | `0.0` |
| `outer_sparse_ring` | `4.0` | `0.8` | `2.0` |

`transition_wall_ring` 的厚度参数已统一到 `SaturnRingBiomeMetrics`，`SaturnRingTerrainProfile` 会从 metrics 读取 `56.0`，不再保留 generator 内独立的厚度常量。

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
- `src/main/resources/assets/slashblade_sendims/textures/environment/saturn_ring_soft.png`
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
| `void_ring` | 极暗深空，最低粒子密度 | `minecraft:ash`，`0.001` |
| `inner_fading_ring` | 淡暗冷灰，环带消散感 | `minecraft:ash`，`0.003` |
| `inner_sparse_ring` | 冷灰碎石区 | `minecraft:white_ash`，`0.0025` |
| `base_ring` | 较清晰的冷灰主环 | `minecraft:white_ash`，`0.0015` |
| `high_density_ring` | 暗蓝灰，高压迫感 | `minecraft:ash`，`0.004` |
| `transition_wall_ring` | 黑灰尘埃小行星带 | `minecraft:ash`，`0.007` |
| `outer_sparse_ring` | 稀薄暗灰外环 | `minecraft:white_ash`，`0.002` |

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
- 环带角度已对齐 EdenRing 的表现：后景软环和前景锐环都位于 `Z=-100` 的远景平面，尺寸为 `130`，X 轴只使用基于玩家 Y 坐标的 `[-0.03, 0.03]` 弧度级小偏移，不再使用固定 `74` 度大倾角。
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
- 已统一 `transition_wall_ring` 厚度参数来源，当前标准值为 `56.0`。
- 已新增第一版土星环天空盒渲染器，并接入纯色占位自定义天空贴图。
- 已加入多层噪声第一版：
  - `SaturnRingBiomeSource` 使用 `boundaryWarpNoise` 增强边界宏观扰动。
  - `SaturnRingTerrainProfile` 使用 `thicknessNoise` 增加厚度变化。
  - `SaturnRingChunkGenerator` 使用 `ridgeNoise` 增加高密度区长条裂谷。
  - `SaturnRingChunkGenerator` 和 `SaturnRingBlocks` 使用 `materialNoise` 增加材质斑块。
- 已新增 `SaturnRingAsteroidField`，并将 `transition_wall_ring` 从整墙式挖空改为确定性椭球小行星带。
- 已将过渡区域垂直扫描厚度从 `120.0` 收紧到 `56.0`。
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
- 当前 `transition_wall_ring.thickness = 56.0`，并由 `SaturnRingTerrainProfile` 从 `SaturnRingBiomeMetrics` 读取。
- 小行星带阶段已替换整墙式生成。
- 当前 `56.0` 作为小行星带垂直扫描范围，而不是实体墙厚度。

### Y 边界安全

- 当前环带中心为 `96`，维度高度为 `0..191`。
- 过渡墙 `56` 厚时理论范围为 `68..124`，在范围内。
- 当前高密度区厚度 `24`、roughness `10`，理论主体仍远离上下边界。
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
| `inner_fading_ring` | `2` | `1~5`，强消散 |
| `inner_sparse_ring` | `4` | `3~8`，破碎薄层 |
| `base_ring` | `10` | `8~16`，主环层 |
| `high_density_ring` | `16` | `16~30`，厚重裂谷区 |
| `transition_wall_ring` | `56` 扫描范围 | 已改为小行星群，不再整墙填充 |
| `outer_sparse_ring` | `2` | `2~6`，稀疏外环 |

### 过渡墙改造

`transition_wall_ring` 已从接近整层厚度的高频噪声墙改为小行星带：

- 不再生成一整片厚墙。
- 垂直扫描范围已收紧为 `56`，约 `Y=68..124`。
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
- `assets/slashblade_sendims/textures/environment/saturn_ring_soft.png`
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
   - 已将垂直扫描范围收紧到 `56`。
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
- 完成任务 10：参考 EdenRing 的 `EdenSkyRenderer`，将 `SaturnRingSkyRenderer` 从单贴图天空盒升级为多层主天空系统。当前包含深空背景、确定性星点、两层星云、地平线尘雾、后景软环、远景行星和前景锐环；新增 `saturn_stars.png`、`saturn_nebula_1.png`、`saturn_nebula_2.png`、`saturn_planet.png`、`saturn_ring_soft.png`、`saturn_ring_sharp.png`、`saturn_horizon_dust.png` 等占位资源。已通过 `compileJava` 和 `processResources` 验证。
- 完成任务 10 修订：修复天空远景层视角移动幅度过大的问题。`SaturnRingSkyRenderer` 现在参考 EdenRing 的透视处理方式，直接使用 Forge `RenderLevelStageEvent` 传入的 `event.getPoseStack()` 渲染天空层，不再额外叠加相机旋转；背景天空盒也改为显式使用该 pose matrix。已通过 `compileJava` 验证。
- 完成任务 10 环带角度修订：参考 EdenRing `EdenSkyRenderer` 的 ring 渲染逻辑，将土星环天空盒 ring 从固定 `74` 度大倾角改为 `Axis.XP.rotation(angle)` 的弧度级小偏移；`angle = clamp((playerY - 96) * 0.0006, -0.03, 0.03)`，后景软环和前景锐环都使用 `Z=-100`、尺寸 `130`。已通过 `compileJava` 验证。
