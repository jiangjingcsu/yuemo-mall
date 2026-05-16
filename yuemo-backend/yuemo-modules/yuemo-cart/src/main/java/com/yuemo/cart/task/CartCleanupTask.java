package com.yuemo.cart.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 购物车过期清理任务
 * 每天凌晨 3 点清理超过 7 天未更新的购物车记录
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CartCleanupTask {

    private final CartItemMapper cartItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int EXPIRE_DAYS = 7;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredCartItems() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(EXPIRE_DAYS);

        log.info("开始清理过期购物车记录（{} 天前）", EXPIRE_DAYS);
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .lt(CartItem::getUpdateTime, deadline));

        // 清理相关 Redis 缓存键
        Set<String> keys = redisTemplate.keys("cart:user:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清理购物车 Redis 缓存: {} 个键", keys.size());
        }

        log.info("购物车过期清理完成");
    }
}
