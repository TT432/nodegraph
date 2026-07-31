# NodeGraph AGENTS.md

面向维护者的项目约定与发版 SOP。opencode / AI agent 在本仓库执行发布相关操作时遵循本文件。

## 发版前置（每次发版必做）

1. 改版本号：`gradle.properties` 的 `mod_version`（semver 三段式，如 `1.2.0`）。**只改基础版本**；
   最终坐标由 build.gradle 自动拼为 `mod_version+{minecraft_version}-{loader}`（如 `1.2.0+1.21.1-neoforge`），
   后缀是 semver build metadata（标识构建目标平台，不参与优先级比较），不要手工写进 `mod_version`。
2. **同步** `README.md` 里的三个依赖坐标（`1.20.1-forge` / `1.21.1-neoforge` / `26.1.2-neoforge`）。
3. 更新 `CHANGELOG_CF.md`（CurseForge changelog，markdown）。
4. 发版前三节点全绿：`./gradlew :1.20.1:test :1.21.1:test :26.1.2:test`（Java 25 daemon，见下）。
5. 提交：`TaskX: ...` 或 `升级到 X.Y.Z ...` 风格（中文，不标记 AI）。

## 版本化语义

- `mod_version` 遵循 semver，标识 API/特性层级，跨平台变体共享；同一 mod_version 各变体功能等价。
- `+{mc_version}-{loader}` 为 build metadata：只标识构建目标平台，不表达新旧。
- 不同后缀 jar 不可互换（面向不同 MC/loader 编译，如 26.1 的 `Identifier`/`GuiGraphicsExtractor`）。
- 三个变体同批发布：Maven Central 每个 node 各发一个带后缀版本；CurseForge 每变体一个文件。
- legacyforge 节点额外发 `dev` classifier：mojmap 字节码 + 无 mods.toml，供 ModDevGradle dev runtime
  消费（主产物是 SRG reobf jar，dev runtime 直接用会 NoSuchMethodError 且会被误识别为 mod）。

## Maven Central 发布

```bash
./gradlew publishToSonatype closeAndReleaseSonatypeRepository
```

- root 的 publishToSonatype 聚合三个版本 node 的发布（每 node 发各自带后缀版本，nexus-publish 在 root 配置）。

- **需要 Java 25 daemon**（见下「Gradle 运行注意」：`plugins{}` 块的 CurseForgeGradle 1.3.33 在 classpath 解析阶段就要求 JVM 25，所有构建都受影响）。
  手动：加 `-Dorg.gradle.java.home=<jdk25>`；或用任意 Java 25 daemon。
- 凭证：`ossrhUsername` / `ossrhPassword`（Central Portal User Token），存于 `~/.gradle/gradle.properties`。
- 签名：`signing` 插件对 `mavenJava` publication 做 GPG 签名，需本机已配置签名密钥。
- 流程：上传 staging → 自动 close + release。一条命令完成。
- 发布后到 central.sonatype.com 手动 Publish（或调 `POST /manual/upload/defaultRepository/<namespace>`）。

## CurseForge 发布

```powershell
./scripts/publish-curseforge.ps1            # 发布全部三个版本变体
./scripts/publish-curseforge.ps1 -Version 1.21.1   # 只发指定节点
```

- 脚本自动探测 Java 25+（`JAVA_25_HOME` 环境变量 → `~/.jdks` → `Program Files`），设 `org.gradle.java.home` 启动 daemon。
- **为何要 Java 25**：`CurseForgeGradle` `1.1.15` 的 HTTP 客户端被 CurseForge Cloudflare bot 防护 403 拦截（Issue #22 未修复）；`1.3.33` 修复但要求 daemon JVM ≥ 25。Java 25 环境维护者本机/CI 均已具备。
- 凭证：`CURSE_TOKEN`，存于 `~/.gradle/gradle.properties`。
- projectId 1601245。
- 配置（见 `build.gradle` 的 `publishCurseForge` 任务，**per-node**：`:<version>:publishCurseForge`）：`disableVersionDetection()` + 手动按节点 `addGameVersion` / `addModLoader 'Forge'|'NeoForge'` / `addJavaVersion` / `addEnvironment 'Client','Server'`（legacyforge 依赖 moddev 会被自动检测误判为 NeoForge；environment 是 CurseForge 2026-07-15 起的强制要求）。
- 手动方式（脚本不可用时）：`./gradlew :1.20.1:publishCurseForge :1.21.1:publishCurseForge :26.1.2:publishCurseForge -Dorg.gradle.java.home=<jdk25 路径>`。

## Gradle 运行注意

- **所有 Gradle 构建都需要 Java 25 daemon**：`plugins{}` 块声明了 `net.darkhax.curseforgegradle:1.3.33`，该插件在 buildscript classpath 解析阶段（配置任何任务之前）就要求 JVM ≥ 25。即使是 `compileJava` / `test` / Maven Central 发布等与 CurseForge 无关的任务，也必须用 Java 25 daemon。
  - 手动：`-Dorg.gradle.java.home=<jdk25 路径>`。
  - CurseForge 发布用 `./scripts/publish-curseforge.ps1`（自动探测 Java 25）。
- Gradle 完成信号在本环境不稳定：用**后台启动 + 日志文件轮询**（`Start-Process ... -RedirectStandardOutput log`，随后 `Get-Content -Tail`），不要在前台阻塞等待。
- `org.gradle.configuration-cache=true` 已启用；`publishCurseForge` 已声明 `notCompatibleWithConfigurationCache`，运行它时 CC 会 discard（良性，BUILD 仍 SUCCESSFUL）。

## git push

- remote：`https://github.com/TT432/nodegraph.git`，分支 `master`。
- 仅在用户明确要求时 push。提交不标记 AI。

## 发版典型顺序

改版本号 + changelog → 提交 → 三节点测试全绿 → Maven Central 发布（root 聚合三变体）→ CurseForge 发布（三变体）→ `git push`。
