-- Redis 滑动窗口限流 Lua 脚本
-- KEYS[1]: rate limit key
-- ARGV[1]: 每分钟允许请求数
-- ARGV[2]: 窗口大小（秒）

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = redis.call('TIME')
local current = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
local windowStart = current - (window * 1000)

-- 移除窗口外的过期记录
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- 统计窗口内请求数
local count = redis.call('ZCARD', key)

if count >= limit then
    return 0
end

-- 添加当前时间戳
redis.call('ZADD', key, current, current .. '-' .. count)
redis.call('EXPIRE', key, window + 1)

return 1
