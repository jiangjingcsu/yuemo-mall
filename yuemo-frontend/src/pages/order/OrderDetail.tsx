import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Descriptions, Tag, Spin, Steps, Button, Space, message } from 'antd';
import { orderApi, Order } from '../../api/order';
import dayjs from 'dayjs';

const STATUS_MAP: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消' };

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchOrder = () => {
    if (!id) return;
    setLoading(true);
    orderApi.getDetail(Number(id))
      .then(setOrder)
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchOrder(); }, [id]);

  const handleConfirm = async () => {
    if (!order) return;
    try {
      await orderApi.confirm(order.id);
      message.success('已确认收货');
      fetchOrder();
    } catch { /* handled */ }
  };

  if (loading) return <Spin style={{ display: 'block', margin: '100px auto' }} />;
  if (!order) return <Card>订单不存在</Card>;

  const stepMap: Record<number, number> = { 0: 0, 1: 1, 2: 2, 3: 3, 4: -1 };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card title={`订单详情 #${order.orderNo}`}
        extra={
          <Space>
            {order.status === 0 && (
              <Button onClick={() => navigate(`/payment/${order.id}`)} type="primary">去支付</Button>
            )}
            {order.status === 2 && (
              <Button onClick={handleConfirm} type="primary">确认收货</Button>
            )}
          </Space>
        }>
        {stepMap[order.status] >= 0 && (
          <Steps current={stepMap[order.status]} style={{ marginBottom: 24 }} size="small"
            items={[
              { title: '待支付' },
              { title: '已支付' },
              { title: '已发货' },
              { title: '已完成' },
            ]}
          />
        )}
        <Descriptions column={2} bordered>
          <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={order.status === 4 ? 'default' : order.status === 3 ? 'green' : 'blue'}>
              {STATUS_MAP[order.status] || '未知'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="订单金额">¥{order.totalAmount?.toFixed(2)}</Descriptions.Item>
          <Descriptions.Item label="实付金额">¥{order.payAmount?.toFixed(2)}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{dayjs(order.createTime).format('YYYY-MM-DD HH:mm:ss')}</Descriptions.Item>
          <Descriptions.Item label="支付时间">
            {order.payTime ? dayjs(order.payTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          {order.deliveryTime && (
            <Descriptions.Item label="发货时间">
              {dayjs(order.deliveryTime).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
          )}
          {order.receiveTime && (
            <Descriptions.Item label="收货时间">
              {dayjs(order.receiveTime).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
          )}
          <Descriptions.Item label="备注">{order.remark || '-'}</Descriptions.Item>
          {order.logisticsCompany && (
            <>
              <Descriptions.Item label="物流公司">{order.logisticsCompany}</Descriptions.Item>
              <Descriptions.Item label="物流单号">{order.logisticsNo}</Descriptions.Item>
            </>
          )}
        </Descriptions>
      </Card>
    </div>
  );
}
