import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Radio, Button, Space, message, Descriptions, Divider, Spin, Tag, Result } from 'antd';
import { WechatOutlined, AlipayCircleOutlined, WalletOutlined } from '@ant-design/icons';
import { paymentApi } from '../../api/payment';
import { orderApi, type Order } from '../../api/order';
import { userApi } from '../../api/user';

export default function PaymentPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const [payType, setPayType] = useState(1);
  const [paying, setPaying] = useState(false);
  const [order, setOrder] = useState<Order | null>(null);
  const [balance, setBalance] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!orderId) {
      setError('缺少订单ID');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    orderApi.getDetail(orderId)
      .then((orderData) => {
        setOrder(orderData);
        return userApi.getBalance();
      })
      .then((balanceData) => {
        setBalance(balanceData ?? 0);
      })
      .catch((err) => {
        if (!order) {
          setError('加载订单信息失败，请返回重试');
        }
      })
      .finally(() => setLoading(false));
  }, [orderId]);

  const balanceInsufficient = order != null && balance < order.payAmount;

  const handlePay = async () => {
    if (!orderId || !order) return;
    setPaying(true);
    try {
      await paymentApi.pay(orderId!, payType);
      if (payType === 3) {
        message.success('余额支付成功');
      } else {
        message.success('支付请求已提交，请完成支付');
      }
      navigate('/orders');
    } catch {
      message.error('支付失败，请重试');
    } finally {
      setPaying(false);
    }
  };

  if (loading) return <Spin style={{ display: 'block', margin: '100px auto' }} />;

  if (error) {
    return (
      <Result
        status="error"
        title={error}
        extra={<Button type="primary" onClick={() => navigate('/orders')}>返回订单列表</Button>}
      />
    );
  }

  if (!order) {
    return (
      <Result
        status="warning"
        title="订单信息获取失败"
        extra={<Button type="primary" onClick={() => navigate('/orders')}>返回订单列表</Button>}
      />
    );
  }

  if (order.status !== 0) {
    const statusText: Record<number, string> = { 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消' };
    return (
      <Result
        status="warning"
        title={`订单${statusText[order.status] ?? '状态异常'}`}
        subTitle="该订单无法进行支付"
        extra={<Button type="primary" onClick={() => navigate('/orders')}>返回订单列表</Button>}
      />
    );
  }

  return (
    <div style={{ maxWidth: 600, margin: '40px auto' }}>
      <Card title="订单支付">
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="订单编号">{order.orderNo || orderId}</Descriptions.Item>
          <Descriptions.Item label="支付金额">
            <span style={{ color: '#f5222d', fontSize: 20, fontWeight: 'bold' }}>
              ¥{(order.payAmount ?? 0).toFixed(2)}
            </span>
          </Descriptions.Item>
        </Descriptions>

        <Divider>选择支付方式</Divider>

        <Radio.Group value={payType} onChange={(e) => setPayType(e.target.value)}
                     style={{ width: '100%' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Radio.Button value={1} style={{ width: '100%', height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <WechatOutlined style={{ fontSize: 24, color: '#52c41a', marginRight: 8 }} />
              微信支付
            </Radio.Button>
            <Radio.Button value={2} style={{ width: '100%', height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <AlipayCircleOutlined style={{ fontSize: 24, color: '#1677ff', marginRight: 8 }} />
              支付宝
            </Radio.Button>
            <Radio.Button
              value={3}
              disabled={balanceInsufficient}
              style={{ width: '100%', height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <WalletOutlined style={{ fontSize: 24, color: '#faad14', marginRight: 8 }} />
              平台余额
              <Tag color={balanceInsufficient ? 'red' : 'green'} style={{ marginLeft: 8 }}>
                ¥{balance.toFixed(2)}
              </Tag>
              {balanceInsufficient && <span style={{ color: '#ff4d4f', fontSize: 12, marginLeft: 4 }}>余额不足</span>}
            </Radio.Button>
          </Space>
        </Radio.Group>

        <Divider />
        <Button type="primary" size="large" block loading={paying}
                onClick={handlePay}>
          确认支付
        </Button>
      </Card>
    </div>
  );
}
