# ============================================================
# 学习日记自动化脚本
# 每天随机生成一句学习心得，追加到 learning-diary.md 并提交推送
# 搭配 Windows 任务计划程序使用
# ============================================================

param(
    [string]$DiaryFile = "D:\lxylq-shopSystem\learning-diary.md",
    [string]$RepoPath  = "D:\lxylq-shopSystem"
)

# ---------- 随机心得库 ----------
$insights = @(
    "Spring Boot 的自动配置机制本质上是"约定优于配置"的极致体现——框架替你做了 80% 的决策，剩下 20% 才是你的创造力空间。",
    "微服务架构中，服务间的松耦合不仅依赖 API 契约，更需要在数据一致性上做出明智取舍——CAP 理论不是选择题，而是权衡的艺术。",
    "AI Agent 的核心不是模型本身，而是如何设计工具调用链与记忆管理——好的 Agent 架构让 LLM 从'聊天者'蜕变为'执行者'。",
    "Java 的 Stream API 让集合操作从命令式转向声明式，正如 Spring Cloud Gateway 让路由从硬编码转向配置驱动——抽象层次的提升永远是工程进化的主旋律。",
    "Spring Cloud 的服务发现机制提醒我们：分布式系统中，'知道谁活着'比'知道谁在哪'更重要——健康检查是微服务治理的第一道防线。",
    "AI Agent 的 ReAct 模式（推理-行动循环）与微服务的 saga 模式有异曲同工之妙——都是通过分步执行和补偿机制来应对不确定性。",
    "Spring Boot 3 的 AOT 编译让我们看到 Java 在云原生时代的进化方向——更快的启动速度、更低的内存占用，GraalVM 正在重塑 Java 微服务的部署形态。",
    "在微服务中引入 AI Agent 后，服务编排从静态的流程引擎转向动态的智能决策——Agent 在理解上下文后自主规划执行路径，而非机械地按 DAG 图流转。",
    "Java 虚拟线程（Virtual Threads）的出现颠覆了传统的线程模型——高并发不再意味着高成本，这为 AI Agent 的并行工具调用提供了天然的土壤。",
    "Spring AI 框架的设计哲学是'让 AI 成为 Spring 生态的一等公民'——就像当年 Spring Data 统一了数据访问，Spring AI 正在统一 LLM 集成的编程模型。",
    "微服务的可观测性三支柱——日志、指标、追踪——在 AI Agent 场景下需要增加第四根支柱：决策轨迹（Decision Trace），让 Agent 的推理过程可审计、可回溯。",
    "Docker Compose 到 Kubernetes 的跃迁，本质是从'运维脚本'到'声明式期望状态'的思维转变——这和 AI Agent 从'规则引擎'到'目标驱动'的进化如出一辙。",
    "Spring Security 的过滤器链模式教会我们：安全不是一道门，而是一条流水线——每一环都有明确的职责，这与 AI Agent 的 Pipeline 架构不谋而合。",
    "Redis 在微服务中不只是缓存，更是分布式协调的瑞士军刀——分布式锁、消息队列、会话共享，AI Agent 的对话记忆同样可以借助 Redis 实现跨实例共享。"
)

# ---------- 随机选取一句 ----------
$insight = Get-Random -InputObject $insights
$today   = Get-Date -Format "yyyy-MM-dd"

# ---------- 构建日记条目 ----------
$entry = @"

### $today

今天的学习记录：
$insight
"@

# ---------- 追加到文件 ----------
Add-Content -Path $DiaryFile -Value $entry -Encoding UTF8

Write-Host "[$(Get-Date -Format 'HH:mm:ss')] 已追加学习日记: " -NoNewline
Write-Host "$insight" -ForegroundColor Cyan

# ---------- Git 操作 ----------
Set-Location $RepoPath

# 清理可能残留的锁文件
$lockFile = Join-Path $RepoPath ".git\index.lock"
if (Test-Path $lockFile) {
    Remove-Item -LiteralPath $lockFile -Force
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] 已清理残留的 index.lock" -ForegroundColor Yellow
}

git add .
if ($LASTEXITCODE -ne 0) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] git add 失败" -ForegroundColor Red
    exit 1
}

git commit -m "learning diary $today"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] git commit 失败（可能无变更）" -ForegroundColor Yellow
}

git push origin main
if ($LASTEXITCODE -ne 0) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] git push 失败" -ForegroundColor Red
    exit 1
}

Write-Host "[$(Get-Date -Format 'HH:mm:ss')] 完成！" -ForegroundColor Green
