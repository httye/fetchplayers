# GitHub 部署指南

## 🚀 快速部署到GitHub

### 1. 创建GitHub仓库

1. 登录GitHub
2. 点击右上角的 "+" → "New repository"
3. 仓库名称：`UserInfoAPI`
4. 描述：`Minecraft Paper服务器插件 - 提供REST API获取玩家信息`
5. 选择 `Public`（公开仓库）
6. 勾选 `Add a README file`
7. 点击 `Create repository`

### 2. 上传代码

#### 方法一：直接上传
1. 在新创建的仓库页面，点击 `uploading an existing file`
2. 拖拽或选择所有文件上传到仓库
3. 提交更改

#### 方法二：使用Git命令行
```bash
# 克隆仓库
git clone https://github.com/你的用户名/UserInfoAPI.git
cd UserInfoAPI

# 复制你的代码文件到这里
# 确保包含以下文件：
# - pom.xml
# - src/ 目录
# - .github/workflows/build.yml
# - README.md
# - simple_php_client.php
# - php_client_library.php

# 添加文件
git add .
git commit -m "初始提交 - UserInfoAPI v2.0"
git push origin main
```

### 3. 启用GitHub Actions

1. 进入你的仓库页面
2. 点击 `Actions` 标签
3. 如果看到工作流，说明已经自动启用
4. 如果没有，点击 `Set up a workflow yourself`

### 4. 自动编译

每次推送代码到main分支时，GitHub Actions会自动：
- 编译Java代码
- 运行测试（如果有）
- 生成JAR文件
- 上传构建产物

### 5. 下载编译好的JAR

1. 进入 `Actions` 标签
2. 点击最新的构建工作流
3. 在 `Artifacts` 部分下载：
   - `UserInfoAPI-JAR` - 插件主文件
   - `PHP-Clients` - PHP客户端文件

## 📋 项目结构

确保你的仓库包含以下文件：

```
UserInfoAPI/
├── .github/
│   └── workflows/
│       └── build.yml          # GitHub Actions工作流
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── userinfoapi/
│       │               ├── UserInfoAPIPlugin.java
│       │               ├── APIServer.java
│       │               ├── SecurityManager.java
│       │               ├── UserInfoService.java
│       │               ├── LoginRecordManager.java
│       │               ├── UserInfoCommand.java
│       │               ├── SecurityHandler.java
│       │               ├── LoginRecordsHandler.java
│       │               ├── OnlinePlayersHandler.java
│       │               ├── SecurityInfoHandler.java
│       │               ├── BatchUserHandler.java
│       │               ├── DataExportHandler.java
│       │               └── RateLimitHandler.java
│       └── resources/
│           ├── plugin.yml
│           └── config.yml
├── pom.xml                      # Maven配置文件
├── README.md                    # 项目文档
├── simple_php_client.php       # 简易PHP客户端
├── php_client_library.php      # 完整PHP客户端库
└── DEPLOYMENT.md               # 本部署指南
```

## 🔧 自定义构建

### 修改Java版本
在 `.github/workflows/build.yml` 中修改：
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v3
  with:
    java-version: '17'  # 修改为需要的版本
    distribution: 'temurin'
```

### 更新GitHub Actions版本
注意：GitHub Actions的某些操作可能需要更新到最新版本：
- `actions/upload-artifact@v4` (替代过时的v3)
- `softprops/action-gh-release@v2` (替代过时的create-release)

### 修改触发条件
在 `.github/workflows/build.yml` 中修改：
```yaml
on:
  push:
    branches: [ main, master, develop ]  # 添加或删除分支
  pull_request:
    branches: [ main, master ]
```

### 添加测试步骤
在 `Build with Maven` 步骤前添加：
```yaml
- name: Run tests
  run: mvn test
```

## 🚀 发布新版本

### 创建Release
1. 进入仓库的 `Releases` 页面
2. 点击 `Create a new release`
3. 输入版本号，如 `v2.0.1`
4. 填写发布说明
5. 点击 `Publish release`

GitHub Actions会自动：
- 编译项目
- 创建Release
- 上传JAR文件到Release

## 📊 构建状态

在README.md中添加构建状态徽章：

```markdown
![Build Status](https://github.com/你的用户名/UserInfoAPI/workflows/Build%20UserInfoAPI/badge.svg)
```

## 🔍 故障排除

### 构建失败
1. 检查 `pom.xml` 语法是否正确
2. 查看GitHub Actions的详细日志
3. 确保所有依赖项都能正确下载

### JAR文件生成失败
1. 检查Maven Shade插件配置
2. 确保主类路径正确
3. 查看构建日志中的错误信息

### 依赖下载失败
1. 检查网络连接
2. 确认依赖仓库地址正确
3. 尝试清除缓存重新构建

## 📈 最佳实践

### 1. 分支管理
- `main` - 稳定版本
- `develop` - 开发版本
- `feature/xxx` - 新功能分支

### 2. 提交规范
```
feat: 添加新功能
fix: 修复bug
docs: 更新文档
style: 代码格式调整
refactor: 代码重构
test: 添加测试
chore: 构建过程或辅助工具的变动
```

### 3. 版本管理
遵循 [Semantic Versioning](https://semver.org/)：
- 主版本号：不兼容的API修改
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修正

## 🎯 下一步

1. **添加测试**：创建单元测试和集成测试
2. **文档完善**：添加JavaDoc和更多示例
3. **功能扩展**：根据用户反馈添加新功能
4. **性能优化**：持续优化代码性能
5. **社区建设**：欢迎贡献和反馈

---

**祝部署顺利！** 🎉