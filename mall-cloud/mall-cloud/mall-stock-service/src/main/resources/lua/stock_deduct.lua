-- KEYS[1] = stock key, 例如 stock:available:1
-- ARGV[1] = 扣减数量 qty

local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
local qty = tonumber(ARGV[1])

if stock < 0 then
    return -1   --库存未初始化
end

if stock < qty then
    return 0   --库存不足
end

redis.call("DECRBY",KEYS[1],qty)
return 1    --扣减成功