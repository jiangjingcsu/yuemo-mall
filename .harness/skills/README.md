# 自定义 Skills

本目录存放项目自定义的 Claude Code Skills。

## Skill 文件格式

每个 Skill 是一个 `.md` 文件，包含：

```markdown
---
name: my-skill
description: 简短描述这个 Skill 的用途
---

# Skill 标题

## 触发条件
描述什么情况下应该使用这个 Skill。

## 执行步骤
1. 步骤一
2. 步骤二
3. 步骤三

## 输出格式
描述 Skill 的输出格式。

## 注意事项
描述使用这个 Skill 时需要注意的事项。
```

## 示例：部署检查 Skill

```markdown
---
name: deploy-check
description: 部署后自动检查服务健康状态
---

# 部署检查

## 触发条件
当用户执行部署操作后，或提到"检查部署"时触发。

## 执行步骤
1. SSH 到生产服务器
2. 执行 `docker compose ps` 检查容器状态
3. 执行 `docker compose logs --tail=50` 检查最近日志
4. 执行 `curl -s http://localhost:8080/doc.html` 检查后端健康
5. 执行 `curl -s http://localhost` 检查前端可访问性
6. 汇总报告

## 输出格式
- 容器状态表
- 关键日志摘要
- 健康检查结果（通过/失败）
- 如果失败，给出修复建议
```

## 使用方式

在 CLAUDE.md 中通过 `.harness/skills/` 路径引用这些 Skill 文件。

Claude Code 在对话中读取到对应 Skill 文件时，会按照 Skill 定义的行为执行。
