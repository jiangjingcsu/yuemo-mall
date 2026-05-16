import request from '../utils/request';

export const couponApi = {
  getList: (params: { page: number; size: number }) =>
    request.get('/coupon/list', { params }),
  receive: (couponId: number) => request.post(`/coupon/receive/${couponId}`),
  getMyCoupons: (status?: number) =>
    request.get('/coupon/my', { params: { status } }),
};
