import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Image, Table, Button, InputNumber, Empty, message, Popconfirm, Space, Tag } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { cartApi, CartItem } from '../../api/cart';
import { useDispatch } from 'react-redux';
import { setCartItems } from '../../stores/cartSlice';

export default function CartPage() {
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const fetchCart = async () => {
    setLoading(true);
    try {
      const data = await cartApi.getList();
      setItems(data);
      dispatch(setCartItems(data));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCart(); }, []);

  const handleQuantityChange = async (itemId: number, qty: number) => {
    await cartApi.updateQuantity(itemId, qty);
    fetchCart();
  };

  const handleRemove = async (itemId: number) => {
    await cartApi.remove(itemId);
    message.success('已移除');
    fetchCart();
  };

  const handleToggleSelect = async (itemId: number, selected: boolean) => {
    await cartApi.toggleSelect(itemId, selected);
    fetchCart();
  };

  const columns = [
    {
      title: '', key: 'select', width: 40,
      render: (_: unknown, r: CartItem) => (
        <input type="checkbox" checked={r.selected}
               onChange={(e) => handleToggleSelect(r.id, e.target.checked)} />
      ),
    },
    {
      title: '商品', key: 'product', width: 280,
      render: (_: unknown, r: CartItem) => (
        <Space>
          <Image src={r.productImage || '/placeholder.png'} width={60} height={60}
                 style={{ objectFit: 'cover', borderRadius: 4 }}
                 fallback="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3QgZmlsbD0iI2VlZSIgd2lkdGg9IjYwIiBoZWlnaHQ9IjYwIi8+PHRleHQgZmlsbD0iIzk5OSIgZm9udC1zaXplPSIxMiIgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYc8L3RleHQ+PC9zdmc+" />
          <div>
            <div style={{ fontWeight: 500 }}>{r.productName}</div>
            {r.specText && <Tag style={{ marginTop: 4 }}>{r.specText}</Tag>}
          </div>
        </Space>
      ),
    },
    { title: '单价', dataIndex: 'price', key: 'price', width: 100, render: (v: number) => `¥${v?.toFixed(2)}` },
    {
      title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100,
      render: (v: number, r: CartItem) => (
        <InputNumber min={1} value={v} onChange={(q) => handleQuantityChange(r.id, q || 1)} />
      ),
    },
    {
      title: '操作', key: 'action', width: 80,
      render: (_: unknown, r: CartItem) => (
        <Popconfirm title="确定移除？" onConfirm={() => handleRemove(r.id)}>
          <Button icon={<DeleteOutlined />} danger size="small" />
        </Popconfirm>
      ),
    },
  ];

  const selectedItems = items.filter((i) => i.selected);
  const totalAmount = selectedItems.reduce((sum, i) => sum + i.price * i.quantity, 0);

  return (
    <div>
      <h2>购物车</h2>
      {items.length === 0 && !loading ? (
        <Empty description="购物车是空的" />
      ) : (
        <>
          <Table columns={columns} dataSource={items} rowKey="id" loading={loading} pagination={false} />
          <div style={{ textAlign: 'right', marginTop: 16 }}>
            <span style={{ fontSize: 16, marginRight: 16 }}>
              已选 {selectedItems.length} 件，合计：
              <span style={{ color: '#f5222d', fontSize: 20, fontWeight: 'bold' }}>
                ¥{totalAmount.toFixed(2)}
              </span>
            </span>
            <Button type="primary" size="large" disabled={selectedItems.length === 0}
                    onClick={() => navigate('/checkout')}>
              去结算
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
