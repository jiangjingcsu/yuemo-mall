import { useState } from 'react';
import { Card, Tabs, Table, Button, Tag, Space, message, Modal, Form, Input, InputNumber } from 'antd';
import request from '../../utils/request';

const { TabPane } = Tabs;

export default function AdminDashboard() {
  return (
    <div>
      <Card title="后台管理">
        <Tabs defaultActiveKey="products">
          <TabPane tab="商品管理" key="products">
            <ProductManager />
          </TabPane>
          <TabPane tab="优惠券管理" key="coupons">
            <CouponManager />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
}

function ProductManager() {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await request.get('/product/list', { params: { page: 1, size: 50 } });
      setProducts((data as any)?.records || []);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    const values = await form.validateFields();
    await request.post('/admin/product', values);
    message.success('商品已创建');
    setModalOpen(false);
    fetchProducts();
  };

  return (
    <div>
      <Button type="primary" onClick={() => setModalOpen(true)} style={{ marginBottom: 16 }}>
        新增商品
      </Button>
      <Table dataSource={products} rowKey="id" loading={loading}
        columns={[
          { title: 'ID', dataIndex: 'id' },
          { title: '名称', dataIndex: 'name' },
          { title: '价格', dataIndex: 'price', render: (v: number) => `¥${v?.toFixed(2)}` },
          { title: '库存', dataIndex: 'stock' },
          {
            title: '状态', dataIndex: 'status',
            render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '上架' : '下架'}</Tag>,
          },
        ]}
      />
      <Modal title="新增商品" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="商品名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="price" label="价格" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="stock" label="库存" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function CouponManager() {
  const [coupons, setCoupons] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchCoupons = async () => {
    setLoading(true);
    try {
      const data = await request.get('/coupon/list', { params: { page: 1, size: 50 } });
      setCoupons((data as any)?.records || []);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    const values = await form.validateFields();
    await request.post('/admin/coupon', values);
    message.success('优惠券已创建');
    setModalOpen(false);
    fetchCoupons();
  };

  return (
    <div>
      <Button type="primary" onClick={() => setModalOpen(true)} style={{ marginBottom: 16 }}>
        创建优惠券
      </Button>
      <Table dataSource={coupons} rowKey="id" loading={loading}
        columns={[
          { title: 'ID', dataIndex: 'id' },
          { title: '名称', dataIndex: 'name' },
          { title: '类型', dataIndex: 'type', render: (v: number) => ['', '满减', '折扣', '立减'][v] || '未知' },
          { title: '面值', dataIndex: 'value' },
          { title: '已领/总量', render: (_: any, r: any) => `${r.receivedCount}/${r.totalCount}` },
        ]}
      />
      <Modal title="创建优惠券" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}>
            <InputNumber min={1} max={3} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="value" label="优惠值" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="threshold" label="门槛（0=无门槛）"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="totalCount" label="总量" rules={[{ required: true }]}><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
