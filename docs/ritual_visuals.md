# 仪式着色器特效

整合包的传送门仪式、地图传送仪式、结构召唤仪式和基岩破坏者的客户端特效。仪式流程和判定都在整合包的 KubeJS 脚本里（`kubejs/server_scripts/custom_spawn/`、`miscs/bedrock_breaker.js`），本模组只负责画面。设计与验证记录见整合包 `docs/development/仪式着色器特效设计-2026-11-13.md`。

## 服务端接口

`com.tonywww.slashblade_sendims.ritual.RitualVisuals`，给 KubeJS 调用：全部是静态方法，参数只用基本类型和字符串，不重载；每次调用发一个阶段包（`network/RitualVisualPacket`，消息 ID 6，协议号 8），返回 false 表示没有发出。

| 方法 | 时机 | 接收者 |
| --- | --- | --- |
| `charge(level, x, y, z, kind, style, colorA, colorB, ticks, param)` | 蓄能开始；基岩破坏者每一下 | 核心 96 格内 |
| `interrupt(level, x, y, z)` | 蓄能被打断 | 核心 96 格内 |
| `release(level, x, y, z, kind, style, colorA, colorB, travelerId)` | 召唤、发起传送、基岩碎裂 | 核心 96 格内 |
| `warp(player, kind, style, colorA, colorB, ticks)` | 传送前的屏幕旋涡 | 只发给传送者 |
| `arrival(level, x, y, z, kind, style, colorA, colorB, travelerId)` | 抵达点（传送者脚下） | 抵达点 96 格内 |

- `kind`：`summon`、`portal`、`breach`。
- `style`：主题名，对应整合包 `RITUAL_THEMES` 的键（`default`、`aether`、`undergarden`、`tofu`、`deep_realm_1`、`deep_realm_2`、`return`、`map_teleport`、`bedrock`），不认识的按默认样式画。
- `colorA`、`colorB`：主题尘埃色字符串，例如 `'0.35 0.9 0.85'`。
- `param`：地图传送是罗盘方位（度，世界 XZ 平面上 `atan2(dz, dx)`），基岩破坏者是第几下；没有时传 NaN。

## 光影兼容

自写着色器只在世界绘制之外运行，世界里的面片用原版材质：

1. **画布烘焙**（`RenderTickEvent` START，世界绘制之前）：`ritual_circle`、`ritual_runes`、`ritual_beam` 把当前帧的法阵、光幕、光柱和能量流画进离屏画布（`client/ritual/RitualCanvas`）。这时 Iris 没有接管，着色器正常运行。
2. **世界几何**（`RenderLevelStageEvent` 的 `AFTER_PARTICLES`）：画布以 `slashblade_sendims:dynamic/ritual/...` 注册为纹理，用 `RenderType.eyes` 贴到面片上（`client/ritual/RitualMaterials`）。开着光影时由 Iris 换成 `gbuffers_spidereyes`，带上光影的雾和泛光。光影画阴影时跳过（`IrisShadowPass`）。
3. **屏幕效果**（`mixin/minecraft/GameRendererRitualScreenMixin`，`GameRenderer.render` 里 `renderLevel` 返回之后）：`ritual_screen` 处理光影最终合成后的画面。不用 Forge 的 `AFTER_LEVEL`，因为它早于光影的最终合成。

着色器在 `RegisterShadersEvent` 注册（`client/ritual/RitualShaders`）；某个加载失败只关闭对应的层并记日志。烘焙或屏幕效果运行出错时同样只关闭对应部分。

## 客户端

- `client/ritual/RitualVisualClient`：接收阶段包，最多同时 8 个仪式、3 套画布（离镜头近的优先）；收到包时一次性按格采样地面生成贴地面片；48 格外每 3 帧烘焙一次。
- `client/ritual/RitualGeometry`：贴地面片、朝向镜头的光柱、双面圆筒光幕、贝塞尔能量流。
- `client/ritual/RitualScreenEffect`：传送旋涡、抵达闪光、召唤涟漪；用现实时间计时，跨维度加载画面期间保持住，画面关闭后才淡出。

## 配置

本地配置 `config/slashblade_sendims.toml` 的 `[ritualVisuals]`：`enabled`、`screenEffects`、`intensity`、`shaderPackGlow`（开光影时烘焙光晕的倍率，默认 1.0）、`maxDistance`、`canvasResolution`。召唤的通天光柱固定 160 格内可见。
