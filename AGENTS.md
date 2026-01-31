# UserInfoAPI - AI 辅助开发指南

本文档为 AI 辅助开发代理 (iFlow CLI) 提供项目背景信息，帮助代理理解项目结构和开发规范。

## 项目概述

**UserInfoAPI** 是一个为 Minecraft Paper 服务器开发的 REST API 插件，提供通过 HTTP 接口获取玩家信息的能力。项目采用"安装即用"的设计理念，零配置即可启动使用。

### 基本信息
- **项目名称**: UserInfoAPI
- **当前版本**: 2.0
- **作者**: httye
- **许可证**: MIT
- **仓库**: https://github.com/xcs324/fetchplayers.git

### 技术栈
- **语言**: Java 17
- **构建工具**: Maven
- **目标平台**: Minecraft Paper 1.20.4
- **主要依赖**:
  - Paper API 1.20.4-R0.1-SNAPSHOT (provided)
  - Gson 2.10.1 (用于 JSON 序列化)
  - com.sun.net.httpserver (内嵌 HTTP 服务器)

### 核心特性
- ✅ 零配置启动 - 安装后立即可用
- ✅ REST API 接口 - 标准 HTTP 端点
- ✅ 在线/离线玩家支持 - 获取玩家历史数据
- ✅ 批量查询 - 一次性查询多个玩家
- ✅ 数据导出 - 支持 JSON/CSV 格式
- ✅ API 限流 - 防止滥用
- ✅ 安全认证 - API 密钥和 IP 白名单
- ✅ 在线时长统计 - 当次和总时长
- ✅ PHP 客户端 - 提供两种客户端库

## 项目结构

```
fetchplayers/
├── .github/
│   └── workflows/
│       └── build.yml              # GitHub Actions 自动构建配置
├── src/main/
│   ├── java/com/httye/userinfoapi/
│   │   ├── UserInfoAPIPlugin.java     # 主插件类
│   │   ├── APIServer.java             # HTTP 服务器实现
│   │   ├── UserInfoService.java       # 玩家信息服务
│   │   ├── UserInfoCommand.java       # 游戏内命令处理器
│   │   ├── SecurityManager.java       # 安全管理器
│   │   ├── SecurityHandler.java       # 安全拦截器
│   │   ├── SecurityInfoHandler.java   # 安全信息端点
│   │   ├── RateLimitHandler.java      # 限流处理器
│   │   ├── LoginRecordManager.java    # 登录记录管理
│   │   ├── LoginRecordsHandler.java   # 登录记录端点
│   │   ├── OnlinePlayersHandler.java  # 在线玩家端点
│   │   ├── BatchUserHandler.java      # 批量查询端点
│   │   └── DataExportHandler.java     # 数据导出端点
│   └── resources/
│       ├── plugin.yml             # 插件元数据
│       └── config.yml             # 配置文件模板
├── pom.xml                        # Maven 构建配置
├── README.md                      # 用户文档
├── PROJECT_SUMMARY.md             # 项目改进总结
├── DEPLOYMENT.md                  # 部署指南
├── simple_php_client.php          # 简易 PHP 客户端
├── php_client_library.php         # 完整 PHP 客户端库
├── php_example.php                # PHP 使用示例
└── build.bat                      # Windows 构建脚本
```

## 核心类说明

### UserInfoAPIPlugin
**职责**: 主插件类，负责插件生命周期管理

**关键功能**:
- 插件启动/禁用
- 配置文件验证和自动修复
- API 服务器启动管理
- 登录记录管理器初始化
- 显示启动信息

**重要方法**:
- `validateAndFixConfig()` - 自动检测并修复配置缺失
- `restartAPIServer()` - 重启 API 服务器
- `getApiServerStatus()` - 获取服务器状态

### APIServer
**职责**: 内嵌 HTTP 服务器，处理 API 请求

**关键功能**:
- 基于 `com.sun.net.httpserver` 实现
- 支持线程池并发处理
- 统计请求次数和响应时间
- 路由注册和处理器管理

**重要方法**:
- `start()` - 启动服务器
- `stopServer()` - 停止服务器
- `recordRequest()` - 记录请求统计

### UserInfoService
**职责**: 玩家信息查询服务

**关键功能**:
- 支持在线玩家实时数据查询
- 支持离线玩家历史数据查询
- 提供多种信息类型（基本信息、等级、位置、背包）
- 计算在线时长

**重要方法**:
- `getUserInfo(String username)` - 获取完整玩家信息
- `getUserLevel(String username)` - 获取等级信息
- `getUserLocation(String username)` - 获取位置信息
- `getUserInventory(String username)` - 获取背包信息

### SecurityManager
**职责**: API 访问安全管理

**关键功能**:
- API 密钥验证
- IP 白名单检查
- 安全配置管理

### RateLimitHandler
**职责**: API 请求限流

**关键功能**:
- 按 IP 和 API 密钥限流
- 可配置每分钟/每小时请求限制
- 提供限流统计信息

### BatchUserHandler
**职责**: 批量玩家查询

**关键功能**:
- 支持一次性查询多个玩家
- 支持多种查询类型
- 限制单次查询最大玩家数（默认 50）

### DataExportHandler
**职责**: 数据导出功能

**关键功能**:
- 支持 JSON 和 CSV 格式导出
- 可导出在线玩家、所有玩家、登录记录
- 限制最大导出记录数（默认 1000）

### LoginRecordManager
**职责**: 玩家登录记录管理

**关键功能**:
- 监听玩家登录/登出事件
- 记录玩家在线时长
- 管理登录历史

## API 端点

### 基础端点

| 端点 | 方法 | 参数 | 描述 |
|------|------|------|------|
| `/api/status` | GET | - | 获取 API 服务器状态 |
| `/api/user/info` | GET | username | 获取玩家完整信息 |
| `/api/user/level` | GET | username | 获取玩家等级信息 |
| `/api/user/location` | GET | username | 获取玩家位置信息 |
| `/api/user/inventory` | GET | username | 获取玩家背包信息 |
| `/api/online-players` | GET | - | 获取在线玩家列表 |

### 高级端点

| 端点 | 方法 | 参数 | 描述 |
|------|------|------|------|
| `/api/user/batch` | POST | usernames[], queryType | 批量查询玩家信息 |
| `/api/export` | GET | type, format | 导出玩家数据 |
| `/api/user/login-records` | GET | username | 获取玩家登录记录 |
| `/api/security/info` | GET | - | 获取安全系统信息 |

### 认证方式
- **默认**: 无需认证（安全功能默认关闭）
- **生产环境**: 支持 API 密钥认证和 IP 白名单

## 配置文件

### config.yml 结构

```yaml
# API 服务器配置
api:
  host: "0.0.0.0"
  port: 8080

# 安全设置
security:
  enabled: false
  allowed-ips:
    - "127.0.0.1"
  api-keys:
    - key: "UK_default_key_change_this"
      name: "默认密钥"
      active: true

# 限流设置
rate-limit:
  enabled: true
  requests-per-minute: 60
  requests-per-hour: 1000

# 批量查询设置
batch-query:
  enabled: true
  max-players: 50

# 数据导出设置
data-export:
  enabled: true
  max-records: 1000
  allowed-formats:
    - "json"
    - "csv"

# 日志设置
logging:
  log-requests: true
  log-errors: true
  debug-mode: false

# 缓存设置
cache:
  enabled: true
  expire-time: 30

# 高级设置
advanced:
  request-timeout: 30
  thread-pool-size: 10
  detailed-errors: true
```

### 配置验证规则
插件启动时会自动检查并修复配置：
- API 主机和端口缺失时使用默认值
- 安全配置缺失时禁用安全功能
- 限流配置缺失时启用默认限流
- API 密钥缺失时自动生成默认密钥

## 游戏内命令

### userinfo 命令
**权限**: `userinfoapi.admin`

**子命令**:
- `/userinfo status` - 查看插件和 API 服务器状态
- `/userinfo reload` - 重载配置文件
- `/userinfo help` - 显示帮助信息

## 构建和部署

### 本地构建

**使用 Maven**:
```bash
mvn clean package
```

**使用批处理脚本** (Windows):
```bash
build.bat
```

**输出文件**:
- `target/UserInfoAPI-1.0.0.jar` - 插件主文件

### GitHub Actions 自动构建

项目配置了 GitHub Actions 工作流 (`.github/workflows/build.yml`)，支持：
- 自动构建和测试
- 生成 JAR 文件
- 上传构建产物
- 创建 Release

**触发条件**:
- 推送到 `main` 分支
- 创建 Pull Request

### 安装部署

1. 将 `UserInfoAPI-1.0.0.jar` 放入服务器的 `plugins` 文件夹
2. 启动服务器
3. 插件会自动生成配置文件
4. API 服务器自动启动在 `http://localhost:8080`

## PHP 客户端

项目提供两种 PHP 客户端：

### simple_php_client.php
**特点**: 极简版本，5个核心函数

**主要函数**:
- `getPlayerInfo($username)` - 获取玩家信息
- `getOnlinePlayers()` - 获取在线玩家
- `isPlayerOnline($username)` - 检查玩家是否在线
- `batchGetPlayers($usernames, $queryType)` - 批量查询
- `exportData($type, $format)` - 导出数据

### php_client_library.php
**特点**: 完整版本，包含高级功能

**额外功能**:
- 请求缓存
- 自动重试
- 调试模式
- 详细的错误处理

## 开发规范

### 代码风格
- 使用 Java 17 语法特性
- 遵循 Google Java Style Guide
- 使用 UTF-8 编码
- 类名使用 PascalCase
- 方法名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE

### 命名约定
- **包名**: `com.httye.userinfoapi`
- **类名**: 功能描述 + Handler/Manager/Service
- **处理器类**: `XxxHandler` (如 UserInfoHandler)
- **管理器类**: `XxxManager` (如 SecurityManager)
- **服务类**: `XxxService` (如 UserInfoService)

### 异常处理
- 所有可能抛出异常的代码必须使用 try-catch
- 记录详细的错误日志
- 向用户返回友好的错误信息
- 避免暴露敏感信息

### 日志规范
- 使用 `Logger` 记录日志
- 启动信息使用 `info` 级别
- 警告信息使用 `warning` 级别
- 错误信息使用 `severe` 级别
- 调试信息使用 `fine` 级别

### 线程安全
- API 服务器使用线程池处理并发请求
- 共享数据使用适当的同步机制
- 避免在主线程执行耗时操作
- 使用 `BukkitRunnable` 进行异步任务

## 常见任务

### 添加新的 API 端点

1. 在 `APIServer.java` 中注册新路由：
```java
server.createContext("/api/new-endpoint", new RateLimitHandler(new SecurityHandler(new NewEndpointHandler()), plugin));
```

2. 创建处理器类：
```java
private class NewEndpointHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 处理请求逻辑
    }
}
```

3. 使用 `UserInfoService` 获取数据
4. 使用 `gson.toJson()` 序列化响应
5. 使用 `sendResponse()` 发送响应

### 添加新的配置项

1. 在 `config.yml` 添加配置项
2. 在 `UserInfoAPIPlugin.validateAndFixConfig()` 中添加验证逻辑
3. 使用 `getConfig().getXxx()` 读取配置

### 修改响应格式

1. 修改对应的 Handler 类
2. 调整 `UserInfoService` 返回的数据结构
3. 确保所有相关端点保持一致

### 添加新的查询类型

1. 在 `UserInfoService` 中添加新方法
2. 在 `BatchUserHandler` 中添加新的查询类型支持
3. 更新文档说明

## 测试建议

### 单元测试
- 测试 `UserInfoService` 的各个方法
- 测试 `SecurityManager` 的验证逻辑
- 测试 `RateLimitHandler` 的限流逻辑

### 集成测试
- 测试 API 端点响应
- 测试配置文件加载和验证
- 测试插件启动和禁用流程

### 手动测试
```bash
# 测试 API 状态
curl "http://localhost:8080/api/status"

# 测试玩家信息查询
curl "http://localhost:8080/api/user/info?username=Steve"

# 测试批量查询
curl -X POST "http://localhost:8080/api/user/batch" \
  -H "Content-Type: application/json" \
  -d '{"usernames":["Steve","Alex"],"queryType":"info"}'
```

## 性能优化建议

### 缓存策略
- 启用缓存减少重复查询
- 设置合理的缓存过期时间（默认 30 秒）
- 考虑使用 Caffeine 或 Ehcache 替代简单缓存

### 限流保护
- 根据服务器性能调整限流参数
- 监控请求频率和响应时间
- 考虑实现滑动窗口限流算法

### 数据库优化
- 如果数据量大，考虑使用数据库存储历史数据
- 添加索引提高查询性能
- 定期清理过期数据

## 安全建议

### 生产环境配置
1. 启用安全功能：`security.enabled: true`
2. 修改默认 API 密钥
3. 配置 IP 白名单
4. 关闭详细错误信息：`detailed-errors: false`
5. 使用 HTTPS（需要反向代理）

### 数据保护
- 不记录敏感信息
- 验证所有输入参数
- 防止 SQL 注入（如果使用数据库）
- 防止 XSS 攻击（如果有 Web 界面）

## 故障排除

### 常见问题

1. **API 无法访问**
   - 检查端口是否被占用
   - 检查防火墙设置
   - 查看服务器控制台日志

2. **获取不到玩家信息**
   - 确保玩家名正确
   - 检查玩家是否加入过服务器
   - 查看插件日志

3. **限流触发**
   - 检查请求频率
   - 调整限流参数
   - 考虑使用缓存

### 调试模式
启用调试模式获取详细日志：
```yaml
logging:
  debug-mode: true
```

## 相关文档

- `README.md` - 用户文档和快速开始指南
- `PROJECT_SUMMARY.md` - 项目改进总结 v2.0
- `DEPLOYMENT.md` - GitHub 部署指南
- `php_example.php` - PHP 客户端使用示例

## 项目状态

**当前分支**: main  
**最新提交**: 配置文件统一 (cc173d3c33f162eaa38d074cd131483aabcbe12b)  
**工作目录**: `/home/xiaochusheng/code/java/fetchplayers`

## 开发环境

- **操作系统**: Linux 6.12.63+deb13-amd64
- **Java 版本**: 17
- **Python 版本**: 3.13.5
- **Git 版本**: 2.47.3
- **Maven**: 已安装

## 联系方式

- **作者**: httye
- **仓库**: https://github.com/xcs324/fetchplayers.git

---

**最后更新**: 2026年1月31日
**文档版本**: 1.0