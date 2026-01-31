<?php
/**
 * UserInfoAPI PHP 调用示例
 * 提供了完整的封装类，方便PHP开发者调用Minecraft服务器API
 */

class MinecraftUserAPI {
    private $api_url;
    private $api_key;
    private $timeout;
    
    /**
     * 构造函数
     * @param string $api_url API服务器地址，如 "http://localhost:8080/api"
     * @param string $api_key API密钥
     * @param int $timeout 超时时间（秒）
     */
    public function __construct($api_url, $api_key, $timeout = 10) {
        $this->api_url = rtrim($api_url, '/');
        $this->api_key = $api_key;
        $this->timeout = $timeout;
    }
    
    /**
     * 获取玩家完整信息
     * @param string $username 玩家名
     * @return array 玩家信息数组
     */
    public function getUserInfo($username) {
        return $this->makeRequest('/user/info', ['username' => $username]);
    }
    
    /**
     * 获取玩家等级信息
     * @param string $username 玩家名
     * @return array 等级信息数组
     */
    public function getUserLevel($username) {
        return $this->makeRequest('/user/level', ['username' => $username]);
    }
    
    /**
     * 获取玩家位置信息
     * @param string $username 玩家名
     * @return array 位置信息数组
     */
    public function getUserLocation($username) {
        return $this->makeRequest('/user/location', ['username' => $username]);
    }
    
    /**
     * 获取玩家背包信息
     * @param string $username 玩家名
     * @return array 背包信息数组
     */
    public function getUserInventory($username) {
        return $this->makeRequest('/user/inventory', ['username' => $username]);
    }
    
    /**
     * 获取玩家登录记录
     * @param string $username 玩家名
     * @param int $limit 记录数量限制（最大100）
     * @return array 登录记录数组
     */
    public function getLoginRecords($username, $limit = 10) {
        return $this->makeRequest('/user/login-records', [
            'username' => $username,
            'limit' => min(max($limit, 1), 100)
        ]);
    }
    
    /**
     * 获取在线玩家列表
     * @return array 在线玩家信息
     */
    public function getOnlinePlayers() {
        return $this->makeRequest('/online-players');
    }
    
    /**
     * 获取服务器状态
     * @return array 服务器状态信息
     */
    public function getServerStatus() {
        return $this->makeRequest('/status');
    }
    
    /**
     * 获取安全信息
     * @return array 安全系统信息
     */
    public function getSecurityInfo() {
        return $this->makeRequest('/security/info');
    }
    
    /**
     * 批量获取多个玩家的信息
     * @param array $usernames 玩家名数组
     * @return array 玩家信息数组
     */
    public function getMultipleUserInfo($usernames) {
        $result = [];
        foreach ($usernames as $username) {
            try {
                $result[$username] = $this->getUserInfo($username);
            } catch (Exception $e) {
                $result[$username] = ['error' => $e->getMessage()];
            }
        }
        return $result;
    }
    
    /**
     * 检查玩家是否在线
     * @param string $username 玩家名
     * @return bool 是否在线
     */
    public function isPlayerOnline($username) {
        try {
            $onlinePlayers = $this->getOnlinePlayers();
            foreach ($onlinePlayers['players'] as $player) {
                if ($player['username'] === $username) {
                    return true;
                }
            }
            return false;
        } catch (Exception $e) {
            return false;
        }
    }
    
    /**
     * 获取玩家在线时长（如果在线）
     * @param string $username 玩家名
     * @return int|false 在线时长（秒），如果不在线返回false
     */
    public function getPlayerOnlineTime($username) {
        try {
            $onlinePlayers = $this->getOnlinePlayers();
            foreach ($onlinePlayers['players'] as $player) {
                if ($player['username'] === $username) {
                    return $player['onlineTime'];
                }
            }
            return false;
        } catch (Exception $e) {
            return false;
        }
    }
    
    /**
     * 发送HTTP请求
     * @param string $endpoint API端点
     * @param array $params 请求参数
     * @return array 响应数据
     * @throws Exception 请求失败时抛出异常
     */
    private function makeRequest($endpoint, $params = []) {
        // 添加API密钥
        $params['api_key'] = $this->api_key;
        
        // 构建查询字符串
        $query_string = http_build_query($params);
        $url = $this->api_url . $endpoint . '?' . $query_string;
        
        // 初始化cURL
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => $this->timeout,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_MAXREDIRS => 3,
            CURLOPT_HTTPHEADER => [
                'Accept: application/json',
                'X-API-Key: ' . $this->api_key,
                'User-Agent: MinecraftUserAPI-PHP-Client/1.0'
            ],
            CURLOPT_SSL_VERIFYPEER => false, // 如果是HTTPS且使用自签名证书
            CURLOPT_ENCODING => 'gzip, deflate'
        ]);
        
        $response = curl_exec($ch);
        $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        if ($response === false) {
            throw new Exception("cURL错误: " . $error);
        }
        
        $data = json_decode($response, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception("JSON解析错误: " . json_last_error_msg());
        }
        
        if ($http_code !== 200) {
            $error_message = isset($data['error']) ? $data['error'] : '未知错误';
            throw new Exception("API错误 (HTTP $http_code): $error_message");
        }
        
        return $data;
    }
    
    /**
     * 设置新的API密钥
     * @param string $api_key 新的API密钥
     */
    public function setApiKey($api_key) {
        $this->api_key = $api_key;
    }
    
    /**
     * 设置超时时间
     * @param int $timeout 超时时间（秒）
     */
    public function setTimeout($timeout) {
        $this->timeout = max(1, min($timeout, 300)); // 限制在1-300秒之间
    }

    /**
     * 获取玩家聊天记录
     * @param string $username 玩家名，不提供则返回所有玩家概览
     * @param int $limit 返回记录数量限制
     * @return array 聊天记录数组
     */
    public function getChatRecords($username = null, $limit = 0) {
        $params = array();

        if ($username !== null && !empty($username)) {
            $params['username'] = $username;
        }

        if ($limit > 0) {
            $params['limit'] = $limit;
        }

        return $this->makeRequest('/chat-records', $params);
    }

    /**
     * 获取服务器资源监控信息
     * @param string $type 返回类型 (all, memory, cpu, tps)
     * @return array 服务器资源信息
     */
    public function getServerResources($type = 'all') {
        $params = array();

        if ($type !== null && !empty($type)) {
            $params['type'] = $type;
        }

        return $this->makeRequest('/server/resources', $params);
    }
}

// ==================== 使用示例 ====================

// 示例1: 基础使用
try {
    // 初始化API客户端
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    
    // 获取玩家信息
    $player_info = $api->getUserInfo("Steve");
    echo "玩家名称: " . $player_info['username'] . "\n";
    echo "等级: " . $player_info['level'] . "\n";
    echo "位置: X=" . round($player_info['location']['x']) . 
         ", Y=" . round($player_info['location']['y']) . 
         ", Z=" . round($player_info['location']['z']) . "\n";
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例2: 获取登录记录
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    $login_records = $api->getLoginRecords("Steve", 5);
    
    echo "=== Steve 的最近登录记录 ===\n";
    foreach ($login_records['records'] as $record) {
        echo "登录时间: " . $record['loginTime'] . "\n";
        echo "IP地址: " . $record['ipAddress'] . "\n";
        echo "在线时长: " . formatTime($record['onlineTime']) . "\n";
        if (isset($record['isOnline']) && $record['isOnline']) {
            echo "状态: 当前在线\n";
        }
        echo "---\n";
    }
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例3: 检查在线状态
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    
    if ($api->isPlayerOnline("Steve")) {
        $online_time = $api->getPlayerOnlineTime("Steve");
        echo "Steve 当前在线，已在线 " . formatTime($online_time) . "\n";
    } else {
        echo "Steve 当前不在线\n";
    }
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例4: 批量获取多个玩家信息
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    $players = ["Steve", "Alex", "Notch"];
    $players_info = $api->getMultipleUserInfo($players);
    
    echo "=== 玩家信息汇总 ===\n";
    foreach ($players_info as $username => $info) {
        if (isset($info['error'])) {
            echo "$username: 获取失败 - " . $info['error'] . "\n";
        } else {
            echo "$username: 等级 " . $info['level'] . 
                 ", 生命 " . $info['health'] . "/" . $info['maxHealth'] . "\n";
        }
    }
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例5: 获取在线玩家列表
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    $online_data = $api->getOnlinePlayers();
    
    echo "当前在线人数: " . $online_data['count'] . "\n";
    echo "在线玩家:\n";
    foreach ($online_data['players'] as $player) {
        echo "- " . $player['username'] . 
             " (在线时长: " . formatTime($player['onlineTime']) . ")\n";
    }
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例6: 获取玩家聊天记录
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");

    // 获取特定玩家的聊天记录
    $chat_records = $api->getChatRecords("Steve", 10);

    echo "=== Steve 的最近聊天记录 ===\n";
    if (isset($chat_records['messages'])) {
        foreach ($chat_records['messages'] as $message) {
            echo "[" . $message['timestamp'] . "] " .
                 $message['playerName'] . ": " .
                 $message['message'] . "\n";
        }
    } else {
        echo "暂无聊天记录\n";
    }

    // 获取所有玩家的聊天记录概览
    $all_chat = $api->getChatRecords();
    echo "\n聊天记录概览:\n";
    echo "总玩家数: " . $all_chat['totalPlayers'] . "\n";
    echo "总消息数: " . $all_chat['totalMessages'] . "\n";

    // 获取所有玩家的聊天记录（限制50条）
    $all_messages = $api->getChatRecords(null, 50, true);
    echo "\n=== 所有玩家的最近聊天记录（最近50条）===\n";
    if (isset($all_messages['messages'])) {
        foreach ($all_messages['messages'] as $message) {
            echo "[" . $message['timestamp'] . "] " .
                 $message['playerName'] . ": " .
                 $message['message'] . "\n";
        }
        echo "\n共 " . $all_messages['count'] . " 条记录\n";
        echo "涉及玩家数: " . $all_messages['totalPlayers'] . "\n";
    } else {
        echo "暂无聊天记录\n";
    }

} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例7: 获取服务器资源监控信息
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");

    // 获取所有资源信息
    $all_resources = $api->getServerResources('all');

    echo "=== 服务器资源监控 ===\n";

    // 内存信息
    if (isset($all_resources['data']['memory'])) {
        $memory = $all_resources['data']['memory'];
        echo "\n内存使用:\n";
        echo "已使用: " . $memory['used'] . "\n";
        echo "最大: " . $memory['max'] . "\n";
        echo "使用率: " . $memory['usagePercent'] . "%\n";
        echo "空闲: " . $memory['free'] . "\n";
    }

    // CPU信息
    if (isset($all_resources['data']['cpu'])) {
        $cpu = $all_resources['data']['cpu'];
        echo "\nCPU信息:\n";
        echo "处理器数量: " . $cpu['availableProcessors'] . "\n";
        echo "系统负载: " . $cpu['systemLoadAverage'] . "\n";
        echo "使用率: " . $cpu['usagePercent'] . "%\n";
        echo "操作系统: " . $cpu['osName'] . " " . $cpu['osVersion'] . "\n";
    }

    // TPS信息
    if (isset($all_resources['data']['tps'])) {
        $tps = $all_resources['data']['tps'];
        echo "\nTPS (每秒刻数):\n";
        echo "当前TPS: " . $tps['currentTps'] . "\n";
        echo "最近1分钟: " . $tps['tps1m'] . "\n";
        echo "最近5分钟: " . $tps['tps5m'] . "\n";
        echo "最近15分钟: " . $tps['tps15m'] . "\n";
    }

    // 服务器信息
    echo "\n服务器信息:\n";
    echo "服务器名称: " . $all_resources['data']['serverName'] . "\n";
    echo "服务器版本: " . $all_resources['data']['serverVersion'] . "\n";
    echo "在线玩家: " . $all_resources['data']['onlinePlayers'] . "/" .
         $all_resources['data']['maxPlayers'] . "\n";

} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例8: 只获取特定类型的资源信息
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");

    // 只获取内存信息
    $memory = $api->getServerResources('memory');
    echo "内存使用率: " . $memory['data']['usagePercent'] . "%\n";

    // 只获取TPS信息
    $tps = $api->getServerResources('tps');
    echo "当前TPS: " . $tps['data']['currentTps'] . "\n";

} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 辅助函数：格式化时间
function formatTime($seconds) {
    if ($seconds < 60) {
        return $seconds . "秒";
    } elseif ($seconds < 3600) {
        return round($seconds / 60, 1) . "分钟";
    } else {
        return round($seconds / 3600, 1) . "小时";
    }
}
?>