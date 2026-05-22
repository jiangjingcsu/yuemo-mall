import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import LoginPage from './pages/user/LoginPage';
import RegisterPage from './pages/user/RegisterPage';
import ProductList from './pages/product/ProductList';
import ProductDetail from './pages/product/ProductDetail';
import OrderList from './pages/order/OrderList';
import OrderDetail from './pages/order/OrderDetail';
import CheckoutPage from './pages/order/CheckoutPage';
import CartPage from './pages/cart/CartPage';
import CouponList from './pages/coupon/CouponList';
import UserCenter from './pages/user/UserCenter';
import AddressPage from './pages/user/AddressPage';
import PaymentPage from './pages/payment/PaymentPage';
import AdminDashboard from './pages/admin/AdminDashboard';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('token');
  return token ? <>{children}</> : <Navigate to="/login" replace />;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" replace />;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (payload.role !== 'ADMIN') return <Navigate to="/" replace />;
  } catch {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<MainLayout />}>
        <Route index element={<ProductList />} />
        <Route path="product/:id" element={<ProductDetail />} />
        <Route path="orders" element={<PrivateRoute><OrderList /></PrivateRoute>} />
        <Route path="orders/:id" element={<PrivateRoute><OrderDetail /></PrivateRoute>} />
        <Route path="payment/:orderId" element={<PrivateRoute><PaymentPage /></PrivateRoute>} />
        <Route path="cart" element={<PrivateRoute><CartPage /></PrivateRoute>} />
        <Route path="checkout" element={<PrivateRoute><CheckoutPage /></PrivateRoute>} />
        <Route path="coupons" element={<CouponList />} />
        <Route path="profile" element={<PrivateRoute><UserCenter /></PrivateRoute>} />
        <Route path="address" element={<PrivateRoute><AddressPage /></PrivateRoute>} />
        <Route path="admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
      </Route>
    </Routes>
  );
}
