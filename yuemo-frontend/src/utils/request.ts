import axios from 'axios';
import { message } from 'antd';

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

instance.interceptors.response.use(
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
    if (error.response) {
      const { status, data } = error.response;
      if (status === 401) {
        localStorage.removeItem('token');
        window.location.href = '/login';
        return Promise.reject(new Error('登录已过期'));
      }
      const msg = data?.message || data?.msg || `请求失败(${status})`;
      message.error(msg);
      return Promise.reject(new Error(msg));
    }
    message.error(error.message || '网络异常');
    return Promise.reject(error);
  }
);

// 包装后返回 T（拦截器已提取 data 字段）
const request = {
  get: <T = unknown>(url: string, config?: Parameters<typeof instance.get>[1]) =>
    instance.get(url, config) as unknown as Promise<T>,
  post: <T = unknown>(url: string, data?: unknown, config?: Parameters<typeof instance.post>[2]) =>
    instance.post(url, data, config) as unknown as Promise<T>,
  put: <T = unknown>(url: string, data?: unknown, config?: Parameters<typeof instance.put>[2]) =>
    instance.put(url, data, config) as unknown as Promise<T>,
  delete: <T = unknown>(url: string, config?: Parameters<typeof instance.delete>[1]) =>
    instance.delete(url, config) as unknown as Promise<T>,
};

export default request;
