import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserState {
  userId: number | null;
  username: string | null;
  token: string | null;
  loggedIn: boolean;
}

const initialState: UserState = {
  userId: null,
  username: null,
  token: localStorage.getItem('token'),
  loggedIn: !!localStorage.getItem('token'),
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    login(state, action: PayloadAction<{ userId: number; username: string; accessToken: string }>) {
      state.userId = action.payload.userId;
      state.username = action.payload.username;
      state.token = action.payload.accessToken;
      state.loggedIn = true;
      localStorage.setItem('token', action.payload.accessToken);
    },
    logout(state) {
      state.userId = null;
      state.username = null;
      state.token = null;
      state.loggedIn = false;
      localStorage.removeItem('token');
    },
  },
});

export const { login, logout } = userSlice.actions;
export default userSlice.reducer;
