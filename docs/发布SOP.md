# nodegraph 发布 SOP

适用：Stonecutter 三版本（1.20.1-forge / 1.21.1-neoforge / 26.1.2-neoforge）的 git push + Maven Central + CurseForge 发布。

## 前置

- **JDK 25+**：root `build.gradle` apply 了 CurseForgeGradle 1.3.33，其插件解析即要求 JVM ≥ 25，
  因此**所有** Gradle 调用（含 `tasks`、`build`）都必须加
  `-Dorg.gradle.java.home=<jdk25>`（`scripts/publish-curseforge.ps1` 会自动探测）。
- 凭据在 `~/.gradle/gradle.properties`：`ossrhUsername` / `ossrhPassword`（Central 上传令牌）、
  `signing.*`（GPG）、`CURSE_TOKEN`（CurseForge 上传令牌，**不是** Core API key，
  拿去调 `api.curseforge.com` 会 403）。

## 步骤

```bash
# 1. 构建三版本产物 + 测试
gradlew.bat :1.20.1:build :1.21.1:build :26.1.2:build --no-daemon -Dorg.gradle.java.home=<jdk25>

# 2. 更新 CHANGELOG_CF.md（CurseForge 上传用它做 changelog，顶部加新版本段）
# 3. git commit && git push

# 4. mavenLocal（消费方本地联调）
gradlew.bat :1.20.1:publishToMavenLocal :1.21.1:publishToMavenLocal :26.1.2:publishToMavenLocal --no-daemon -Dorg.gradle.java.home=<jdk25>

# 5. Maven Central：上传 staging
gradlew.bat publishToSonatype --no-daemon -Dorg.gradle.java.home=<jdk25>
```

### Central staging 的坑（2026-09-28 实踩）

- `nexusPublishing` 必须显式 `packageGroup = 'io.github.tt432'`（stonecutter.gradle 已配）——
  Stonecutter 布局下 root 无 group，否则 `initializeSonatypeStagingRepository` 报
  `Failed to find staging profile for package group:`（空值）。
- `closeAndReleaseSonatypeStagingRepository` **单独调用会失败**：
  stagingRepositoryId 存在上一次构建的状态里，新进程找不到（`No staging repository ... created`）。
  且 `findSonatypeStagingRepository` 的 descriptionRegex 是 `\b\Q:rootName:version\E`，
  当 root 无 group/version 时描述为 `:nodegraph:unspecified`（冒号开头），`\b` 匹配不上，查不到。
- 兜底：直接调 OSSRH Staging API 手动 close：
  ```bash
  curl -u "$ossrhUsername:$ossrhPassword" -X POST \
    https://ossrh-staging-api.central.sonatype.com/service/local/staging/bulk/close \
    -H 'Content-Type: application/json' \
    -d '{"data":{"stagedRepositoryIds":["<repoId>"],"description":"release"}}'
  ```
  repoId 从 `findSonatypeStagingRepository` 的报错文本里抄（它会列出全部 open 仓库）。
- **`/staging/bulk/release` 在该兼容层上不存在**（"not supported"）——close 成功即触发
  校验+自动发布，无需手动 release。同步到 repo1.maven.org 约 10–40 分钟。

## CurseForge

```powershell
./scripts/publish-curseforge.ps1            # 三版本全发
./scripts/publish-curseforge.ps1 -Version 1.20.1
```

- 上传成功标志：`--debug` 日志里 `Artifact ... uploaded with ID <数字>`；
  服务端拒绝会抛 `Curse rejected artifact ...` 并使构建失败（BUILD SUCCESSFUL = 已被接收）。
- 新文件在公开文件页有审核/处理延迟，anon 访问 `Showing X of Y` 不立即变多是正常的；
  以 file ID 为准，不要因页面未更新而重复上传。
