# TheTidengFrastMod_ChainMining 更新日志

> MC 1.21.6 | Fabric | Mod ID: tidengfirstmod

---

## v1.5.22

**代码优化：**

- 修复多处注释编码乱码（ChainMiningHandler、ModMenuIntegration）
- 消除重复常量：TILLABLE_BLOCKS 统一到 FarmlandTillingHandler，TidengClientMod 引用之
- 统一种子↔作物双向映射到 SeedPlantingHandler（注册表模式），CropReplantHandler 复用
- ChainMiningConfig.isPlantLike() 结果缓存，避免每帧查注册表
- 移除空 Mixin（ExampleMixin），清理 mixins.json

---

## v1.5.21

**优化：**

- UseBlockCallback 精准匹配：锄头仅在瞄准可耕地（泥土/草方块等）时触发一键锄地，种子仅在瞄准耕地时触发播种
- 新增 TILLABLE_BLOCKS 集合，避免对无关方块（石头、木头等）消耗右键交互
- 非目标方块右键时返回 PASS，恢复原版交互行为

---

## v1.5.20

**Bug 修复：**

- 修复播种时严重卡顿：SeedPlantingHandler 和 FarmlandTillingHandler 的 setBlock 改为单次 server.execute() 批量提交，不再逐方块同步调用
- 修复 HUD 界面与屏幕边缘缝隙：ChainMiningHud 坐标从 x=2,y=2 改为 x=0,y=0

**优化：**

- 播种/锄地改用 client.execute() → server.execute() 异步批量模式，客户端线程零阻塞

---

## v1.5.19

**Bug 修复：**

- 修复 ESC 键被吞：回归 KeyBindingHelper.registerKeyBinding 注册 KeyMapping，不再使用 GLFW 直读
- 修复大面积收割不复种：移除 replant 时多余的 isAir() 检查（destroy 和 replant 任务在服务器线程顺序执行，无需额外检查）
- 修复部分耕地复种失败问题：replant 任务不再因玩家位置或维度同步延迟而跳过

**优化：**

- HUD 视觉质感提升：深色背景微调、边框颜色优化、文字阴影增强可读性
- HUD 智能显示：未瞄准方块时仅显示第一行（连锁状态），瞄准后才显示预计破坏和工具状态

---

## v1.5.18

**Bug 修复：**

- 修复闪退：Accessing LegacyRandomSource from multiple threads
  - tickBreak 的 ServerLevel.destroyBlock 通过 breakServer.execute() 在服务器线程执行
  - tryAutoReplant 的 ServerLevel.setBlock 通过 server.execute() 在服务器线程执行
  - batch 数据拷贝后传入 lambda，避免客户端 clear() 后服务端读到空队列
  - client.player / client.level.dimension() 在 lambda 外捕获，lambda 内使用服务端引用
- 补充复种保护：replant 前检查该位置仍为空气块，防止重复播种

---

## v1.5.17

**Bug 修复：**

- 修复部分作物未补种的问题：prepareBreak / breakAllOres 增加正在连锁中的防重入保护，避免玩家多次点击打断补种流程
- 进一步优化 ESC 键：isChainMiningKeyDown 增加 GUI 检查，AttackBlockCallback / UseBlockCallback 增加 ESC 键直接检测 + 正在连锁保护（三重保障）
- 减少连锁破坏卡顿：批次大小从 10 降为 5，分散实体生成负载

---

## v1.5.16

**Bug 修复：**

- 修复 ESC 键被吞：增加直接检测 ESC 按键状态 + GUI 打开时跳过模组处理
- 修复连锁破坏作物时严重卡顿：BFS 扫描对作物改为同种精确匹配，不再遍历 20+ 种作物
- 修复部分作物不复种：destroyBlock 和 
eplant 使用正确维度，不再硬编码主世界
- 修复空手打作物时不补种问题（空手只连锁破坏，不消耗种子）

**优化：**

- BFS 扫描跳过未成熟作物，大幅减少扫描范围
- canBreakBlock 不再在破坏循环中重复调用（由 ServerLevel.destroyBlock 自身处理）

---

## v1.5.15

**Bug 修复：**

- 手持锄头连锁成熟作物时不再连带破坏未成熟作物（仅破坏同种成熟作物，跳过未成熟和其他种类）
- 修复 ESC 键被吞无法调出菜单的问题（所有回调在 GUI 打开时自动跳过处理）
- 修复连锁破坏作物时连带破坏附近无关方块的问题（仅连锁同种成熟作物，跨种类跳过）

---

## v1.5.14

**Bug 修复：**

- ESC 键被吞：去除 KeyBindingHelper.registerKeyBinding，改用 GLFW 直读按键，不再干扰原版键位绑定表
- 锄头连锁收割不补种：breakAllOres 破坏前保存成熟作物状态，破坏完成后批量调用 CropReplantHandler 补种，覆盖所有被连锁破坏的作物
- 连锁破坏大量作物时卡顿：破坏改为分批执行（每 tick 10 个方块），tickBreak() 逐帧推进，不再一帧全干

---

## v1.5.13

**Bug 修复：**

- ESC 键被吞：播种逻辑从 tick handler 移入 UseBlockCallback，自带 500ms 内置冷却，移除原版 tick 干预
- 作物无法连锁：配置文件加载时合并旧版与新版默认方块，旧版升级后新增方块（作物等）自动生效

---

## v1.5.12

**改进：**

- 播种和锄地中心点从玩家脚下改为准心对准的方块，可远程对准目标区域一键播种/锄地

---

## v1.5.11

**新功能：**

- 作物加入连锁列表：小麦、胡萝卜、马铃薯、甜菜根、下界疣、火把花、瓶子草、西瓜茎、南瓜茎、西瓜、南瓜
- 作物单独分组，方便连锁收割

**Bug 修复：**

- 修复播种只触发一次的问题：长按 + 右键现在每 10 tick（0.5 秒）自动重复播种，边走边种

---

## v1.5.10

**Bug 修复：**

- 修复清理草丛时概率连锁下方泥土/草方块的问题

**根因 & 修复：**

- 扫描冷却期间（10 tick）切换目标，旧结果未清空就被 prepareBreak() 保存
- 修复：目标变化时立即清空旧结果 + 强制重扫（冷却归零）
- 保险：breakAllOres 破坏前逐方块验证与目标同组，不同组跳过

**调整：**

- 最大连锁方块数从 64 提升至 128（草丛/大面积方块覆盖更广）

---

## v1.5.9

**Bug 修复：**

- 播种检测从 UseBlockCallback 移至 ClientTickEvents（tick handler），解决右键耕地时回调不触发的问题
- 播种触发条件：手持种子 + 按住 + 按住右键，触发一次后需松开右键才能再次触发，防止重复播种

**改动：**

- TidengClientMod：播种逻辑改用 client.options.keyUse 检测右键，与连锁挖矿的 tick 检测同源
- UseBlockCallback 简化为仅处理锄头锄地

---

## v1.5.8

**Bug 修复 / 交互重构：**

- 播种触发方式改为手持种子 +  + 右键耕地，不再与锄头冲突

**改动：**

- TidengClientMod：锄头右键 锄地，种子右键耕地 播种，两条路彻底分离
- SeedPlantingHandler：新增 isSeed() 方法，手持种子即为播种类型，不再搜背包
- 播种范围保持 13×13

---

## v1.5.7

**Bug 修复：**

- 修复播种不工作：SeedPlantingHandler 和 FarmlandTillingHandler 的方块状态读取改为服务端优先，消除客户端-服务端不同步导致的刚锄的地播不上种问题

**调整：**

- 播种范围从 9×9（半径4）扩大到 13×13（半径6）

---

## v1.5.6

**Bug 修复：**

- 修复一键锄地中心偏移：blockPosition() 改为 blockPosition().below()，确保锄地以玩家脚下地面为中心
- 修复一键播种中心同样偏移的问题
- 修复锄头右键时锄地抢在播种前面的冲突

---

## v1.5.5

**Bug 修复：**

- 修复一键锄地/播种后作物无法被破坏：SeedPlantingHandler 和 FarmlandTillingHandler 的 setBlock 改为单机模式下使用服务端 ServerLevel.setBlock()，确保客户端与服务端方块同步

---

## v1.5.4

**新增：**

- 一键锄地：手拿锄头 + 按住 + 右键，以玩家为中心 9×9 范围自动将泥土/草方块等锄成耕地

---

## v1.5.3

**Bug 修复：**

- 修复连锁破坏时无视工具等级：breakAllOres() 新增 canBreakBlock() 逐方块检查

---

## v1.5.2

**新增：**

- 模组显示图标：使用 512×512 下界合金镐图标

---

## v1.5.1

**Bug 修复：**

- 修复 Mod Menu 配置界面崩溃

---

## v1.5.0

**新功能：**

- 方块配置系统
- 分组连锁机制
- 农田玩法

---

## v1.2.11

**HUD 界面位置调整：**

- HUD 从屏幕中下方移至左上角

---

## v1.2.10

**版本号管理：**

- 建立版本号递增规则

---

## v1.0.0

**首次发布：**

- 按键绑定，连锁挖矿，矿物描边透视
