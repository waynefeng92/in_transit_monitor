# Atlas 防浪费规则

> 生成时间: 2026-05-27
> 事件: arrived-vehicle-monitor 完成后，Atlas 因系统 nudge 陷入 20+ 次重复 todowrite 循环，浪费大量时间/token

## 致命陷阱：Completed 状态循环

**症状**: Boulder 已完成，但反复收到 `[internal] Continue...` nudge，导致不断重新验证已完成的 todo。

**根因**: 缺少 TERMINAL STATE 意识，把系统恢复消息当作待处理任务。

## 铁律（必须遵守）

### 1. 先读状态，再决定行动
- 收到任何 nudge 或恢复消息时，**首先**检查 `.omo/boulder.json` 的 `status`
- 如果 `status === "completed"` 且 `elapsed_ms > 0` → **直接输出最终总结，不再调用任何工具**
- 禁止：重新 read boulder.json → 重新 todowrite → 重复确认

### 2. Completed = 停止
- 如果 todo 已经是 `completed`，**禁止再次调用 todowrite**
- 如果 boulder 已经是 `completed`，**禁止启动新的任务或验证**
- 唯一允许的操作：输出最终总结并等待用户新指令

### 3. 区分消息类型
| 消息类型 | 处理方式 |
|---------|---------|
| `[internal] Continue...` | 检查状态，无变化则简短回应或不回应 |
| `BOULDER COMPLETE` nudge | 输出最终总结，停止，不调用工具 |
| 用户新指令 | 正常工作流 |

### 4. 最终总结 = 终点
- 输出最终总结后，明确说"等待你的下一步指令"
- 之后不再主动调用任何工具，除非用户发送新消息

## 验证清单（每次行动前）
- [ ] 这个操作会改变任何状态吗？如果不会，跳过。
- [ ] 这个 todo 已经是 completed 了吗？如果是，跳过。
- [ ] Boulder 已经是 completed 了吗？如果是，只输出总结。

## 教训
- **过度谨慎 = 浪费**：反复确认已完成的 work 是病态行为
- **工具调用有成本**：每次 read/write 都消耗时间和 token
- **系统 nudge ≠ 任务**：只是会话恢复机制，不是新工作
