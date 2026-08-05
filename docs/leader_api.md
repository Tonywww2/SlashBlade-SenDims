# Leader API

Leader API 位于主 Mod JAR 的 `com.tonywww.slashblade_sendims.api.leader` 包中。当前不单独发布 API JAR。

需要让 AI Agent 实施接入时，请提供 [Leader API：AI Agent 集成指南](leader_api_ai_agent_guide.md)。

## 状态语义

Leader 的阶段互斥：

| 阶段 | 含义 |
| --- | --- |
| `NORMAL` | 当前没有招架窗口，也没有处于破防状态。 |
| `PARRYABLE` | 招架窗口开启，实体当前可以被招架。 |
| `PARRIED` | 招架已经成功，实体处于破防/易伤阶段。 |

请使用 `isParryable` 表达“当前能否被招架”，使用 `isParried` 表达“当前是否已经被招架”。API 没有把 `isParrying` 作为别名，因为系统并不记录实体自身执行招架动作的状态。

## 注册

注册单个已经生成的实体必须在逻辑服务端执行。默认使用 `EXTERNAL`：

```java
LeaderApi.registerLeader(mob);
```

也可以显式选择配置档：

```java
LeaderApi.registerLeader(mob, LeaderProfile.MANAGED);
```

- `MANAGED`：SenDims 负责生命初始化、队伍、随机特殊攻击、招架窗口和恢复流程。
- `EXTERNAL`：其他项目控制招架窗口；SenDims 仍负责招架成功、破防计时、伤害倍率、事件与同步。

在 common setup 的 `enqueueWork` 中注册实体类型后，该规则会应用于以后加入世界的实例：

```java
event.enqueueWork(() -> LeaderApi.registerLeaderType(
        ModEntities.EXAMPLE_BOSS.get(),
        LeaderProfile.EXTERNAL
));
```

单实体显式配置优先于类型默认配置。重复相同注册会成功；尝试用不同配置覆盖已有显式配置会返回 `false`。当前版本不提供注销 API，因为 `MANAGED` 可能已经添加永久属性修饰符和 scoreboard team。

## 控制与查询

`EXTERNAL` Leader 可以使用定时或无限期窗口：

```java
LeaderApi.openParryWindow(boss, 20);
LeaderApi.openParryWindow(boss);
LeaderApi.closeParryWindow(boss);
```

这些写操作只在逻辑服务端有效，对 `MANAGED` Leader 返回 `false`。定时窗口的持续时间必须大于零。

服务端返回权威状态；客户端返回服务器同步的只读快照：

```java
boolean canBeParried = LeaderApi.isParryable(entity);
boolean isVulnerable = LeaderApi.isParried(entity);

LeaderApi.getSnapshot(entity).ifPresent(snapshot -> {
    LeaderPhase phase = snapshot.phase();
    OptionalInt remaining = snapshot.remainingTicks();
});
```

玩家开始追踪实体以及状态真正变化时会发送快照。客户端倒计时在本地递减，不会每 tick 收包。实体尚未生成时到达的快照会按 UUID 暂存，并在实体加入客户端世界时应用。

## 触发招架

其他战斗系统可以绕过 SlashBlade combo 判定，提交一次标准招架：

```java
ParryResult result = LeaderApi.tryParry(
        target,
        attacker,
        ResourceLocation.fromNamespaceAndPath("examplemod", "shield_parry")
);
```

只有逻辑服务端、已注册 Leader 且当前为 `PARRYABLE` 时返回 `SUCCESS`。成功后 API 会：

1. 将目标切换为 `PARRIED` 并关闭窗口。
2. 应用标准 stun；Naga 使用其原有 daze/charging 处理。
3. 同步客户端。
4. 发布状态变化事件和招架成功事件。

## Forge 事件

事件发布在 `MinecraftForge.EVENT_BUS`，均为只读且不可取消：

- `LeaderStateChangedEvent`：服务端阶段或窗口期限真正变化后发布，包含旧/新快照和 `LeaderStateChangeCause`。
- `LeaderParriedEvent`：服务端招架成功后发布，包含目标、可空的触发者、来源 ID 和最终快照。
- `ClientLeaderStateChangedEvent`：客户端应用同步快照后发布；旧快照可能为空。

```java
@SubscribeEvent
public static void onLeaderParried(LeaderParriedEvent event) {
    LivingEntity target = event.getTarget();
    ResourceLocation source = event.getSourceId();
}
```

服务端成功招架的顺序为：提交状态、应用目标反应、发送同步、发布 `LeaderStateChangedEvent`、发布 `LeaderParriedEvent`。SlashBlade 自身的玩家治疗和反击奖励在这些事件之后执行。

## 兼容性

旧 NBT 键和现有 Minoshroom、KnightPhantom、AlphaYeti、Naga 的数值与时序继续保留。旧代码可以继续读取原字段，但新集成应只依赖 `LeaderApi` 和 `api.leader.event`，不要直接写实体 NBT，也不要调用 `SBSDLeader` 的原始 `CompoundTag` setter。