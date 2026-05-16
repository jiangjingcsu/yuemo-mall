import request from '../utils/request';

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productImage: string;
  price: number;
  quantity: number;
  selected: boolean;
}

export const cartApi = {
  add: (productId: number, quantity: number) =>
    request.post('/cart/add', null, { params: { productId, quantity } }),
  getList: () => request.get<any, CartItem[]>('/cart/list'),
  updateQuantity: (itemId: number, quantity: number) =>
    request.put(`/cart/${itemId}`, null, { params: { quantity } }),
  remove: (itemId: number) => request.delete(`/cart/${itemId}`),
  toggleSelect: (itemId: number, selected: boolean) =>
    request.put(`/cart/${itemId}/select`, null, { params: { selected } }),
};
