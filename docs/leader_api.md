# Leader API

Leader API 位于主 Mod JAR 的 `com.tonywww.slashblade_sendims.api.leader` 包中。当前不单独发布 API JAR。

需要让 AI Agent 实施接入时，请提供 [Leader API：AI Agent 集成指南](leader_api_ai_agent_guide.md)。

所有注册、窗口控制和招架写操作必须在逻辑服务端主线程调用。客户端仅可查询同步快照。

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

只有逻辑服务端、存活且未移除的已注册 Leader、当前为 `PARRYABLE` 时才会接受招架。返回值包括：

- `SUCCESS`：招架成功，目标进入 `PARRIED`。
- `ABSORBED`：招架已被目标的外部机制吸收，目标回到 `NORMAL`。
- `NOT_LEADER`：目标没有注册为 Leader。
- `NOT_PARRYABLE`：窗口未开启、目标已经破防、目标无效，或同一目标正在提交另一次招架。
- `WRONG_SIDE`：在逻辑客户端调用。

`ParryResult.isAccepted()` 对 `SUCCESS` 和 `ABSORBED` 都返回 `true`。消费方使用 `switch` 时必须显式处理新增的 `ABSORBED`。

`SUCCESS` 后 API 会：

1. 将目标切换为 `PARRIED` 并关闭窗口。
2. 应用标准 stun；Naga 使用其原有 daze/charging 处理。
3. 同步客户端。
4. 发布状态变化事件和招架成功事件。

### 吸收招架与覆盖时长

`LeaderParryAttemptEvent` 在校验通过、状态提交前发布。它不可取消，但监听器可以：

```java
@SubscribeEvent
public static void onParryAttempt(LeaderParryAttemptEvent event) {
    if (shouldAbsorb(event.getTarget())) {
        event.setDecision(LeaderParryDecision.ABSORB);
        return;
    }
    event.setParriedTicks(100);
    event.setStunTicks(100);
}
```

`parriedTicks` 和 `stunTicks` 必须大于零。最后一个写入值生效；监听器不得在该事件中对同一目标再次调用 `tryParry`、窗口控制或强制破防。

吸收会关闭当前窗口、清除当前动作计时并返回 `NORMAL`，但不会应用 stun、`PARRIED` 或易伤。SlashBlade 自身把 `SUCCESS` 和 `ABSORBED` 都视为有效招架，因此两者都会执行一次标准反击和治疗奖励。

### 无窗口强制破防

盾牌格挡、护盾层归零等外部机制可以直接提交标准破防：

```java
ParryResult result = LeaderApi.enterParriedState(
        target,
        attacker,
        ResourceLocation.fromNamespaceAndPath("examplemod", "barrier_break"),
        100,
        100
);
```

该调用不要求招架窗口，也不会发布 `LeaderParryAttemptEvent`。目标必须是逻辑服务端上存活、未移除、已注册且尚未 `PARRIED` 的 Leader。成功后仍发布标准状态变化与 `LeaderParriedEvent`，但 SenDims 不会因为这个 API 调用自动发放 SlashBlade 玩家奖励。

## Forge 事件

事件发布在 `MinecraftForge.EVENT_BUS`：

- `LeaderParryAttemptEvent`：可修改但不可取消的预提交事件；包含旧快照、决议和破防/stun 时长。
- `LeaderStateChangedEvent`：服务端阶段或窗口期限真正变化后发布，包含旧/新快照和 `LeaderStateChangeCause`。
- `LeaderParriedEvent`：服务端招架成功后发布，包含目标、可空的触发者、来源 ID 和最终快照。
- `LeaderParryAbsorbedEvent`：服务端吸收招架后发布，最终快照阶段为 `NORMAL`。
- `ClientLeaderStateChangedEvent`：客户端应用同步快照后发布；旧快照可能为空。

```java
@SubscribeEvent
public static void onLeaderParried(LeaderParriedEvent event) {
    LivingEntity target = event.getTarget();
    ResourceLocation source = event.getSourceId();
}
```

正常招架顺序为：`LeaderParryAttemptEvent`、提交状态、应用目标反应、发送同步、`LeaderStateChangedEvent(PARRIED)`、`LeaderParriedEvent`。吸收顺序为：`LeaderParryAttemptEvent`、提交 `NORMAL`、发送同步、`LeaderStateChangedEvent(PARRY_ABSORBED)`、`LeaderParryAbsorbedEvent`。SlashBlade 自身的玩家治疗和反击奖励在结果事件之后执行。

## 兼容性

新破防使用绝对游戏时间保存截止点，`remainingTicks()`、服务端恢复和客户端倒计时使用同一时钟。旧存档中没有截止点的 `PARRIED` 状态继续使用原 action tick 兼容路径。

新枚举值均追加在末尾，旧 ordinal 不变。尽管如此，旧源码中的穷尽 `switch` 在升级后必须增加 `ABSORBED` 或 `default` 分支。旧代码可以继续读取原 NBT 字段，但新集成应只依赖 `LeaderApi` 和 `api.leader.event`，不要直接写实体 NBT，也不要调用 `SBSDLeader` 的原始 `CompoundTag` setter。