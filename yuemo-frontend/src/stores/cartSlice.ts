import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productImage: string;
  price: number;
  quantity: number;
  selected: boolean;
}

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
