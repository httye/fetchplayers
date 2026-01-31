<?php
/**
 * UserInfoAPI 简易PHP客户端 v2.0
 * 安装即用版本 - 最小化配置，最大化便利
 *
 * @author    httye
 * @project   UserInfoAPI
 * @version   2.0
 *
 * 使用方法：
 * 1. 将此文件保存为 minecraft_api.php
 * 2. 修改下面的API密钥
 * 3. 直接调用函数即可
 *
 * 注意：现在支持查询离线玩家信息（需要玩家曾经加入过服务器）
 */

// ==================== 配置区域 ====================
// 只需修改这里即可开始使用
define('MINECRAFT_API_URL', 'http://localhost:8080/api');
define('MINECRAFT_API_KEY', 'UK_default_key_change_this'); // 修改为你的API密钥
// =================================================

/**
 * 获取玩家信息 - 最简化的函数
 * @param string $username 玩家名
 * @return array|false 玩家信息，失败返回false
 */
function getPlayerInfo($username) {
    return minecraftApiRequest('/user/info', ['username' => $username]);
}

/**
 * 获取在线玩家列表
 * @return array|false 在线玩家信息，失败返回false
 */
function getOnlinePlayers() {
    return minecraftApiRequest('/online-players');
}

/**
 * 获取服务器状态
 * @return array|false 服务器状态，失败返回false
 */
function getServerStatus() {
    return minecraftApiRequest('/status');
}

/**
 * 检查玩家是否在线
 * @param string $username 玩家名
 * @return bool 是否在线
 */
function isPlayerOnline($username) {
    $onlinePlayers = getOnlinePlayers();
    if (!$onlinePlayers || !isset($onlinePlayers['players'])) {
        return false;
    }
    
    foreach ($onlinePlayers['players'] as $player) {
        if ($player['username'] === $username) {
            return true;
        }
    }
    return false;
}

/**
 * 获取玩家登录记录
 * @param string $username 玩家名
 * @param int $limit 记录数量
 * @return array|false 登录记录，失败返回false
 */
function getPlayerLoginRecords($username, $limit = 10) {
    return minecraftApiRequest('/user/login-records', [
        'username' => $username,
        'limit' => $limit
    ]);
}

/**
 * 批量获取多个玩家信息
 * @param array $usernames 玩家名数组
 * @return array|false 玩家信息，失败返回false
 */
function getMultiplePlayersInfo($usernames) {
    if (count($usernames) > 50) {
        $usernames = array_slice($usernames, 0, 50);
    }
    
    $post_data = json_encode([
        'usernames' => $usernames,
        'queryType' => 'info'
    ]);
    
    return minecraftApiRequest('/user/batch', [], 'POST', $post_data);
}

/**
 * 导出数据到文件
 * @param string $type 导出类型 (players, online-players)
 * @param string $format 导出格式 (json, csv)
 * @param string $filename 保存文件名
 * @return bool 是否成功
 */
function exportDataToFile($type = 'online-players', $format = 'json', $filename = null) {
    if (!$filename) {
        $filename = $type . '_' . date('Ymd_His') . '.' . $format;
    }

    $data = minecraftApiRequest('/export', [
        'type' => $type,
        'format' => $format
    ], 'GET', null, false);

    if ($data !== false) {
        return file_put_contents($filename, $data) !== false;
    }
    return false;
}

/**
 * 获取玩家聊天记录
 * @param string $username 玩家名，不提供则根据 $all 参数决定
 * @param int $limit 返回记录数量限制
 * @param bool $all 是否获取所有玩家的聊天记录
 * @return array|false 聊天记录，失败返回false
 */
function getChatRecords($username = null, $limit = 0, $all = false) {
    $params = [];

    if ($username !== null && !empty($username)) {
        $params['username'] = $username;
    } elseif ($all) {
        $params['all'] = 'true';
    }

    if ($limit > 0) {
        $params['limit'] = $limit;
    }

    return minecraftApiRequest('/chat-records', $params);
}

/**
 * 获取服务器资源监控信息
 * @param string $type 返回类型 (all, memory, cpu, tps)
 * @return array|false 服务器资源信息，失败返回false
 */
function getServerResources($type = 'all') {
    $params = [];

    if ($type !== null && !empty($type)) {
        $params['type'] = $type;
    }

    return minecraftApiRequest('/server/resources', $params);
}

/**
 * 内部API请求函数
 * 处理所有HTTP请求和错误处理
 */
function minecraftApiRequest($endpoint, $params = [], $method = 'GET', $post_data = null, $json_decode = true) {
    $url = MINECRAFT_API_URL . $endpoint;
    
    // 添加API密钥
    if ($method === 'GET') {
        $params['api_key'] = MINECRAFT_API_KEY;
        $url .= '?' . http_build_query($params);
    }
    
    // 初始化cURL
    $ch = curl_init();
    
    $options = [
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 10,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTPHEADER => [
            'Accept: application/json',
            'X-API-Key: ' . MINECRAFT_API_KEY,
            'User-Agent: MinecraftAPI-Simple-Client/2.0'
        ]
    ];
    
    if ($method === 'POST') {
        $options[CURLOPT_POST] = true;
        if ($post_data !== null) {
            $options[CURLOPT_POSTFIELDS] = $post_data;
            $options[CURLOPT_HTTPHEADER][] = 'Content-Type: application/json';
        }
    }
    
    curl_setopt_array($ch, $options);
    
    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    if ($response === false) {
        error_log("API请求失败: " . $error);
        return false;
    }
    
    if ($http_code !== 200) {
        error_log("API错误 (HTTP $http_code): " . $response);
        return false;
    }
    
    if ($json_decode) {
        $data = json_decode($response, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            error_log("JSON解析错误: " . json_last_error_msg());
            return false;
        }
        return $data;
    }
    
    return $response;
}

// ==================== 使用示例 ====================

// 示例1: 获取玩家信息
if (false) { // 设置为true来运行示例
    $player_info = getPlayerInfo('Steve');
    if ($player_info) {
        echo "玩家名称: " . $player_info['username'] . "\n";
        echo "等级: " . $player_info['level'] . "\n";
        echo "位置: X=" . round($player_info['location']['x']) . 
             ", Y=" . round($player_info['location']['y']) . 
             ", Z=" . round($player_info['location']['z']) . "\n";
    } else {
        echo "获取玩家信息失败\n";
    }
}

// 示例2: 获取在线玩家
if (false) { // 设置为true来运行示例
    $online_players = getOnlinePlayers();
    if ($online_players) {
        echo "当前在线人数: " . $online_players['count'] . "\n";
        foreach ($online_players['players'] as $player) {
            echo "- " . $player['username'] . "\n";
        }
    } else {
        echo "获取在线玩家失败\n";
    }
}

// 示例3: 检查玩家是否在线
if (false) { // 设置为true来运行示例
    if (isPlayerOnline('Steve')) {
        echo "Steve 当前在线\n";
    } else {
        echo "Steve 当前不在线\n";
    }
}

// 示例4: 批量查询
if (false) { // 设置为true来运行示例
    $players = ['Steve', 'Alex', 'Notch'];
    $result = getMultiplePlayersInfo($players);
    if ($result) {
        foreach ($result['results'] as $player_result) {
            if ($player_result['success']) {
                echo $player_result['username'] . ": 等级 " . $player_result['data']['level'] . "\n";
            } else {
                echo $player_result['username'] . ": 查询失败\n";
            }
        }
    }
}

// 示例5: 导出数据
if (false) { // 设置为true来运行示例
    if (exportDataToFile('online-players', 'json')) {
        echo "在线玩家数据已导出\n";
    } else {
        echo "数据导出失败\n";
    }
}

// 示例6: 获取玩家聊天记录
if (false) { // 设置为true来运行示例
    // 获取特定玩家的聊天记录
    $chat_records = getChatRecords('Steve', 10);
    if ($chat_records && isset($chat_records['messages'])) {
        echo "=== Steve 的聊天记录 ===\n";
        foreach ($chat_records['messages'] as $msg) {
            echo "[" . $msg['timestamp'] . "] " . $msg['playerName'] . ": " . $msg['message'] . "\n";
        }
    }

    // 获取所有玩家的聊天记录（限制50条）
    $all_chat = getChatRecords(null, 50, true);
    if ($all_chat && isset($all_chat['messages'])) {
        echo "\n=== 所有玩家的聊天记录（最近50条）===\n";
        foreach ($all_chat['messages'] as $msg) {
            echo "[" . $msg['timestamp'] . "] " . $msg['playerName'] . ": " . $msg['message'] . "\n";
        }
    }
}

// 示例7: 获取服务器资源信息
if (false) { // 设置为true来运行示例
    // 获取所有资源信息
    $resources = getServerResources('all');
    if ($resources && isset($resources['data'])) {
        echo "=== 服务器资源监控 ===\n";

        // 内存信息
        if (isset($resources['data']['memory'])) {
            $mem = $resources['data']['memory'];
            echo "内存使用: " . $mem['used'] . " / " . $mem['max'] . " (" . $mem['usagePercent'] . "%)\n";
        }

        // CPU信息
        if (isset($resources['data']['cpu'])) {
            $cpu = $resources['data']['cpu'];
            echo "CPU使用率: " . $cpu['usagePercent'] . "%\n";
        }

        // TPS信息
        if (isset($resources['data']['tps'])) {
            $tps = $resources['data']['tps'];
            echo "当前TPS: " . $tps['currentTps'] . "\n";
        }
    }
}

// 辅助函数：格式化时间
function formatTime($seconds) {
    if ($seconds < 60) return $seconds . "秒";
    if ($seconds < 3600) return round($seconds / 60, 1) . "分钟";
    return round($seconds / 3600, 1) . "小时";
}
?>