import { useEffect, useState } from 'react';
import { Table, Tag, Button, message } from 'antd';
import { orderApi, Order } from '../../api/order';
import dayjs from 'dayjs';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'orange' },
  1: { label: '已支付', color: 'blue' },
  2: { label: '已发货', color: 'cyan' },
  3: { label: '已完成', color: 'green' },
  4: { label: '已取消', color: 'default' },
};

export default function OrderList() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await orderApi.getList({ page: 1, size: 50 });
      setOrders((data as any).records || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchOrders(); }, []);

  const handleCancel = async (id: number) => {
    try {
      await orderApi.cancel(id);
      message.success('已取消');
      fetchOrders();
    } catch { /* handled */ }
  };

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
    {
      title: '金额', dataIndex: 'payAmount', key: 'payAmount',
      render: (v: number) => `¥${v?.toFixed(2)}`,
    },
    {
      title: '状态', dataIndex: 'status', key: 'status',
      render: (v: number) => {
        const s = STATUS_MAP[v] || { label: '未知', color: 'default' };
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: '时间', dataIndex: 'createTime', key: 'createTime',
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作', key: 'action',
      render: (_: unknown, record: Order) =>
        record.status === 0 && (
          <Button size="small" danger onClick={() => handleCancel(record.id)}>
            取消
          </Button>
        ),
    },
  ];

  return <Table columns={columns} dataSource={orders} rowKey="id" loading={loading} />;
}
