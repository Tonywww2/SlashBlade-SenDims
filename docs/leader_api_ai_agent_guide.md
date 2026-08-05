# Leader API：AI Agent 集成指南

本文用于指导 AI Agent 在 Minecraft 1.20.1、Forge 47 项目中正确接入 SlashBlade SenDimS 的 Leader API。

面向开发者的语义说明见 `docs/leader_api.md`。当本文与源码不一致时，以 `com.tonywww.slashblade_sendims.api.leader` 包中的公开类型为准。

## Agent 的目标

使用 Leader API 完成以下一种或多种任务：

- 将任意 `LivingEntity` 实例注册为 Leader。
- 为某个 `EntityType` 的后续实例设置默认 Leader 行为。
- 查询实体是否可被招架，或是否已经进入被招架后的破防阶段。
- 由其他 Mod 的攻击、技能或 AI 打开招架窗口并触发招架。
- 监听服务端权威事件或客户端同步事件。

不要复制 Leader 系统的内部实现。不要直接读写 Leader NBT。

## 开始前必须检查

Agent 在修改消费项目之前，必须确认：

1. 项目目标版本为 Minecraft 1.20.1 和 Forge 47.x。
2. 运行环境包含 `slashblade_sendims` 主 Mod；API 当前不单独发布为 API JAR。
3. 消费项目能够在编译期引用包含 `com.tonywww.slashblade_sendims.api.leader` 的 JAR。
4. `mods.toml` 是否需要声明对 `slashblade_sendims` 的强制依赖。
5. 需求发生在逻辑服务端、客户端，还是两端。

所有状态写操作必须运行在逻辑服务端主线程。不要从异步网络回调、`CompletableFuture` 或工作线程直接调用写 API。

如果消费项目将 SenDimS 设为可选依赖，不得直接在无 Mod 环境会加载的类中引用 API 类型。此时应先设计隔离的 compat 类，并用 `ModList.get().isLoaded("slashblade_sendims")` 控制类加载。

## 只允许依赖的包

业务代码应只导入：

```java
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderParryDecision;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.LeaderStateChangeCause;
import com.tonywww.slashblade_sendims.api.leader.ParryResult;
import com.tonywww.slashblade_sendims.api.leader.event.ClientLeaderStateChangedEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParryAbsorbedEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParryAttemptEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParriedEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderStateChangedEvent;
```

禁止依赖以下实现细节：

- `com.tonywww.slashblade_sendims.leader.*`
- `SBSDLeader`
- `LeaderManager`
- `LeaderStateStorage`
- `SBSDValues` 中的 Leader NBT 字段
- `entity.getPersistentData()` 中以 `sbsd.` 或 `apoth.boss` 开头的 Leader 数据

直接调用内部类会绕过兼容边界，并可能使事件、客户端同步或未来版本失效。

## 精确理解三个阶段

`LeaderPhase` 的值互斥：

| 阶段 | 正确含义 | 常用判断 |
| --- | --- | --- |
| `NORMAL` | 无招架窗口，且未处于破防状态 | 检查快照的 `phase()` |
| `PARRYABLE` | 实体当前可以被招架 | `LeaderApi.isParryable(entity)` |
| `PARRIED` | 招架已经成功，实体处于破防状态 | `LeaderApi.isParried(entity)` |

不要生成 `isParrying`、`isBeingParried` 等自定义别名。当前 API 不记录“实体正在执行招架动作”这一独立概念。

不要把 `PARRYABLE` 解释为“已经被招架”。

## 选择配置档

在写代码前，Agent 必须先选择 `LeaderProfile`：

| 需求 | 使用配置档 |
| --- | --- |
| SenDimS 接管生命初始化、队伍、随机特殊攻击、自动招架窗口和恢复 | `MANAGED` |
| 其他 Mod 的 AI、技能或事件决定何时打开招架窗口 | `EXTERNAL` |
| 只想让某个实体能够被外部系统招架 | `EXTERNAL` |
| 无法确定 | 默认 `EXTERNAL`，并在实现说明中记录假设 |

不得对 `MANAGED` Leader 调用 `openParryWindow` 或 `closeParryWindow`。这些方法会返回 `false`，因为 `MANAGED` 的窗口由内部时序控制。

## Agent 决策流程

```text
需求是否要注册 Leader？
  ├─ 已有实体实例 → 在逻辑服务端调用 registerLeader
  └─ 某类型未来生成的全部实例 → common setup 中调用 registerLeaderType

窗口由谁控制？
  ├─ SenDimS → MANAGED
  └─ 消费项目 → EXTERNAL

是否要改变状态？
    ├─ 是 → 只在逻辑服务端主线程调用 open/close/tryParry/enterParriedState
  └─ 否 → 两端都可调用查询 API

是否只需在状态变化时处理？
  ├─ 服务端玩法 → LeaderStateChangedEvent 或 LeaderParriedEvent
  ├─ 客户端显示 → ClientLeaderStateChangedEvent
  └─ 每帧渲染 → 查询客户端只读快照
```

## 标准任务模板

### 1. 注册一个已经生成的实体

必须在逻辑服务端执行：

```java
public static boolean makeExternalLeader(LivingEntity entity) {
    if (entity.level().isClientSide) {
        return false;
    }
    return LeaderApi.registerLeader(entity, LeaderProfile.EXTERNAL);
}
```

`registerLeader(entity)` 等价于使用 `LeaderProfile.EXTERNAL`。

重复注册相同配置是幂等操作。若实体已有不同的显式配置，方法返回 `false`，Agent 不应通过写 NBT 强制覆盖。

### 2. 注册某种实体的后续实例

在 Forge common setup 的 `enqueueWork` 中注册：

```java
private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        boolean registered = LeaderApi.registerLeaderType(
                ModEntities.EXAMPLE_BOSS.get(),
                LeaderProfile.EXTERNAL
        );
        if (!registered) {
            LOGGER.warn("A conflicting Leader profile is already registered for example_boss");
        }
    });
}
```

类型规则只作用于以后加入世界的实例。已经存在的实体需要单独调用 `registerLeader`。

同一 `EntityType` 的首个不同配置获胜。不要依赖 Mod 加载顺序反复覆盖配置。

### 3. 打开和关闭 EXTERNAL 招架窗口

定时窗口：

```java
if (!boss.level().isClientSide) {
    boolean opened = LeaderApi.openParryWindow(boss, 20);
    if (!opened) {
        LOGGER.debug("Leader window was not opened for {}", boss.getUUID());
    }
}
```

无限期窗口：

```java
LeaderApi.openParryWindow(boss);
```

关闭窗口：

```java
LeaderApi.closeParryWindow(boss);
```

`durationTicks` 必须大于零，否则抛出 `IllegalArgumentException`。

不要每 tick 重复打开同一个窗口。应在 AI 状态、技能阶段或动画关键帧真正变化时调用。

### 4. 查询状态

简单布尔查询：

```java
if (LeaderApi.isParryable(target)) {
    // The target can be parried now.
}

if (LeaderApi.isParried(target)) {
    // The target is in the post-parry vulnerable phase.
}
```

需要配置档、阶段或剩余时间时读取快照：

```java
Optional<LeaderSnapshot> optionalSnapshot = LeaderApi.getSnapshot(target);
if (optionalSnapshot.isEmpty()) {
    return;
}

LeaderSnapshot snapshot = optionalSnapshot.get();
LeaderProfile profile = snapshot.profile();
LeaderPhase phase = snapshot.phase();
OptionalInt remainingTicks = snapshot.remainingTicks();
```

`Optional.empty()` 表示实体不是 Leader。

`remainingTicks().isEmpty()` 表示当前阶段没有已知截止时间，例如无限期 EXTERNAL 窗口。它不表示零 tick，也不表示查询失败。

客户端查询是服务器同步的只读快照。在玩家开始追踪实体之前，`isLeader` 可能暂时返回 `false`；不得用客户端查询结果执行权威伤害或招架判定。

### 5. 从其他战斗系统触发招架

只在逻辑服务端调用：

```java
ResourceLocation sourceId = ResourceLocation.fromNamespaceAndPath(
        "examplemod",
        "shield_parry"
);

ParryResult result = LeaderApi.tryParry(target, attacker, sourceId);
switch (result) {
    case SUCCESS -> onParrySucceeded(attacker, target);
    case ABSORBED -> onParryAbsorbed(attacker, target);
    case NOT_LEADER -> LOGGER.debug("Target is not a Leader");
    case NOT_PARRYABLE -> LOGGER.debug("Leader parry window is closed");
    case WRONG_SIDE -> LOGGER.warn("tryParry was called on the logical client");
}
```

`actor` 允许为 `null`，但只应在没有合理触发实体时使用。`sourceId` 不允许为 `null`，应稳定标识招架来源，而不是每次动态生成。

`result.isAccepted()` 对 `SUCCESS` 和 `ABSORBED` 都返回 `true`。`SUCCESS` 后 API 已经完成以下操作：

1. 将目标切换为 `PARRIED`。
2. 关闭当前窗口并重置相关计时。
3. 应用标准目标反应。
4. 同步客户端。
5. 发布服务端事件。

Agent 不得在 `SUCCESS` 后再次写状态、重复眩晕目标或手动发送 Leader 同步包。消费项目可以在成功后添加自己的声音、粒子、资源消耗或奖励。

### 5.1 在提交前吸收招架或覆盖时长

需要护盾层吸收招架时，监听 `LeaderParryAttemptEvent`：

```java
@SubscribeEvent
public static void onLeaderParryAttempt(LeaderParryAttemptEvent event) {
    if (!isOwnedTarget(event.getTarget())) {
        return;
    }
    if (hasBarrier(event.getTarget())) {
        consumeBarrier(event.getTarget());
        event.setDecision(LeaderParryDecision.ABSORB);
    } else {
        event.setParriedTicks(100);
        event.setStunTicks(100);
    }
}
```

该事件只在服务端校验通过、目标仍为 `PARRYABLE` 时发布。它不可取消；决议默认为 `PARRY`。`parriedTicks` 和 `stunTicks` 必须大于零。多个监听器修改同一字段时最后一次写入生效。

监听器不得在 attempt 事件中对同一目标重入 `tryParry`、`enterParriedState`、`openParryWindow` 或 `closeParryWindow`。API 会拒绝这些重入，但业务代码仍应避免依赖拒绝行为。

`ABSORB` 会关闭窗口、回到 `NORMAL` 并发布 `LeaderParryAbsorbedEvent`；不会应用 stun、破防或易伤。SlashBlade 标准反击和治疗仍执行一次。

### 5.2 从外部机制直接触发破防

不经过招架窗口的护盾破裂使用：

```java
ParryResult result = LeaderApi.enterParriedState(
        target,
        actor,
        ResourceLocation.fromNamespaceAndPath("examplemod", "barrier_break"),
        100,
        100
);
```

该方法不发布 attempt 事件。目标必须存活、未移除、已注册且当前不是 `PARRIED`。成功后由 API 负责关窗、PARRIED、stun、同步和标准结果事件；消费项目不得重复这些动作。该调用不会自动发放 SlashBlade 玩家奖励。

### 6. 监听服务端状态变化

```java
@Mod.EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class LeaderEvents {
    private LeaderEvents() {
    }

    @SubscribeEvent
    public static void onLeaderStateChanged(LeaderStateChangedEvent event) {
        if (event.getCause() == LeaderStateChangeCause.PARRIED) {
            LivingEntity target = event.getEntity();
            LeaderSnapshot snapshot = event.getNewSnapshot();
        }
    }

    @SubscribeEvent
    public static void onLeaderParried(LeaderParriedEvent event) {
        LivingEntity target = event.getTarget();
        LivingEntity actor = event.getActor(); // Nullable.
        ResourceLocation sourceId = event.getSourceId();
    }

    @SubscribeEvent
    public static void onLeaderParryAbsorbed(LeaderParryAbsorbedEvent event) {
        LivingEntity target = event.getTarget();
        ResourceLocation sourceId = event.getSourceId();
    }
}
```

事件发布在 `MinecraftForge.EVENT_BUS`，不是 Mod event bus。只有 `LeaderParryAttemptEvent` 可修改决议和时长；结果事件只读且不可取消。

`LeaderStateChangedEvent` 的 cause 包含：

| Cause | 含义 |
| --- | --- |
| `WINDOW_OPENED` | 窗口被打开，或窗口期限发生有效变化 |
| `WINDOW_CLOSED` | 招架窗口关闭 |
| `PARRIED` | 招架成功并进入破防阶段 |
| `RECOVERED` | 从破防阶段恢复到正常阶段 |
| `PARRY_ABSORBED` | 招架被外部机制吸收并回到正常阶段 |

一次成功招架的服务端顺序是：attempt、状态提交、目标反应、客户端同步、`LeaderStateChangedEvent`、`LeaderParriedEvent`。吸收顺序是：attempt、NORMAL 提交、客户端同步、`LeaderStateChangedEvent`、`LeaderParryAbsorbedEvent`。SlashBlade 奖励在结果事件之后执行。

需要知道招架者和来源时监听 `LeaderParriedEvent`。只关心阶段变化时监听 `LeaderStateChangedEvent`。不要同时在两个事件中发放同一奖励。

### 7. 监听客户端同步

客户端视觉逻辑使用 `ClientLeaderStateChangedEvent`：

```java
@Mod.EventBusSubscriber(
        modid = ExampleMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientLeaderEvents {
    private ClientLeaderEvents() {
    }

    @SubscribeEvent
    public static void onLeaderStateChanged(ClientLeaderStateChangedEvent event) {
        LeaderSnapshot snapshot = event.getNewSnapshot();
        if (snapshot.phase() == LeaderPhase.PARRYABLE) {
            // Start HUD, sound, or render feedback.
        }
    }
}
```

该监听器必须限制为 `Dist.CLIENT`。不要让包含 `net.minecraft.client` 引用的类在 dedicated server 加载。

`getOldSnapshot()` 可能为空，表示客户端首次收到该实体的 Leader 快照。

客户端事件只用于 HUD、渲染和音效，不能决定伤害、资源消耗或是否招架成功。

## 返回值处理规则

Agent 不得忽略会表达失败的返回值：

| 调用 | `false` 或失败值的常见原因 |
| --- | --- |
| `registerLeader` | 在客户端调用，或已有冲突的显式 profile |
| `registerLeaderType` | 同一类型已有不同 profile |
| `openParryWindow` | 非 Leader、非 EXTERNAL、客户端调用，或当前已 PARRIED |
| `closeParryWindow` | 非 Leader、非 EXTERNAL、客户端调用，或窗口未开启 |
| `tryParry` | `ABSORBED` 表示已接受但未破防；失败值为 `NOT_LEADER`、`NOT_PARRYABLE` 或 `WRONG_SIDE` |
| `enterParriedState` | 与 `tryParry` 相同的失败值；不要求窗口，但已 PARRIED 或目标无效会返回 `NOT_PARRYABLE` |

失败通常不是异常。Agent 应根据业务需要记录日志、跳过动作或回退，而不是直接修改 NBT 绕过失败。

## 禁止模式

生成代码时禁止：

```java
// Wrong: private persistent format.
entity.getPersistentData().putBoolean("sbsd.isparried", true);

// Wrong: internal compatibility facade.
SBSDLeader.setParried(entity.getPersistentData(), true);

// Wrong: client-authoritative gameplay decision.
if (Minecraft.getInstance().level != null && LeaderApi.isParryable(target)) {
    damageTarget(target);
}

// Wrong: MANAGED windows cannot be externally controlled.
LeaderApi.registerLeader(entity, LeaderProfile.MANAGED);
LeaderApi.openParryWindow(entity, 20);

// Wrong: this is not a Mod event bus event.
modEventBus.addListener(this::onLeaderParried);
```

还应避免：

- 通过反射访问 `LeaderManager` 或 `LeaderStateStorage`。
- 缓存 `LeaderSnapshot` 并长期当作权威状态使用。
- 每 tick 调用 `tryParry`。
- 在 `LeaderParriedEvent` 中再次调用 `tryParry`。
- 在 `LeaderParryAttemptEvent` 中对同一目标重入任何 Leader 写操作。
- 假设 `event.getActor()` 永不为 `null`。
- 把 `remainingTicks().isEmpty()` 当作状态已结束。
- 为了改变 profile 而删除或重写实体持久化数据。

## Agent 实施步骤

处理真实代码库任务时按以下顺序执行：

1. 搜索消费项目现有 Forge setup、事件订阅和实体注册模式。
2. 确认 SenDimS 是强制依赖还是可选依赖。
3. 判断需要实例注册还是类型注册。
4. 明确选择 `MANAGED` 或 `EXTERNAL`，不要同时使用两套控制方式。
5. 把所有写操作放在逻辑服务端路径。
6. 只从公开 API 包导入类型。
7. 对失败返回值和 `ParryResult` 做显式处理。
8. 客户端代码放入 `Dist.CLIENT` 隔离类。
9. 编译消费项目。
10. 用服务端和客户端场景验证状态转换与事件次数。

## 最小验收矩阵

Agent 完成集成后至少验证：

| 场景 | 期望结果 |
| --- | --- |
| 查询普通实体 | `isLeader == false`，快照为空 |
| 服务端注册 EXTERNAL 实体 | `isLeader == true`，phase 为 `NORMAL` |
| 客户端直接注册实体 | 返回 `false`，不改变服务端状态 |
| 打开 20 tick 窗口 | phase 变为 `PARRYABLE`，发布一次状态事件 |
| 窗口内调用 `tryParry` | 返回 `SUCCESS`，phase 变为 `PARRIED` |
| attempt 将决议设为 ABSORB | 返回 `ABSORBED`，phase 回到 `NORMAL`，不 stun |
| attempt 覆盖为 100/100 tick | PARRIED 与 stun 均精确持续 100 tick |
| 从 NORMAL 调用 `enterParriedState` | 返回 `SUCCESS`，无需窗口进入 PARRIED |
| 窗口外调用 `tryParry` | 返回 `NOT_PARRYABLE` |
| 对普通实体调用 `tryParry` | 返回 `NOT_LEADER` |
| MANAGED 实体调用 `openParryWindow` | 返回 `false` |
| 玩家开始追踪 Leader | 客户端最终能读取同步快照 |
| dedicated server 加载 | 不发生客户端类加载错误 |

## Agent 完成前自检

提交代码前回答以下问题；任一答案为“否”都应继续修正：

- 是否只依赖 `api.leader` 和 `api.leader.event`？
- 是否没有直接读写 Leader NBT？
- 是否明确选择且只使用一个 profile？
- 是否保证所有状态写操作在逻辑服务端？
- 是否处理了 API 的布尔返回值和 `ParryResult`？
- 是否显式处理了 `ABSORBED`，并避免在 attempt 事件中重入？
- 是否区分 `PARRYABLE` 与 `PARRIED`？
- 是否把客户端监听器限制到 `Dist.CLIENT`？
- 是否避免在两个服务端事件中重复发放奖励？
- 是否完成编译以及至少一个成功和一个失败路径测试？

满足以上条件后，才应将 Leader API 集成标记为完成。