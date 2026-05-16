package com.yuemo.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.promotion.entity.Coupon;
import com.yuemo.promotion.entity.UserCoupon;
import com.yuemo.promotion.mapper.CouponMapper;
import com.yuemo.promotion.mapper.UserCouponMapper;
import com.yuemo.promotion.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void createCoupon(Coupon coupon) {
        couponMapper.insert(coupon);
        // 预热优惠券模板到缓存
        redisTemplate.opsForValue().set("coupon:" + coupon.getId(), coupon);
    }

    @Override
    public IPage<Coupon> pageCoupons(Integer page, Integer size) {
        return couponMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
        }
        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券已领完");
        }

        // 幂等检查
        String receiveKey = "coupon:receive:" + userId + ":" + couponId;
        Boolean first = redisTemplate.opsForSet().add(receiveKey, "1") > 0;
        if (Boolean.FALSE.equals(first)) {
            throw new BusinessException(ResultCode.COUPON_USED, "已领取过");
        }

        coupon.setReceivedCount(coupon.getReceivedCount() + 1);
        couponMapper.updateById(coupon);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCouponMapper.insert(userCoupon);
    }

    @Override
    public List<UserCoupon> getUserCoupons(Long userId, Integer status) {
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || uc.getStatus() != 0) {
            throw new BusinessException(ResultCode.COUPON_USED);
        }
        uc.setStatus(1);
        uc.setOrderId(orderId);
        userCouponMapper.updateById(uc);
    }
}
