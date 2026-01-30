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