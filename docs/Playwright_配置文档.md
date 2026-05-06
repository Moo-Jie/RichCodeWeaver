# Playwright 安装文档

### 参考文档：

```

https://blog.csdn.net/ak_bingbing/article/details/135852038

```

---

### 1.配置国内镜像源

Playwright 默认从 Azure CDN 下载浏览器，国内访问较慢。**配置淘宝镜像源**：

#### Windows

```powershell
# 临时设置（当前终端有效）
$env:PLAYWRIGHT_DOWNLOAD_HOST="https://npmmirror.com/mirrors/playwright/"

# 永久设置（推荐）
[System.Environment]::SetEnvironmentVariable("PLAYWRIGHT_DOWNLOAD_HOST", "https://npmmirror.com/mirrors/playwright/", "User")
```

#### Linux/macOS

```bash
# 临时设置（当前终端有效）
export PLAYWRIGHT_DOWNLOAD_HOST=https://npmmirror.com/mirrors/playwright/

# 永久设置（推荐）
echo 'export PLAYWRIGHT_DOWNLOAD_HOST=https://npmmirror.com/mirrors/playwright/' >> ~/.bashrc
source ~/.bashrc
```

### 2.安装浏览器（首次运行必须）

Playwright 需要下载浏览器二进制文件。**首次部署时必须执行以下命令**：

#### Windows 本地开发

```bash
# 方式1：使用 Maven 插件（推荐）
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# 方式2：直接使用 Playwright CLI
java -cp "target/rich-code-weaver-file-1.0-SNAPSHOT.jar;%USERPROFILE%\.m2\repository\com\microsoft\playwright\playwright\1.48.0\playwright-1.48.0.jar" com.microsoft.playwright.CLI install chromium
```

#### Linux 生产环境

```bash
# 安装所有必需依赖
dnf install -y \
mesa-libgbm \
alsa-lib \
libxshmfence \
atk \
at-spi2-atk \
cups-libs \
libdrm \
libxkbcommon \
libwayland-client \
libwayland-cursor \
libwayland-egl \
mesa-libEGL \
mesa-libGL
```

#### Docker 环境

在 Dockerfile 中添加：

```dockerfile
# 安装系统依赖
RUN apt-get update && apt-get install -y \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# 安装 Playwright 浏览器
RUN java -cp "app.jar:BOOT-INF/lib/playwright-1.48.0.jar" \
    com.microsoft.playwright.CLI install chromium
```

---

## 浏览器安装位置

Playwright 会将浏览器下载到以下位置：

- **Windows**: `%USERPROFILE%\AppData\Local\ms-playwright`
- **Linux/macOS**: `~/.cache/ms-playwright`

**大小约**: Chromium ~300MB

---

## 注意事项

### 1. 首次启动前必须安装浏览器

如果未安装浏览器，会报错：

```
Executable doesn't exist at /path/to/chromium
```

**解决方法**：执行上述安装命令。

### 2. Linux 生产环境可能缺少系统依赖

如果报错：

```
error while loading shared libraries: libnss3.so
```

**解决方法**：

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps chromium"
```

### 3. 网络问题

Playwright 默认从 Microsoft CDN 下载浏览器。如果网络受限，可设置镜像：

```bash
# 使用淘宝镜像
export PLAYWRIGHT_DOWNLOAD_HOST=https://registry.npmmirror.com/-/binary/playwright/
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

---

## 性能对比

| 指标   | Selenium | Playwright |
|------|----------|------------|
| 首次启动 | ~5s      | ~2s        |
| 截图速度 | ~3s      | ~1s        |
| 内存占用 | ~200MB   | ~150MB     |

---

## 常见问题

### Q1: 如何验证浏览器是否已安装？

```bash
# Windows
dir %USERPROFILE%\AppData\Local\ms-playwright

# Linux/macOS
ls ~/.cache/ms-playwright
```

### Q2: 如何重新安装浏览器？

```bash
# 先卸载
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="uninstall"

# 再安装
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### Q3: 如何在 CI/CD 中使用？

在 CI 脚本中添加安装步骤：

```yaml
# GitHub Actions 示例
- name: Install Playwright browsers
  run: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```