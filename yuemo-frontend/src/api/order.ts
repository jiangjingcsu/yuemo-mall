import request from '../utils/request';

export interface Order {
  id: number;
  orderNo: string;
  userId: number;
  totalAmount: number;
  payAmount: number;
  status: number;
  addressId: number;
  remark: string;
  logisticsCompany: string;
  logisticsNo: string;
  payTime: string;
  deliveryTime: string;
  receiveTime: string;
  createTime: string;
}

export interface OrderPage {
  records: Order[];
  total: number;
  size: number;
  current: number;
}

export interface CreateOrderParams {
  addressId: number;
  items: { productId: number; skuId?: number; quantity: number }[];
  remark?: string;
  couponId?: number;
}

export const orderApi = {
  create: (params: CreateOrderParams) =>
    request.post<Order>('/order/create', params),
  getList: (params: { page: number; size: number; status?: number }) =>
    request.get<OrderPage>('/order/list', { params }),
  getDetail: (id: number) => request.get<Order>(`/order/${id}`),
  cancel: (id: number) => request.post(`/order/cancel/${id}`),
  ship: (id: number, logisticsCompany: string, logisticsNo: string) =>
    request.post(`/order/ship/${id}`, null, { params: { logisticsCompany, logisticsNo } }),
  confirm: (id: number) => request.post(`/order/confirm/${id}`),
  delete: (id: number) => request.delete(`/order/${id}`),
};
