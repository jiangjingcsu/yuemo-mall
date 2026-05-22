import request from '../utils/request';

export interface CartItem {
  skuId: number;
  productId: number;
  quantity: number;
  selected: boolean;
  productName: string;
  productImage: string;
  specText: string;
  price: number;
  subtotal: number;
}

export const cartApi = {
  add: (skuId: number, quantity: number) =>
    request.post<void>('/cart/add', { skuId, quantity }),
  getList: () => request.get<CartItem[]>('/cart/list'),
  updateQuantity: (skuId: number, quantity: number) =>
    request.put<void>(`/cart/sku/${skuId}`, { quantity }),
  remove: (skuId: number) => request.delete<void>(`/cart/sku/${skuId}`),
  toggleSelect: (skuId: number, selected: boolean) =>
    request.put<void>(`/cart/sku/${skuId}/select`, { selected }),
  clearSelected: () => request.delete<void>('/cart/selected'),
  selectAll: (selected: boolean) =>
    request.put<void>('/cart/select-all', null, { params: { selected } }),
};
