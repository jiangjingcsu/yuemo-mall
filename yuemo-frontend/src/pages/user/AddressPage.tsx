import { useEffect, useState } from 'react';
import { Card, List, Button, Modal, Form, Input, message, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import request from '../../utils/request';

interface Address {
  id: number;
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: boolean;
}

export default function AddressPage() {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Address | null>(null);
  const [form] = Form.useForm();

  const fetchAddresses = async () => {
    setLoading(true);
    try {
      const data = await request.get('/user/address/list');
      setAddresses(data as any || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAddresses(); }, []);

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (addr: Address) => {
    setEditing(addr);
    form.setFieldsValue(addr);
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await request.delete(`/user/address/${id}`);
    message.success('已删除');
    fetchAddresses();
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    if (editing) {
      await request.put(`/user/address/${editing.id}`, values);
    } else {
      await request.post('/user/address', values);
    }
    message.success(editing ? '已更新' : '已添加');
    setModalOpen(false);
    fetchAddresses();
  };

  const handleSetDefault = async (id: number) => {
    await request.put(`/user/address/${id}/default`);
    message.success('已设为默认');
    fetchAddresses();
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card title="收货地址"
        extra={<Button icon={<PlusOutlined />} type="primary" onClick={handleAdd}>新增地址</Button>}
      >
        <List
          loading={loading}
          dataSource={addresses}
          locale={{ emptyText: '暂无地址，请添加' }}
          renderItem={(addr) => (
            <List.Item
              actions={[
                <Button icon={<EditOutlined />} size="small" onClick={() => handleEdit(addr)}>编辑</Button>,
                <Button icon={<DeleteOutlined />} size="small" danger onClick={() => handleDelete(addr.id)}>删除</Button>,
                !addr.isDefault && <Button size="small" onClick={() => handleSetDefault(addr.id)}>设为默认</Button>,
              ]}
            >
              <List.Item.Meta
                title={
                  <span>
                    {addr.receiverName} {addr.receiverPhone}
                    {addr.isDefault && <Tag color="blue" style={{ marginLeft: 8 }}>默认</Tag>}
                  </span>
                }
                description={`${addr.province || ''}${addr.city || ''}${addr.district || ''} ${addr.detail}`}
              />
            </List.Item>
          )}
        />
      </Card>

      <Modal title={editing ? '编辑地址' : '新增地址'} open={modalOpen} onOk={handleSave} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="receiverName" label="收货人" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="receiverPhone" label="联系电话" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="province" label="省份"><Input /></Form.Item>
          <Form.Item name="city" label="城市"><Input /></Form.Item>
          <Form.Item name="district" label="区县"><Input /></Form.Item>
          <Form.Item name="detail" label="详细地址" rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
