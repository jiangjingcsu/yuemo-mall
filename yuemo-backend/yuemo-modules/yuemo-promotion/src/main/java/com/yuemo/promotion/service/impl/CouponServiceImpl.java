package com.yuemo.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.promotion.constant.PromotionRedisKeyConstants;
import com.yuemo.promotion.entity.Coupon;
import com.yuemo.promotion.entity.UserCoupon;
import com.yuemo.promotion.enums.UserCouponStatus;
import com.yuemo.promotion.mapper.CouponMapper;
import com.yuemo.promotion.mapper.UserCouponMapper;
import com.yuemo.promotion.service.CouponService;
import com.yuemo.promotion.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @CacheEvict(value = "couponTemplate", key = "'all'")
    public void createCoupon(Coupon coupon) {
        couponMapper.insert(coupon);
    }

    @Override
    @Cacheable(value = "couponPage", key = "#page + ':' + #size")
    public IPage<CouponVO> pageCoupons(Integer page, Integer size) {
        IPage<Coupon> couponPage = couponMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getCreateTime));
        return couponPage.convert(CouponVO::from);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "couponPage", allEntries = true)
    public void receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
        }
        if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券已领完");
        }

        String receiveKey = PromotionRedisKeyConstants.COUPON_RECEIVE_PREFIX + userId + ":" + couponId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(receiveKey, "1",
                PromotionRedisKeyConstants.COUPON_RECEIVE_TTL_DAYS, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(first)) {
            throw new BusinessException(ResultCode.COUPON_USED, "已领取过");
        }

        int updated = couponMapper.incrementReceivedCount(couponId);
        if (updated == 0) {
            redisTemplate.delete(receiveKey);
            throw new BusinessException(ResultCode.COUPON_EXPIRED, "优惠券已领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponStatus.UNUSED.getCode());
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
        if (uc == null || uc.getStatus() != UserCouponStatus.UNUSED.getCode()) {
            throw new BusinessException(ResultCode.COUPON_USED);
        }
        uc.setStatus(UserCouponStatus.USED.getCode());
        uc.setOrderId(orderId);
        userCouponMapper.updateById(uc);
    }
}
