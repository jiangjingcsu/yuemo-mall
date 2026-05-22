import request from '../utils/request';

export interface OrderItem {
  id: string;
  productId: string;
  skuId: string | null;
  productName: string;
  productImage: string;
  specText: string | null;
  price: number;
  quantity: number;
  totalAmount: number;
}

export interface Order {
  id: string;
  orderNo: string;
  userId: string;
  totalAmount: number;
  payAmount: number;
  status: number;
  addressId: string;
  remark: string;
  logisticsCompany: string;
  logisticsNo: string;
  payTime: string;
  deliveryTime: string;
  receiveTime: string;
  createTime: string;
  items?: OrderItem[];
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
  getDetail: (id: string) => request.get<Order>(`/order/${id}`),
  cancel: (id: string) => request.post(`/order/cancel/${id}`),
  ship: (id: string, logisticsCompany: string, logisticsNo: string) =>
    request.post(`/order/ship/${id}`, null, { params: { logisticsCompany, logisticsNo } }),
  confirm: (id: string) => request.post(`/order/confirm/${id}`),
  delete: (id: string) => request.delete(`/order/${id}`),
};
