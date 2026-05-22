import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { CartItem } from '../api/cart';

interface CartState {
  items: CartItem[];
  totalCount: number;
}

const initialState: CartState = {
  items: [],
  totalCount: 0,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    setCartItems(state, action: PayloadAction<CartItem[]>) {
      state.items = action.payload;
      state.totalCount = action.payload.reduce((sum, item) => sum + item.quantity, 0);
    },
    clearCart(state) {
      state.items = [];
      state.totalCount = 0;
    },
  },
});

export const { setCartItems, clearCart } = cartSlice.actions;
export default cartSlice.reducer;
