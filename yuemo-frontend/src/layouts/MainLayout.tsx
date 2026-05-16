import { Outlet, useNavigate } from 'react-router-dom';
import { Layout, Menu, Button, Badge, Dropdown, Space } from 'antd';
import {
  HomeOutlined,
  ShoppingCartOutlined,
  OrderedListOutlined,
  GiftOutlined,
  UserOutlined,
  LogoutOutlined,
  EnvironmentOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../stores';
import { logout } from '../stores/userSlice';
import { userApi } from '../api/user';

const { Header, Content, Footer } = Layout;

export default function MainLayout() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { loggedIn, username } = useSelector((s: RootState) => s.user);
  const { totalCount } = useSelector((s: RootState) => s.cart);

  const handleLogout = async () => {
    try { await userApi.logout(); } catch { /* ignore */ }
    dispatch(logout());
    navigate('/');
  };

  const menuItems = [
    { key: '/', label: '首页', icon: <HomeOutlined /> },
    { key: '/orders', label: '我的订单', icon: <OrderedListOutlined /> },
    { key: '/coupons', label: '优惠券', icon: <GiftOutlined /> },
  ];

  const userMenuItems = [
    { key: 'profile', label: '个人中心', icon: <UserOutlined /> },
    { key: 'address', label: '收货地址', icon: <EnvironmentOutlined /> },
    { key: 'admin', label: '后台管理', icon: <SettingOutlined /> },
    { type: 'divider' as const },
    { key: 'logout', label: '退出登录', icon: <LogoutOutlined />, danger: true },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        background: '#fff', borderBottom: '1px solid #f0f0f0', padding: '0 50px',
        position: 'sticky', top: 0, zIndex: 100,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 40 }}>
          <h1 style={{ margin: 0, fontSize: 20, color: '#1677ff', cursor: 'pointer' }}
              onClick={() => navigate('/')}>
            月魔商城
          </h1>
          <Menu
            mode="horizontal"
            defaultSelectedKeys={['/']}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ border: 'none', flex: 1, minWidth: 300 }}
          />
        </div>

        <Space size="middle">
          <Badge count={totalCount} size="small">
            <Button icon={<ShoppingCartOutlined />} onClick={() => navigate('/cart')}>
              购物车
            </Button>
          </Badge>
          {loggedIn ? (
            <Dropdown menu={{
              items: [
                { key: 'username', label: username, disabled: true },
                ...userMenuItems,
              ],
              onClick: ({ key }) => {
              if (key === 'logout') handleLogout();
              else navigate(`/${key}`);
            },
            }}>
              <Button icon={<UserOutlined />}>{username}</Button>
            </Dropdown>
          ) : (
            <Button type="primary" onClick={() => navigate('/login')}>登录</Button>
          )}
        </Space>
      </Header>

      <Content style={{ padding: '24px 50px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </Content>

      <Footer style={{ textAlign: 'center', background: '#f5f5f5' }}>
        月魔商城 ©2026 — 单体分层架构实践
      </Footer>
    </Layout>
  );
}
