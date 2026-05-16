import request from '../utils/request';

export interface Order {
  id: number;
  orderNo: string;
  userId: number;
  totalAmount: number;
  payAmount: number;
  status: number;
  createTime: string;
}

export const orderApi = {
  create: (params: { addressId: number; items: { productId: number; quantity: number }[] }) =>
    request.post('/order/create', params),
  getList: (params: { page: number; size: number; status?: number }) =>
    request.get('/order/list', { params }),
  getDetail: (id: number) => request.get<any, Order>(`/order/${id}`),
  cancel: (id: number) => request.post(`/order/cancel/${id}`),
};
