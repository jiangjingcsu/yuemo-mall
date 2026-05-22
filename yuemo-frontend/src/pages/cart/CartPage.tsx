import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Image, Table, Button, InputNumber, Empty, message, Popconfirm, Space, Tag, Checkbox, theme } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { cartApi, type CartItem } from '@/api/cart';
import { useDispatch } from 'react-redux';
import { setCartItems } from '@/stores/cartSlice';

export default function CartPage() {
  const [items, setItems] = useState<CartItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [removingIds, setRemovingIds] = useState<Set<number>>(new Set());
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { token } = theme.useToken();
  const quantityTimers = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());

  const fetchCart = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await cartApi.getList();
      setItems(data);
      dispatch(setCartItems(data));
    } catch {
      message.error('加载购物车失败');
    } finally {
      setIsLoading(false);
    }
  }, [dispatch]);

  useEffect(() => { fetchCart(); }, [fetchCart]);

  const handleQuantityChange = useCallback((skuId: number, qty: number) => {
    setItems((prev) => prev.map((i) => i.skuId === skuId ? { ...i, quantity: qty } : i));

    const existing = quantityTimers.current.get(skuId);
    if (existing) clearTimeout(existing);

    const timer = setTimeout(async () => {
      try {
        await cartApi.updateQuantity(skuId, qty);
      } catch {
        message.error('更新数量失败');
        fetchCart();
      }
    }, 500);
    quantityTimers.current.set(skuId, timer);
  }, [fetchCart]);

  useEffect(() => {
    return () => {
      quantityTimers.current.forEach((t) => clearTimeout(t));
    };
  }, []);

  const handleRemove = useCallback(async (skuId: number) => {
    if (removingIds.has(skuId)) return;
    setRemovingIds((prev) => new Set(prev).add(skuId));
    try {
      await cartApi.remove(skuId);
      message.success('已移除');
      fetchCart();
    } catch {
      message.error('移除失败');
    } finally {
      setRemovingIds((prev) => {
        const next = new Set(prev);
        next.delete(skuId);
        return next;
      });
    }
  }, [fetchCart, removingIds]);

  const handleToggleSelect = useCallback(async (skuId: number, selected: boolean) => {
    try {
      await cartApi.toggleSelect(skuId, selected);
      fetchCart();
    } catch {
      message.error('操作失败');
    }
  }, [fetchCart]);

  const handleSelectAll = useCallback(async (selected: boolean) => {
    try {
      await cartApi.selectAll(selected);
      fetchCart();
    } catch {
      message.error('操作失败');
    }
  }, [fetchCart]);

  const handleClearSelected = useCallback(async () => {
    try {
      await cartApi.clearSelected();
      message.success('已清除选中商品');
      fetchCart();
    } catch {
      message.error('清除失败');
    }
  }, [fetchCart]);

  const selectedItems = useMemo(() => items.filter((i) => i.selected), [items]);
  const allSelected = items.length > 0 && selectedItems.length === items.length;

  const handleCheckout = useCallback(() => {
    navigate('/checkout', { state: { selectedItems } });
  }, [navigate, selectedItems]);

  const totalAmount = useMemo(
    () => selectedItems.reduce((sum, i) => sum + (i.subtotal ?? (i.price ?? 0) * i.quantity), 0),
    [selectedItems]
  );

  const columns = useMemo(() => [
    {
      title: (
        <Checkbox checked={allSelected}
                  indeterminate={selectedItems.length > 0 && selectedItems.length < items.length}
                  onChange={(e) => handleSelectAll(e.target.checked)} />
      ),
      key: 'select', width: 48,
      render: (_: unknown, r: CartItem) => (
        <Checkbox checked={r.selected} onChange={(e) => handleToggleSelect(r.skuId, e.target.checked)} />
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
    { title: '单价', dataIndex: 'price', key: 'price', width: 100, render: (v: number) => `¥${v?.toFixed(2) ?? '0.00'}` },
    {
      title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100,
      render: (v: number, r: CartItem) => (
        <InputNumber min={1} value={v} onChange={(q) => q != null && handleQuantityChange(r.skuId, q)} />
      ),
    },
    {
      title: '小计', key: 'subtotal', width: 120,
      render: (_: unknown, r: CartItem) => (
        <span style={{ color: token.colorError, fontWeight: 'bold' }}>¥{(r.subtotal ?? 0).toFixed(2)}</span>
      ),
    },
    {
      title: '操作', key: 'action', width: 80,
      render: (_: unknown, r: CartItem) => (
        <Popconfirm title="确定移除？" onConfirm={() => handleRemove(r.skuId)}>
          <Button icon={<DeleteOutlined />} danger size="small" loading={removingIds.has(r.skuId)} />
        </Popconfirm>
      ),
    },
  ], [allSelected, selectedItems.length, items.length, handleSelectAll, handleToggleSelect, handleQuantityChange, handleRemove, removingIds, token.colorError]);

  return (
    <div>
      <h2>购物车</h2>
      {items.length === 0 && !isLoading ? (
        <Empty description="购物车是空的" />
      ) : (
        <>
          <Table columns={columns} dataSource={items} rowKey="skuId" loading={isLoading} pagination={false} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16 }}>
            <Space>
              <Checkbox checked={allSelected}
                        indeterminate={selectedItems.length > 0 && selectedItems.length < items.length}
                        onChange={(e) => handleSelectAll(e.target.checked)}>
                全选
              </Checkbox>
              {selectedItems.length > 0 && (
                <Popconfirm title="确定删除选中的商品？" onConfirm={handleClearSelected}>
                  <Button danger size="small">删除选中</Button>
                </Popconfirm>
              )}
            </Space>
            <Space size="large">
              <span style={{ fontSize: 16 }}>
                已选 {selectedItems.length} 件，合计：
                <span style={{ color: token.colorError, fontSize: 20, fontWeight: 'bold' }}>
                  ¥{totalAmount.toFixed(2)}
                </span>
              </span>
              <Button type="primary" size="large" disabled={selectedItems.length === 0}
                      onClick={handleCheckout}>
                去结算
              </Button>
            </Space>
          </div>
        </>
      )}
    </div>
  );
}
