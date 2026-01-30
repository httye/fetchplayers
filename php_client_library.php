<?php
/**
 * UserInfoAPI PHP客户端库 v2.0
 * 提供了完整的封装类，方便PHP开发者调用Minecraft服务器API
 *
 * @author    httye
 * @project   UserInfoAPI
 * @version   2.0
 *
 * 功能特性：
 * - 完整的API封装
 * - 批量查询支持
 * - 数据导出功能
 * - 缓存机制
 * - 重试机制
 * - 详细的错误处理
 * - 调试模式
 */

class MinecraftUserAPI {
    private $api_url;
    private $api_key;
    private $timeout;
    private $debug;
    private $cache_enabled;
    private $cache_ttl;
    private $cache_dir;
    private $retry_attempts;
    private $retry_delay;
    
    // 缓存存储
    private static $memory_cache = array();
    
    /**
     * 构造函数
     * @param string $api_url API服务器地址，如 "http://localhost:8080/api"
     * @param string $api_key API密钥
     * @param array $options 可选配置参数
     */
    public function __construct($api_url, $api_key, $options = array()) {
        $this->api_url = rtrim($api_url, '/');
        $this->api_key = $api_key;
        
        // 默认配置
        $this->timeout = isset($options['timeout']) ? $options['timeout'] : 10;
        $this->debug = isset($options['debug']) ? $options['debug'] : false;
        $this->cache_enabled = isset($options['cache_enabled']) ? $options['cache_enabled'] : true;
        $this->cache_ttl = isset($options['cache_ttl']) ? $options['cache_ttl'] : 30; // 30秒
        $this->cache_dir = isset($options['cache_dir']) ? $options['cache_dir'] : sys_get_temp_dir() . '/minecraft_api_cache/';
        $this->retry_attempts = isset($options['retry_attempts']) ? $options['retry_attempts'] : 3;
        $this->retry_delay = isset($options['retry_delay']) ? $options['retry_delay'] : 1; // 1秒
        
        // 创建缓存目录
        if ($this->cache_enabled && !file_exists($this->cache_dir)) {
            mkdir($this->cache_dir, 0755, true);
        }
    }
    
    /**
     * 获取玩家完整信息
     * @param string $username 玩家名
     * @param bool $use_cache 是否使用缓存
     * @return array 玩家信息数组
     */
    public function getUserInfo($username, $use_cache = true) {
        $cache_key = "user_info_" . $username;
        
        if ($use_cache && $this->cache_enabled) {
            $cached = $this->getCache($cache_key);
            if ($cached !== false) {
                $this->log("从缓存获取玩家信息: " . $username);
                return $cached;
            }
        }
        
        $result = $this->makeRequest('/user/info', ['username' => $username]);
        
        if ($use_cache && $this->cache_enabled) {
            $this->setCache($cache_key, $result);
        }
        
        return $result;
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
     * @param string $queryType 查询类型 (info, level, location, inventory)
     * @return array 玩家信息数组
     */
    public function getMultipleUserInfo($usernames, $queryType = 'info') {
        if (count($usernames) > 50) {
            throw new Exception("一次最多查询50个玩家");
        }
        
        $post_data = json_encode([
            'usernames' => $usernames,
            'queryType' => $queryType
        ]);
        
        return $this->makeRequest('/user/batch', [], 'POST', $post_data);
    }
    
    /**
     * 导出数据
     * @param string $type 导出类型 (players, login-records, online-players)
     * @param string $format 导出格式 (json, csv)
     * @param string $username 玩家名（导出登录记录时需要）
     * @return string 导出的数据内容
     */
    public function exportData($type = 'players', $format = 'json', $username = null) {
        $params = [
            'type' => $type,
            'format' => $format
        ];
        
        if ($username !== null) {
            $params['username'] = $username;
        }
        
        // 对于导出请求，我们直接返回原始内容
        return $this->makeRequest('/export', $params, 'GET', null, false);
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
     * 获取多个玩家的在线状态
     * @param array $usernames 玩家名数组
     * @return array 玩家在线状态数组
     */
    public function getMultiplePlayersOnlineStatus($usernames) {
        $result = array();
        
        try {
            $onlinePlayers = $this->getOnlinePlayers();
            $onlineUsernames = array();
            
            foreach ($onlinePlayers['players'] as $player) {
                $onlineUsernames[] = $player['username'];
            }
            
            foreach ($usernames as $username) {
                $result[$username] = array(
                    'online' => in_array($username, $onlineUsernames),
                    'onlineTime' => $this->getPlayerOnlineTime($username)
                );
            }
        } catch (Exception $e) {
            // 如果获取失败，默认所有玩家都不在线
            foreach ($usernames as $username) {
                $result[$username] = array(
                    'online' => false,
                    'onlineTime' => false
                );
            }
        }
        
        return $result;
    }
    
    /**
     * 获取服务器信息摘要
     * @return array 服务器摘要信息
     */
    public function getServerSummary() {
        try {
            $status = $this->getServerStatus();
            $online = $this->getOnlinePlayers();
            $security = $this->getSecurityInfo();
            
            return array(
                'server_status' => $status,
                'online_players' => $online,
                'security_info' => $security,
                'timestamp' => time()
            );
        } catch (Exception $e) {
            throw new Exception("获取服务器摘要失败: " . $e->getMessage());
        }
    }
    
    /**
     * 保存玩家数据到文件
     * @param string $username 玩家名
     * @param string $filename 文件名
     * @param string $format 格式 (json, csv)
     * @return bool 是否成功
     */
    public function savePlayerDataToFile($username, $filename, $format = 'json') {
        try {
            $playerInfo = $this->getUserInfo($username);
            
            if ($format === 'csv') {
                $content = $this->convertToCSV($playerInfo);
            } else {
                $content = json_encode($playerInfo, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
            }
            
            return file_put_contents($filename, $content) !== false;
        } catch (Exception $e) {
            $this->log("保存玩家数据到文件失败: " . $e->getMessage());
            return false;
        }
    }
    
    /**
     * 发送HTTP请求
     * @param string $endpoint API端点
     * @param array $params 请求参数
     * @param string $method 请求方法 (GET, POST)
     * @param string $post_data POST数据
     * @param bool $json_decode 是否解码JSON响应
     * @return mixed 响应数据
     * @throws Exception 请求失败时抛出异常
     */
    private function makeRequest($endpoint, $params = array(), $method = 'GET', $post_data = null, $json_decode = true) {
        $start_time = microtime(true);
        
        // 添加API密钥到参数
        if ($method === 'GET') {
            $params['api_key'] = $this->api_key;
        }
        
        $url = $this->api_url . $endpoint;
        if ($method === 'GET' && !empty($params)) {
            $url .= '?' . http_build_query($params);
        }
        
        $this->log("请求URL: " . $url);
        
        $attempt = 0;
        $last_error = null;
        
        while ($attempt < $this->retry_attempts) {
            $attempt++;
            
            try {
                // 初始化cURL
                $ch = curl_init();
                
                $curl_options = array(
                    CURLOPT_URL => $url,
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_TIMEOUT => $this->timeout,
                    CURLOPT_FOLLOWLOCATION => true,
                    CURLOPT_MAXREDIRS => 3,
                    CURLOPT_HTTPHEADER => array(
                        'Accept: application/json',
                        'X-API-Key: ' . $this->api_key,
                        'User-Agent: MinecraftUserAPI-PHP-Client/2.0'
                    ),
                    CURLOPT_SSL_VERIFYPEER => false,
                    CURLOPT_ENCODING => 'gzip, deflate'
                );
                
                if ($method === 'POST') {
                    $curl_options[CURLOPT_POST] = true;
                    if ($post_data !== null) {
                        $curl_options[CURLOPT_POSTFIELDS] = $post_data;
                        $curl_options[CURLOPT_HTTPHEADER][] = 'Content-Type: application/json';
                    }
                }
                
                curl_setopt_array($ch, $curl_options);
                
                $response = curl_exec($ch);
                $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                $curl_error = curl_error($ch);
                curl_close($ch);
                
                if ($response === false) {
                    throw new Exception("cURL错误: " . $curl_error);
                }
                
                // 记录响应时间
                $response_time = microtime(true) - $start_time;
                $this->log("请求完成，HTTP代码: " . $http_code . ", 响应时间: " . round($response_time, 3) . "秒");
                
                if ($http_code === 429) {
                    // 限流错误，等待后重试
                    $retry_after = $this->extractRetryAfter($response);
                    $this->log("触发限流，等待 " . $retry_after . " 秒后重试 (尝试 " . $attempt . "/" . $this->retry_attempts . ")");
                    sleep($retry_after);
                    continue;
                }
                
                if ($http_code !== 200) {
                    $error_data = json_decode($response, true);
                    $error_message = isset($error_data['error']) ? $error_data['error'] : '未知错误';
                    throw new Exception("API错误 (HTTP $http_code): $error_message");
                }
                
                if ($json_decode) {
                    $data = json_decode($response, true);
                    if (json_last_error() !== JSON_ERROR_NONE) {
                        throw new Exception("JSON解析错误: " . json_last_error_msg());
                    }
                    return $data;
                } else {
                    return $response;
                }
                
            } catch (Exception $e) {
                $last_error = $e;
                $this->log("请求失败 (尝试 " . $attempt . "/" . $this->retry_attempts . "): " . $e->getMessage());
                
                if ($attempt < $this->retry_attempts) {
                    sleep($this->retry_delay);
                }
            }
        }
        
        // 所有重试都失败了
        throw new Exception("请求失败，已重试 " . $this->retry_attempts . " 次。最后一次错误: " . $last_error->getMessage());
    }
    
    /**
     * 提取重试等待时间
     */
    private function extractRetryAfter($response) {
        $data = json_decode($response, true);
        if (isset($data['retryAfter'])) {
            return intval($data['retryAfter']);
        }
        return 60; // 默认等待60秒
    }
    
    /**
     * 缓存相关方法
     */
    private function getCache($key) {
        // 内存缓存
        if (isset(self::$memory_cache[$key])) {
            $cached = self::$memory_cache[$key];
            if (time() - $cached['time'] < $this->cache_ttl) {
                return $cached['data'];
            } else {
                unset(self::$memory_cache[$key]);
            }
        }
        
        // 文件缓存
        if ($this->cache_enabled) {
            $cache_file = $this->cache_dir . md5($key) . '.cache';
            if (file_exists($cache_file)) {
                $cached = unserialize(file_get_contents($cache_file));
                if (time() - $cached['time'] < $this->cache_ttl) {
                    return $cached['data'];
                } else {
                    unlink($cache_file);
                }
            }
        }
        
        return false;
    }
    
    private function setCache($key, $data) {
        $cache_data = array(
            'time' => time(),
            'data' => $data
        );
        
        // 内存缓存
        self::$memory_cache[$key] = $cache_data;
        
        // 文件缓存
        if ($this->cache_enabled) {
            $cache_file = $this->cache_dir . md5($key) . '.cache';
            file_put_contents($cache_file, serialize($cache_data));
        }
    }
    
    /**
     * 工具方法
     */
    private function convertToCSV($data) {
        $csv = '';
        
        // 处理数组数据
        if (is_array($data)) {
            // 标题行
            $headers = array();
            foreach ($data as $key => $value) {
                if (!is_array($value)) {
                    $headers[] = $key;
                }
            }
            $csv .= implode(',', $headers) . "\n";
            
            // 数据行
            $values = array();
            foreach ($data as $key => $value) {
                if (!is_array($value)) {
                    $values[] = $this->escapeCSV($value);
                }
            }
            $csv .= implode(',', $values) . "\n";
        }
        
        return $csv;
    }
    
    private function escapeCSV($value) {
        if (is_null($value)) return '';
        $value = strval($value);
        if (strpos($value, ',') !== false || strpos($value, '"') !== false || strpos($value, "\n") !== false) {
            return '"' . str_replace('"', '""', $value) . '"';
        }
        return $value;
    }
    
    private function log($message) {
        if ($this->debug) {
            $timestamp = date('Y-m-d H:i:s');
            echo "[$timestamp] $message\n";
        }
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
     * 清除缓存
     */
    public function clearCache() {
        self::$memory_cache = array();
        
        if ($this->cache_enabled && file_exists($this->cache_dir)) {
            $files = glob($this->cache_dir . '*.cache');
            foreach ($files as $file) {
                unlink($file);
            }
        }
    }
}

// ==================== 使用示例 ====================

// 示例1: 基础使用
try {
    // 初始化API客户端
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥", array(
        'timeout' => 15,
        'debug' => true,
        'cache_enabled' => true,
        'retry_attempts' => 3
    ));
    
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

// 示例2: 批量查询
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    $players = array("Steve", "Alex", "Notch");
    $batch_result = $api->getMultipleUserInfo($players, 'info');
    
    echo "=== 批量查询结果 ===\n";
    foreach ($batch_result['results'] as $result) {
        if ($result['success']) {
            echo $result['username'] . ": 等级 " . $result['data']['level'] . "\n";
        } else {
            echo $result['username'] . ": 查询失败 - " . $result['error'] . "\n";
        }
    }
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例3: 导出数据
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    
    // 导出在线玩家数据为CSV
    $csv_data = $api->exportData('online-players', 'csv');
    file_put_contents('online_players.csv', $csv_data);
    echo "在线玩家数据已导出到 online_players.csv\n";
    
    // 导出JSON格式
    $json_data = $api->exportData('players', 'json');
    file_put_contents('players.json', $json_data);
    echo "玩家数据已导出到 players.json\n";
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
}

// 示例4: 获取服务器摘要
try {
    $api = new MinecraftUserAPI("http://localhost:8080/api", "你的API密钥");
    $summary = $api->getServerSummary();
    
    echo "=== 服务器摘要 ===\n";
    echo "在线人数: " . $summary['online_players']['count'] . "\n";
    echo "安全功能: " . ($summary['security_info']['securityEnabled'] ? '启用' : '禁用') . "\n";
    echo "活跃API密钥: " . $summary['security_info']['activeApiKeys'] . "\n";
    
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