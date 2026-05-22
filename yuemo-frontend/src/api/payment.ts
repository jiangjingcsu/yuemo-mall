import request from '../utils/request';

export interface Payment {
  id: string;
  paymentNo: string;
  orderId: string;
  orderNo: string;
  userId: string;
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
  pay: (orderId: string, payType: number) => request.post<Payment>('/payment/pay', { orderId, payType }),
  getList: (params: { page: number; size: number }) =>
    request.get('/payment/list', { params }),
  getDetail: (id: string) => request.get<Payment>(`/payment/${id}`),
  refund: (orderId: string, reason?: string) =>
    request.post('/payment/refund', { orderId, reason }),
};
