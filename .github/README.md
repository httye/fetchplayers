# 🚀 UserInfoAPI - Minecraft Paper 插件

[![Build Status](https://github.com/httye/UserInfoAPI/workflows/Build%20UserInfoAPI/badge.svg)](https://github.com/httye/UserInfoAPI/actions)
[![Release](https://img.shields.io/github/v/release/httye/UserInfoAPI)](https://github.com/httye/UserInfoAPI/releases)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17+-orange.svg)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/paper-1.20+-yellow.svg)](https://papermc.io/)

**作者**: httye
**项目**: UserInfoAPI

## 📋 项目简介

UserInfoAPI 是一个为 Minecraft Paper 服务器开发的插件，提供 REST API 接口来获取玩家信息。**安装即用，零配置启动！**

### ✨ 主要特性

- 🎯 **零配置启动** - 插件安装后立即可用
- ⚡ **批量查询** - 一次性查询多个玩家信息
- 📊 **数据导出** - 支持JSON/CSV格式导出
- 🔒 **API限流** - 防止API被滥用
- 💾 **智能缓存** - 提高响应速度
- 🐘 **PHP友好** - 优化的JSON格式和CORS支持
- 🔧 **错误恢复** - 自动修复配置问题

## 🚀 快速开始

### 1. 下载安装
1. 从 [Releases](https://github.com/用户名/UserInfoAPI/releases) 下载最新版本
2. 将 `UserInfoAPI-*.jar` 放入服务器的 `plugins` 文件夹
3. 启动服务器，插件会自动启动API服务

### 2. 测试API
```bash
curl "http://localhost:8080/api/status"
```

### 3. PHP调用示例
```php
include 'simple_php_client.php';
$player = getPlayerInfo('Steve');
echo "玩家等级: " . $player['level'];
```

## 📖 详细文档

- [完整文档](README.md) - 详细的使用说明和API文档
- [部署指南](DEPLOYMENT.md) - GitHub自动编译部署指南
- [PHP客户端](simple_php_client.php) - 简易PHP客户端库

## 🔧 API 端点

### 基础端点
- `GET /api/status` - 服务器状态
- `GET /api/user/info?username=Steve` - 玩家信息
- `GET /api/online-players` - 在线玩家
- `GET /api/user/login-records?username=Steve` - 登录记录

### 高级端点
- `POST /api/user/batch` - 批量查询
- `GET /api/export?type=players&format=json` - 数据导出

## 📁 下载

### 最新版本
前往 [Releases](https://github.com/httye/UserInfoAPI/releases) 页面下载最新版本。

### 构建产物
每次提交代码后，GitHub Actions会自动构建：
1. 进入 [Actions](https://github.com/httye/UserInfoAPI/actions) 页面
2. 选择最新的构建
3. 在 `Artifacts` 部分下载构建产物

## 🛠️ 开发

### 环境要求
- Java 17 或更高版本
- Maven 3.6+
- Minecraft Paper 1.20+

### 本地构建
```bash
git clone https://github.com/httye/UserInfoAPI.git
cd UserInfoAPI
mvn clean package
```

### 项目结构
```
UserInfoAPI/
├── src/main/java/              # Java源代码
├── src/main/resources/         # 配置文件
├── .github/workflows/          # GitHub Actions
├── simple_php_client.php       # 简易PHP客户端
├── php_client_library.php      # 完整PHP客户端库
├── pom.xml                     # Maven配置
└── README.md                   # 文档
```

## 🤝 贡献

欢迎贡献代码、报告问题或提出功能建议！

### 如何贡献
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 报告问题
请使用 [Issues](https://github.com/用户名/UserInfoAPI/issues) 页面报告问题。

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [PaperMC](https://papermc.io/) - 优秀的Minecraft服务器软件
- [Gson](https://github.com/google/gson) - JSON处理库
- 所有贡献者和用户

## 📞 联系方式

- 提交 Issue: [Issues](https://github.com/用户名/UserInfoAPI/issues)
- 讨论区: [Discussions](https://github.com/用户名/UserInfoAPI/discussions)

---

**⭐ 如果这个项目对你有帮助，请给个Star！** 

Made with ❤️ for the Minecraft community