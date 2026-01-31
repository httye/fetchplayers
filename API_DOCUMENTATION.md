# UserInfoAPI - API 接口文档

本文档详细描述了 UserInfoAPI 提供的所有 REST API 接口。

**基础信息**
- **API 基础 URL**: `http://localhost:8080/api`
- **数据格式**: JSON
- **字符编码**: UTF-8
- **CORS 支持**: 已启用

**认证方式**
- 默认：无需认证（安全功能默认关闭）
- 生产环境：支持 API 密钥认证和 IP 白名单

---

## 目录

- [基础端点](#基础端点)
- [玩家信息端点](#玩家信息端点)
- [高级功能端点](#高级功能端点)
- [新增功能端点](#新增功能端点)
- [错误响应](#错误响应)
- [认证](#认证)

---

## 基础端点

### 1. 获取 API 状态

获取 API 服务器的运行状态。

**端点**: `/api/status`
**方法**: `GET`
**参数**: 无

**请求示例**:
```bash
curl "http://localhost:8080/api/status"
```

**响应示例**:
```json
{
  "status": "online",
  "plugin": "UserInfoAPI",
  "version": "1.0.0"
}
```

---

## 玩家信息端点

### 2. 获取玩家完整信息

获取指定玩家的完整信息（支持在线和离线玩家）。

**端点**: `/api/user/info`
**方法**: `GET`
**参数**:
- `username` (必需): 玩家用户名

**请求示例**:
```bash
curl "http://localhost:8080/api/user/info?username=Steve"
```

**响应示例**:
```json
{
  "username": "Steve",
  "uuid": "8667ba71b85a407a960c8e4e30a6b02d",
  "level": 10,
  "exp": 0.5,
  "health": 20.0,
  "foodLevel": 20,
  "gameMode": "SURVIVAL",
  "online": true,
  "firstPlayed": 1640995200000,
  "lastPlayed": 1640995200000,
  "isOnline": true,
  "whitelisted": false,
  "banned": false,
  "op": false,
  "currentSessionOnlineTime": 1800,
  "totalOnlineTime": 7200
}
```

### 3. 获取玩家等级信息

获取指定玩家的等级和经验信息。

**端点**: `/api/user/level`
**方法**: `GET`
**参数**:
- `username` (必需): 玩家用户名

**请求示例**:
```bash
curl "http://localhost:8080/api/user/level?username=Steve"
```

**响应示例**:
```json
{
  "username": "Steve",
  "level": 10,
  "exp": 0.5,
  "expToLevel": 50
}
```

### 4. 获取玩家位置信息

获取指定玩家的位置坐标。

**端点**: `/api/user/location`
**方法**: `GET`
**参数**:
- `username` (必需): 玩家用户名

**请求示例**:
```bash
curl "http://localhost:8080/api/user/location?username=Steve"
```

**响应示例**:
```json
{
  "username": "Steve",
  "location": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.3,
    "world": "world"
  }
}
```

### 5. 获取玩家背包信息

获取指定玩家的背包内容。

**端点**: `/api/user/inventory`
**方法**: `GET`
**参数**:
- `username` (必需): 玩家用户名

**请求示例**:
```bash
curl "http://localhost:8080/api/user/inventory?username=Steve"
```

**响应示例**:
```json
{
  "username": "Steve",
  "inventory": [
    {
      "type": "DIAMOND_SWORD",
      "amount": 1,
      "slot": 0
    }
  ]
}
```

---

## 高级功能端点

### 6. 获取在线玩家列表

获取当前在线的所有玩家信息。

**端点**: `/api/online-players`
**方法**: `GET`
**参数**: 无

**请求示例**:
```bash
curl "http://localhost:8080/api/online-players"
```

**响应示例**:
```json
{
  "count": 3,
  "players": [
    {
      "username": "Steve",
      "displayName": "Steve",
      "onlineTime": 3600
    },
    {
      "username": "Alex",
      "displayName": "Alex",
      "onlineTime": 1800
    }
  ]
}
```

### 7. 批量查询玩家信息

一次性查询多个玩家的信息。

**端点**: `/api/user/batch`
**方法**: `POST`
**Content-Type**: `application/json`
**请求体**:
```json
{
  "usernames": ["Steve", "Alex"],
  "queryType": "info"
}
```

**参数**:
- `usernames` (必需): 玩家用户名数组
- `queryType` (可选): 查询类型，支持 `info`, `level`, `location`, `inventory`，默认为 `info`

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/user/batch" \
  -H "Content-Type: application/json" \
  -d '{"usernames":["Steve","Alex"],"queryType":"info"}'
```

**响应示例**:
```json
{
  "results": [
    {
      "username": "Steve",
      "level": 10,
      "online": true
    },
    {
      "username": "Alex",
      "level": 5,
      "online": true
    }
  ],
  "count": 2
}
```

### 8. 导出玩家数据

导出玩家数据为指定格式。

**端点**: `/api/export`
**方法**: `GET`
**参数**:
- `type` (可选): 导出类型，支持 `players`, `online`, `records`，默认为 `players`
- `format` (可选): 导出格式，支持 `json`, `csv`，默认为 `json`

**请求示例**:
```bash
curl "http://localhost:8080/api/export?type=players&format=json"
```

**响应示例**:
```json
{
  "data": [...],
  "format": "json",
  "exportTime": "2026-01-31 10:00:00",
  "recordCount": 100
}
```

### 9. 获取玩家登录记录

获取指定玩家的登录历史记录。

**端点**: `/api/user/login-records`
**方法**: `GET`
**参数**:
- `username` (必需): 玩家用户名

**请求示例**:
```bash
curl "http://localhost:8080/api/user/login-records?username=Steve"
```

**响应示例**:
```json
{
  "username": "Steve",
  "records": [
    {
      "loginTime": "2026-01-31 09:00:00",
      "logoutTime": "2026-01-31 10:00:00",
      "duration": 3600
    }
  ]
}
```

### 10. 获取安全系统信息

获取安全系统的配置和状态。

**端点**: `/api/security/info`
**方法**: `GET`
**参数**: 无

**请求示例**:
```bash
curl "http://localhost:8080/api/security/info"
```

**响应示例**:
```json
{
  "security": {
    "enabled": false,
    "allowedIps": ["127.0.0.1"],
    "apiKeyCount": 1
  },
  "rateLimit": {
    "enabled": true,
    "requestsPerMinute": 60
  }
}
```

---

## 新增功能端点

### 11. 获取玩家聊天记录 ⭐ 新增

获取聊天记录，支持单个玩家、所有玩家或概览。

**端点**: `/api/chat-records`
**方法**: `GET`
**参数**:
- `username` (可选): 玩家用户名
  - 提供：返回该玩家的聊天记录
  - 不提供 + `all=true`：返回所有玩家的聊天记录
  - 不提供 + `all=false` 或不提供：返回概览信息
- `all` (可选): 是否获取所有玩家的聊天记录，默认 `false`
- `limit` (可选): 返回记录数量限制，默认返回所有记录

**请求示例**:
```bash
# 获取特定玩家的聊天记录
curl "http://localhost:8080/api/chat-records?username=Steve&limit=10"

# 获取所有玩家的聊天记录
curl "http://localhost:8080/api/chat-records?all=true&limit=50"

# 获取所有玩家的聊天记录（不限制条数）
curl "http://localhost:8080/api/chat-records?all=true"

# 获取概览信息
curl "http://localhost:8080/api/chat-records"
```

**响应示例** (指定玩家):
```json
{
  "username": "Steve",
  "messages": [
    {
      "playerName": "Steve",
      "message": "大家好！",
      "timestamp": "2026-01-31 10:00:00"
    },
    {
      "playerName": "Steve",
      "message": "今天天气不错",
      "timestamp": "2026-01-31 10:01:00"
    }
  ],
  "count": 2,
  "responseTime": "5ms"
}
```

**响应示例** (所有玩家):
```json
{
  "messages": [
    {
      "playerName": "Steve",
      "message": "大家好！",
      "timestamp": "2026-01-31 10:00:00"
    },
    {
      "playerName": "Alex",
      "message": "嗨 Steve！",
      "timestamp": "2026-01-31 10:00:05"
    },
    {
      "playerName": "Steve",
      "message": "今天天气不错",
      "timestamp": "2026-01-31 10:01:00"
    }
  ],
  "count": 3,
  "totalPlayers": 2,
  "totalMessages": 3,
  "responseTime": "8ms"
}
```

**响应示例** (概览):
```json
{
  "totalPlayers": 5,
  "totalMessages": 120,
  "description": "使用 all=true 参数获取所有聊天记录",
  "responseTime": "3ms"
}
```

### 12. 获取服务器资源监控信息 ⭐ 新增

获取服务器的资源使用情况，包括内存、CPU 和 TPS。

**端点**: `/api/server/resources`
**方法**: `GET`
**参数**:
- `type` (可选): 返回类型，支持 `all`, `memory`, `cpu`, `tps`，默认为 `all`

**请求示例**:
```bash
# 获取所有资源信息
curl "http://localhost:8080/api/server/resources"

# 只获取内存信息
curl "http://localhost:8080/api/server/resources?type=memory"

# 只获取CPU信息
curl "http://localhost:8080/api/server/resources?type=cpu"

# 只获取TPS信息
curl "http://localhost:8080/api/server/resources?type=tps"
```

**响应示例** (type=all):
```json
{
  "data": {
    "memory": {
      "type": "memory",
      "used": "512.5 MB",
      "max": "1024 MB",
      "committed": "512 MB",
      "free": "511.5 MB",
      "usagePercent": "50.05",
      "nonHeapUsed": "45.2 MB",
      "nonHeapMax": "128 MB"
    },
    "cpu": {
      "type": "cpu",
      "availableProcessors": 4,
      "systemLoadAverage": "1.5",
      "usagePercent": "37.5",
      "osName": "Linux",
      "osVersion": "6.12.63+deb13-amd64",
      "osArch": "amd64"
    },
    "tps": {
      "type": "tps",
      "currentTps": "19.95",
      "tps1m": "19.95",
      "tps5m": "19.98",
      "tps15m": "20.00"
    },
    "serverName": "Minecraft Server",
    "serverVersion": "git-Paper-123",
    "bukkitVersion": "1.20.4-R0.1-SNAPSHOT",
    "onlinePlayers": 3,
    "maxPlayers": 20
  },
  "type": "all",
  "timestamp": 1738305600000,
  "plugin": "UserInfoAPI",
  "version": "2.0",
  "responseTime": "10ms"
}
```

**响应示例** (type=memory):
```json
{
  "data": {
    "type": "memory",
    "used": "512.5 MB",
    "max": "1024 MB",
    "committed": "512 MB",
    "free": "511.5 MB",
    "usagePercent": "50.05",
    "nonHeapUsed": "45.2 MB",
    "nonHeapMax": "128 MB"
  },
  "type": "memory",
  "timestamp": 1738305600000,
  "plugin": "UserInfoAPI",
  "version": "2.0",
  "responseTime": "3ms"
}
```

---

## 错误响应

所有错误响应都遵循以下格式：

```json
{
  "error": "错误描述信息"
}
```

### 常见错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 404 | 资源未找到 |
| 429 | 请求过于频繁（限流） |
| 500 | 服务器内部错误 |

### 错误示例

**参数错误**:
```json
{
  "error": "缺少用户名参数"
}
```

**用户未找到**:
```json
{
  "error": "用户未找到"
}
```

---

## 认证

### API 密钥认证

如果启用了安全功能，需要在请求头中包含 API 密钥：

```bash
curl -H "X-API-Key: your-api-key" "http://localhost:8080/api/user/info?username=Steve"
```

### IP 白名单

如果启用了 IP 白名单，只有白名单中的 IP 地址可以访问 API。

---

## 限流

如果启用了限流功能，API 会限制每个 IP 地址的请求频率：

- 默认每分钟最多 60 次请求
- 默认每小时最多 1000 次请求

超过限制时返回：
```json
{
  "error": "请求过于频繁，请稍后再试"
}
```

---

## CORS 支持

API 支持 CORS，允许跨域请求。默认允许所有来源：

```http
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-API-Key
```

---

## PHP 客户端使用

### 简易客户端 (simple_php_client.php)

```php
include 'simple_php_client.php';

// 获取玩家聊天记录
$chatRecords = getChatRecords('Steve', 10);
print_r($chatRecords);

// 获取服务器资源信息
$resources = getServerResources('all');
print_r($resources);
```

### 完整客户端 (php_client_library.php)

```php
include 'php_client_library.php';

$api = new UserInfoAPI('http://localhost:8080', 'your-api-key');

// 获取玩家聊天记录
$chatRecords = $api->getChatRecords('Steve', 10);
print_r($chatRecords);

// 获取服务器资源信息
$memory = $api->getServerResources('memory');
$cpu = $api->getServerResources('cpu');
$tps = $api->getServerResources('tps');
$all = $api->getServerResources('all');
```

---

## 更新日志

### v2.0 (2026-01-31)
- ✅ 新增聊天记录接口 (`/api/chat-records`)
- ✅ 新增服务器资源监控接口 (`/api/server/resources`)
- ✅ 支持离线玩家查询
- ✅ 新增在线时长统计
- ✅ 优化批量查询功能
- ✅ 完善错误处理和日志

---

**文档版本**: 1.0
**最后更新**: 2026年1月31日
**API 版本**: 2.0