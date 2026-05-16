import { useEffect, useState } from 'react';
import { Card, Descriptions, Avatar, Button, Form, Input, Modal, message } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { userApi, UserInfo } from '../../api/user';

export default function UserCenter() {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchUser = async () => {
    setLoading(true);
    try {
      const data = await userApi.getUserInfo();
      setUser(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchUser(); }, []);

  const handleEdit = () => {
    form.setFieldsValue(user);
    setEditOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    // await userApi.updateUserInfo(values);
    message.success('信息已更新');
    setEditOpen(false);
    fetchUser();
  };

  if (loading) return <Card loading />;

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card title="个人中心">
        <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 24 }}>
          <Avatar size={80} icon={<UserOutlined />} src={user?.avatar} />
          <div>
            <h2>{user?.nickname || user?.username}</h2>
            <Button type="primary" ghost onClick={handleEdit}>编辑资料</Button>
          </div>
        </div>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="用户名">{user?.username}</Descriptions.Item>
          <Descriptions.Item label="昵称">{user?.nickname || '-'}</Descriptions.Item>
          <Descriptions.Item label="手机号">{user?.phone || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Modal title="编辑资料" open={editOpen} onOk={handleSave} onCancel={() => setEditOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="nickname" label="昵称">
            <Input placeholder="请输入昵称" />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input placeholder="请输入手机号" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
