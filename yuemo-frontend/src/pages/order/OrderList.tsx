import { useCallback, useEffect, useState } from 'react';
import { Table, Tag, Button, Space, message, Popconfirm } from 'antd';
import { orderApi, type Order, type OrderPage } from '../../api/order';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'orange' },
  1: { label: '已支付', color: 'blue' },
  2: { label: '已发货', color: 'cyan' },
  3: { label: '已完成', color: 'green' },
  4: { label: '已取消', color: 'default' },
};

export default function OrderList() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);

  const fetchOrders = useCallback(async (p: number = page) => {
    setLoading(true);
    try {
      const data = await orderApi.getList({ page: p, size: 10 });
      setOrders(data.records || []);
      setTotal(data.total || 0);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { fetchOrders(1); }, []);

  const handleCancel = async (id: string) => {
    try {
      await orderApi.cancel(id);
      message.success('已取消');
      fetchOrders();
    } catch { /* handled */ }
  };

  const handleConfirm = async (id: string) => {
    try {
      await orderApi.confirm(id);
      message.success('已确认收货');
      fetchOrders();
    } catch { /* handled */ }
  };

  const handleDelete = async (id: string) => {
    try {
      await orderApi.delete(id);
      message.success('已删除');
      fetchOrders();
    } catch { /* handled */ }
  };

  const columns = [
    {
      title: '订单号', dataIndex: 'orderNo', key: 'orderNo',
      render: (v: string, r: Order) => (
        <a onClick={() => navigate(`/order/${r.id}`)}>{v}</a>
      ),
    },
    {
      title: '金额', dataIndex: 'payAmount', key: 'payAmount',
      render: (v: number) => `¥${(v ?? 0).toFixed(2)}`,
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
      render: (_: unknown, record: Order) => (
        <Space>
          {record.status === 0 && (
            <>
              <Button size="small" type="primary" onClick={() => navigate(`/payment/${record.id}`)}>去支付</Button>
              <Popconfirm title="确定取消此订单？" onConfirm={() => handleCancel(record.id)}>
                <Button size="small" danger>取消</Button>
              </Popconfirm>
            </>
          )}
          {record.status === 2 && (
            <Button size="small" type="primary" onClick={() => handleConfirm(record.id)}>确认收货</Button>
          )}
          {(record.status === 3 || record.status === 4) && (
            <Popconfirm title="确定删除此订单？" onConfirm={() => handleDelete(record.id)}>
              <Button size="small">删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table columns={columns} dataSource={orders} rowKey="id" loading={loading}
           pagination={{
             current: page,
             total,
             pageSize: 10,
             onChange: (p) => { setPage(p); fetchOrders(p); },
             showTotal: (t) => `共 ${t} 条`,
           }} />
  );
}
