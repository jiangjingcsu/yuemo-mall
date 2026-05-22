package com.yuemo.cart.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.cart.constant.CartConstants;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CartCleanupTask {

    private final CartItemMapper cartItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredCartItems() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(CartConstants.CART_EXPIRE_DAYS);

        log.info("开始清理过期购物车记录（{} 天前）", CartConstants.CART_EXPIRE_DAYS);

        List<CartItem> expiredItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .lt(CartItem::getUpdateTime, deadline));

        if (expiredItems.isEmpty()) {
            log.info("无过期购物车记录");
            return;
        }

        Set<Long> expiredUserIds = expiredItems.stream()
                .map(CartItem::getUserId)
                .collect(Collectors.toSet());

        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .lt(CartItem::getUpdateTime, deadline));

        for (Long userId : expiredUserIds) {
            redisTemplate.delete(CartConstants.CART_KEY_PREFIX + userId);
        }

        log.info("购物车过期清理完成: 清理 {} 条记录, 涉及 {} 个用户", expiredItems.size(), expiredUserIds.size());
    }
}
