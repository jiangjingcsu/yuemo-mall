import request from '../utils/request';

export interface Payment {
  id: number;
  paymentNo: string;
  orderId: number;
  orderNo: string;
  userId: number;
  amount: number;
  payType: number;
  status: number;
  thirdTradeNo: string;
  refundNo: string;
  refundReason: string;
  payTime: string;
  refundTime: string;
  createTime: string;
}

export const paymentApi = {
  pay: (orderId: number, payType: number) => request.post<Payment>('/payment/pay', { orderId, payType }),
  getList: (params: { page: number; size: number }) =>
    request.get('/payment/list', { params }),
  getDetail: (id: number) => request.get<Payment>(`/payment/${id}`),
  refund: (orderId: number, reason?: string) =>
    request.post('/payment/refund', null, { params: { orderId, reason } }),
};
