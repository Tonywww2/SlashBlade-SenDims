# 世界树海：自然木色与室内装饰更新

## 配色

聚落的红色块来自 `botania:framed_livingwood` 和 `botania:mossy_livingwood_planks`。两者已从这批 117 个结构模板中移除，改用深褐色古橡木作连贯墙脚、木框和地板嵌条，配合灰蓝色们瑞欧木板与茅草屋顶。

## 新增细节

| 区域 | 地面 | 墙面与周边 |
| --- | --- | --- |
| 聚落木屋、采样棚、瞭望亭 | 木板嵌条、边框、少量棕绿编织垫 | 木质护墙、靠墙座椅、壁架、盆栽 |
| 古回廊、根隙圣龛 | 安山岩边带、石雕拼花、磨损砖和苔痕 | 旧木壁架、石座、附墙藤蔓 |
| 隐藏研究室、深水设施 | 灰白检修地垫和青色标记 | 管线面板、格栅、样品台；原铁板外壳完整保留 |

新增布置按各房间的墙体、地板和可用空间放置。楼梯、舱口、控制按钮、箱子、警句板和刷怪笼周围保留操作空间；主通道保持畅通。开放平台与断桥主要增加地板细节。

![实际模板剖面](C:/Users/Tony/AppData/Roaming/PrismLauncher/instances/SenDimsBanForges/minecraft/docs/world_tree_dungeons/previews/interiors.png)

图片读取真实生产 NBT 和安装的方块模型纹理；剖面移除屋顶与近侧墙，非游戏截图。植被色调和光照以客户端为准。

## 验证与生效

- 117 个模板不含红色活木及深暗之园方块；原有箱子、64 处英文警句板、30 个刷怪笼和结构标记 NBT 完全保留。
- 99 个地牢模块的所有端口、箱子、楼梯和上层气闸静态通路检查通过。
- 隔离 Forge 检查全部模板的注册 ID/属性，并验证三类地牢共 24 个随机布局、九类结构的自然生成、实际门洞/支撑、英文文本保存和气密舱室。四处隐藏刷怪笼能实际生成强化怪物。
- 71 个受保护文件哈希一致，包含维度配置、生成器 Java/JAR、拼图池、分布设置和战利品表；本轮没有修改维度 ID `sdbf:deep_realm_level_2`、生成器、战斗参数或掉落概率。
- 沿用隔离测试中既有 Quark 铁板/木炭块与 KubeJS 物品占位；生产仍使用已安装的真实模组。尚未进行客户端实机画面验收。

结构数据已写入 `kubejs/data/sdbf/structures/`。重启游戏后，在新生成区块生效。

备份：`backups/world_tree_interiors_before/`。报告：`local/world_tree_work/verification/interiors/`。可复现布置代码：`local/world_tree_work/world_tree_interiors.py` 与 `world_tree_decor.py`。
