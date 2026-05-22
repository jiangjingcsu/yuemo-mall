# 可观测性策略

> AI Agent 执行过程的观测、审计和日志策略。运行时数据通过日志系统采集，不在此目录存储。

---

## 观测维度

### 1. Agent 执行日志

```yaml
记录内容:
  - agent_id: 执行的 Agent 名称
  - task_id: 任务标识
  - start_time / end_time: 执行时间
  - status: SUCCESS / FAILURE / TIMEOUT
  - files_modified: 修改的文件列表
  - error_message: 错误信息（如有）
  - retry_count: 重试次数

存储位置: 应用日志（logback）
日志级别: INFO（成功）/ WARN（重试后成功）/ ERROR（最终失败）

日志配置:
  框架: logback（Spring Boot 默认）
  配置文件: logback-spring.xml
  Appender: CONSOLE + FILE + ERROR_FILE
  滚动策略: SizeAndTimeBasedRollingPolicy，单文件 100MB，保留 30 天，总量上限 3GB
  日志路径: ${LOG_PATH:-/app/logs}/yuemo-mall.log
  Profile 分离:
    default(开发): CONSOLE + FILE + ERROR_FILE
    prod: 仅 FILE + ERROR_FILE

规划中:
  - ELK 日志聚合（当前未部署，日志仅通过文件系统查看）
```

### 2. 约束违规记录

```yaml
记录内容:
  - violated_constraint: 违反的约束文件名
  - violation_detail: 具体违规内容
  - location: 违规代码位置
  - action_taken: 处理方式（BLOCK/AUTOFIX/REPORT）
  - timestamp: 违规时间

存储位置: 应用日志（logback） + memory/incidents.md（如造成事故）
日志级别: WARN（自动修复）/ ERROR（BLOCK，需人工介入）
```

### 3. 幻觉检测记录

```yaml
记录内容:
  - hallucination_type: API|CONFIG|PATH|DATABASE|EXTERNAL
  - claimed_content: AI 声称的内容
  - actual_content: 实际情况
  - fixed: true|false（是否自动修复）
  - fix_method: AUTO|MANUAL

存储位置: 应用日志（logback）
日志级别: WARN（自动修复）/ ERROR（无法自动修复）
```

### 4. 执行性能

```yaml
规划中（当前无 Micrometer/Prometheus 采集基础设施）:
  - task_type: 任务类型（feature/bug/refactor/review）
  - context_loading_ms: 上下文加载耗时
  - execution_ms: 执行耗时
  - verification_ms: 验证耗时
  - total_ms: 总耗时
  - files_count: 涉及文件数
  - token_usage: Token 消耗（如有）

目的: 识别慢任务，优化上下文加载策略

当前实际手段:
  - 通过 logback 日志中的时间戳手动计算耗时
  - 编译/测试结果通过 CI/CD 日志查看
```

### 5. 上下文漂移检测记录

```yaml
记录内容:
  - drift_level: L1(Warning) / L2(Required Review) / L3(Blocking)
  - drift_point: 漂移点描述
  - code_actual: 代码实际状态
  - doc_described: 文档描述状态
  - affected_files: 受影响的 .harness 文件
  - fix_suggestion: 修复建议

技能: skills/drift-detection/SKILL.md
触发时机: 任务执行前（自动）、代码审查时（自动）、用户主动触发

结果处理:
  L3: 停止任务，先修复漂移
  L2: 提醒用户确认
  L1: 记录到漂移报告，不阻断
```

---

## 应用健康检查

```yaml
框架: Spring Boot Actuator
暴露端点:
  - /actuator/health: 健康检查（show-details: when-authorized）
  - /actuator/info: 应用信息（info.env.enabled: true）

网关白名单:
  - /actuator/health
  - /actuator/info

约束:
  - 禁止暴露 /actuator/env、/actuator/beans、/actuator/mappings 等敏感端点
  - 生产环境 show-details 应设为 never 或 when-authorized
```

---

## 流量监控

```yaml
框架: Alibaba Sentinel
Dashboard: ${SENTINEL_DASHBOARD:192.168.1.56:8080}
配置: eager: true（应用启动即连接 Dashboard）

能力:
  - 实时流量监控（QPS、响应时间）
  - 限流规则配置
  - 熔断降级

与网关配合:
  - 网关层限流配置在 application.yml 中
  - Sentinel Dashboard 提供可视化监控
```

---

## 日志脱敏策略

```yaml
已知债务: TD023 — 日志脱敏格式未统一

必须脱敏的字段:
  - password: 输出为 ***
  - phone: 输出为 138****1234
  - idCard: 输出为 310***********1234
  - token: 输出为 {前6位}...{后4位}
  - apiKey/secretKey: 禁止输出

脱敏方式:
  - logback 脱敏插件（推荐，如 logback-desensitize）
  - 或在代码中手动脱敏后再记录日志

禁止:
  - 在日志中输出完整密码、Token、密钥
  - 在 ERROR 日志中打印完整请求体（可能含敏感字段）
```

---

## 审计日志策略

```yaml
已知债务: TD024 — 管理员操作审计日志未实现

必须审计的操作:
  - 管理员登录/登出
  - 商品价格修改
  - 优惠券发放/修改
  - 订单状态人工变更
  - 退款操作
  - 用户余额变更

审计记录内容:
  - operator_id: 操作人 ID
  - operator_role: 操作人角色
  - action: 操作类型
  - target_type: 操作对象类型
  - target_id: 操作对象 ID
  - before_value: 变更前值
  - after_value: 变更后值
  - ip: 操作来源 IP
  - timestamp: 操作时间

存储方式:
  - 独立审计日志表（yu_audit_log）
  - 或写入应用日志（logback）+ ELK 聚合（规划中）

禁止:
  - 审计日志中记录密码原文
  - 审计日志可被普通用户访问
```

---

## 审计追踪

```yaml
审计要求:
  - 每次代码修改记录:
    - 修改前 git commit hash
    - 修改后 git commit hash
    - 修改文件列表
    - diff 摘要（新增/修改/删除行数）
  
  - 每次部署记录:
    - 部署版本
    - 部署时间
    - 部署结果
    - 回滚操作（如有）
  
  - 每次约束违规:
    - 完整记录违规内容
    - 谁做出的决策
    - 是否有豁免

审计存储: Git 历史 + CI/CD 日志
```

---

## 健康指标

```yaml
Agent 健康指标:
  task_success_rate: 目标任务成功率 > 95%
  compile_success_rate: 编译一次通过率 > 90%
  test_success_rate: 测试一次通过率 > 85%
  hallucination_rate: 幻觉检出率 < 5%
  constraint_violation_rate: 约束违规率 < 3%
  avg_fix_cycles: 平均修复轮次 < 2

告警阈值:
  - 连续 3 次编译失败 → 通知用户
  - 连续 3 次测试失败 → 通知用户
  - 幻觉检出 > 10% → 检查上下文加载策略
  - 约束违规 > 5% → 检查规则有效性

注意: 以上指标当前无自动化采集手段，需人工通过日志和 CI/CD 记录评估
```

---

## 目录说明

```yaml
本目录策略:
  - 只存放策略文件（本文件）
  - 不存放运行时数据（日志、trace、metrics）
  - 运行时数据通过以下渠道获取:
    - logback 日志文件 → 应用运行日志（${LOG_PATH}/yuemo-mall.log）
    - Git 历史 → 代码变更追踪
    - CI/CD 日志 → 编译/测试结果
    - Docker 日志 → 运行时行为（docker logs）
    - Sentinel Dashboard → 流量监控和限流指标
    - Spring Boot Actuator → 健康检查和应用信息
    - memory/ 目录 → 决策和事故记录
```
