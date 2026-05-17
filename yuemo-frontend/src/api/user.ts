import request from '../utils/request';

export interface LoginParams {
  username: string;
  password: string;
}

export interface RegisterParams {
  username: string;
  password: string;
  nickname?: string;
  phone?: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  userId: number;
  username: string;
}

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  avatar: string;
}

export const userApi = {
  login: (params: LoginParams) => request.post<LoginResult>('/user/login', params),
  register: (params: RegisterParams) => request.post('/user/register', params),
  getUserInfo: () => request.get<UserInfo>('/user/info'),
  getBalance: () => request.get<number>('/user/balance'),
  logout: () => request.post('/user/logout'),
};
