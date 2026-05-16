import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const { code, message: msg, data } = response.data;
    if (code === 200) {
      return data;
    }
    if (code === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      return Promise.reject(new Error(msg));
    }
    message.error(msg || '请求失败');
    return Promise.reject(new Error(msg));
  },
  (error) => {
    message.error(error.message || '网络异常');
    return Promise.reject(error);
  }
);

export default request;
