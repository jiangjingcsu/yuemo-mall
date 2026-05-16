import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Radio, Button, Space, message, Descriptions, Divider } from 'antd';
import { WechatOutlined, AlipayCircleOutlined } from '@ant-design/icons';
import request from '../../utils/request';

export default function PaymentPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const [payType, setPayType] = useState(1);
  const [paying, setPaying] = useState(false);
  const navigate = useNavigate();

  const handlePay = async () => {
    setPaying(true);
    try {
      await request.post('/payment/pay', null, { params: { orderId } });
      message.success('支付请求已提交，请完成支付');
      navigate('/orders');
    } catch {
      // handled
    } finally {
      setPaying(false);
    }
  };

  return (
    <div style={{ maxWidth: 600, margin: '40px auto' }}>
      <Card title="订单支付">
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="订单编号">{orderId}</Descriptions.Item>
          <Descriptions.Item label="支付金额">
            <span style={{ color: '#f5222d', fontSize: 20, fontWeight: 'bold' }}>
              ¥--.-- (实际金额由后端返回)
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
