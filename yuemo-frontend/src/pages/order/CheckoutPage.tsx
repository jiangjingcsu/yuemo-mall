import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Card, Radio, Button, Descriptions, Divider, Spin, message, Input, Empty, Tag, Image, Space, theme } from 'antd';
import type { RadioChangeEvent } from 'antd';
import { orderApi } from '@/api/order';
import { type CartItem } from '@/api/cart';
import request from '@/utils/request';

interface Address {
  id: string;
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: boolean;
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const stateItems = (location.state as { selectedItems?: CartItem[] } | null)?.selectedItems;
  const [items, setItems] = useState<CartItem[]>(stateItems ?? []);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const [remark, setRemark] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (stateItems && stateItems.length > 0) {
      request.get<Address[]>('/user/address/list')
        .then((addressData) => {
          setAddresses(addressData || []);
          const defaultAddr = (addressData || []).find((a) => a.isDefault);
          if (defaultAddr) {
            setSelectedAddressId(defaultAddr.id);
          } else if ((addressData || []).length > 0) {
            setSelectedAddressId(addressData[0].id);
          }
        })
        .catch(() => message.error('加载地址失败'))
        .finally(() => setIsLoading(false));
      return;
    }

    navigate('/cart');
  }, [stateItems, navigate]);

  const totalAmount = useMemo(
    () => items.reduce((sum, i) => sum + (i.subtotal || i.price * i.quantity), 0),
    [items]
  );

  const handleSubmit = useCallback(async () => {
    if (!selectedAddressId) {
      message.warning('请选择收货地址');
      return;
    }
    setIsSubmitting(true);
    try {
      const order = await orderApi.create({
        addressId: Number(selectedAddressId),
        items: items.map((i) => ({
          productId: i.productId,
          skuId: i.skuId,
          quantity: i.quantity,
        })),
        remark: remark || undefined,
      });
      message.success('订单创建成功');
      if (order?.id) {
        navigate(`/payment/${order.id}`);
      } else {
        navigate('/orders');
      }
    } catch {
      message.error('创建订单失败');
    } finally {
      setIsSubmitting(false);
    }
  }, [selectedAddressId, items, remark, navigate]);

  const handleAddressChange = useCallback((e: RadioChangeEvent) => {
    setSelectedAddressId(e.target.value);
  }, []);

  const handleGoToAddress = useCallback(() => navigate('/address'), [navigate]);
  const handleGoBack = useCallback(() => navigate('/cart'), [navigate]);
  const handleRemarkChange = useCallback((e: { target: { value: string } }) => {
    setRemark(e.target.value);
  }, []);

  if (isLoading) return <Spin style={{ display: 'block', margin: '100px auto' }} />;

  if (addresses.length === 0) {
    return (
      <div style={{ maxWidth: 800, margin: '40px auto' }}>
        <Card>
          <Empty description="请先添加收货地址">
            <Button type="primary" onClick={handleGoToAddress}>去添加地址</Button>
          </Empty>
        </Card>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      <Card title="确认订单">
        <h3 style={{ marginBottom: 16 }}>收货地址</h3>
        <Radio.Group
          value={selectedAddressId}
          onChange={handleAddressChange}
          style={{ width: '100%' }}
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            {addresses.map((addr) => (
              <Radio.Button
                key={addr.id}
                value={addr.id}
                style={{
                  width: '100%',
                  height: 'auto',
                  minHeight: 56,
                  display: 'flex',
                  alignItems: 'center',
                  padding: '12px 16px',
                }}
              >
                <span style={{ fontWeight: 500 }}>{addr.receiverName}</span>
                <span style={{ marginLeft: 12, color: token.colorTextSecondary }}>{addr.receiverPhone}</span>
                <span style={{ marginLeft: 12 }}>
                  {addr.province}{addr.city}{addr.district} {addr.detail}
                </span>
                {addr.isDefault && <Tag color="blue" style={{ marginLeft: 8 }}>默认</Tag>}
              </Radio.Button>
            ))}
          </Space>
        </Radio.Group>

        <Divider>商品清单</Divider>

        {items.map((item) => (
          <div key={item.skuId} style={{ display: 'flex', alignItems: 'center', marginBottom: 16, padding: '8px 0' }}>
            <Image
              src={item.productImage || '/placeholder.png'}
              width={64}
              height={64}
              style={{ objectFit: 'cover', borderRadius: 4, flexShrink: 0 }}
              fallback="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3QgZmlsbD0iI2VlZSIgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0Ii8+PHRleHQgZmlsbD0iIzk5OSIgZm9udC1zaXplPSIxMiIgeD0iNTAlIiB5PSI1MCUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7lm77niYc8L3RleHQ+PC9zdmc+"
            />
            <div style={{ marginLeft: 16, flex: 1 }}>
              <div style={{ fontWeight: 500 }}>{item.productName}</div>
              {item.specText && <Tag style={{ marginTop: 4 }}>{item.specText}</Tag>}
            </div>
            <div style={{ textAlign: 'right', minWidth: 120 }}>
              <div style={{ color: token.colorError }}>¥{item.price?.toFixed(2)}</div>
              <div style={{ color: token.colorTextTertiary }}>x{item.quantity}</div>
            </div>
          </div>
        ))}

        <Divider />

        <div style={{ marginBottom: 16 }}>
          <span style={{ marginRight: 8 }}>订单备注：</span>
          <Input
            value={remark}
            onChange={handleRemarkChange}
            placeholder="选填，可以告诉卖家您的特殊需求"
            style={{ width: 400 }}
            maxLength={200}
          />
        </div>

        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="商品数量">{items.length} 件</Descriptions.Item>
          <Descriptions.Item label="商品总额">
            <span style={{ color: token.colorError, fontSize: 20, fontWeight: 'bold' }}>
              ¥{totalAmount.toFixed(2)}
            </span>
          </Descriptions.Item>
        </Descriptions>

        <Divider />

        <div style={{ textAlign: 'right' }}>
          <Button style={{ marginRight: 16 }} onClick={handleGoBack}>
            返回购物车
          </Button>
          <Button type="primary" size="large" loading={isSubmitting} onClick={handleSubmit}>
            提交订单
          </Button>
        </div>
      </Card>
    </div>
  );
}
