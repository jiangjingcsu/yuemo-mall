import { useEffect, useState } from 'react';
import { Card, Row, Col, Button, message, Tag, Spin, Empty } from 'antd';
import { couponApi } from '../../api/coupon';

interface Coupon {
  id: number;
  name: string;
  type: number;
  threshold: number;
  value: number;
  startTime: string;
  endTime: string;
  status: number;
}

export default function CouponList() {
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    couponApi.getList({ page: 1, size: 100 })
      .then((data: any) => setCoupons(data.records || []))
      .finally(() => setLoading(false));
  }, []);

  const handleReceive = async (id: number) => {
    try {
      await couponApi.receive(id);
      message.success('领取成功');
    } catch { /* handled */ }
  };

  return (
    <div>
      <h2>优惠券中心</h2>
      <Spin spinning={loading}>
        {coupons.length === 0 && !loading ? (
          <Empty description="暂无优惠券" />
        ) : (
          <Row gutter={[16, 16]}>
            {coupons.map((c) => (
              <Col key={c.id} xs={24} sm={12} md={8}>
                <Card>
                  <h3>{c.name}</h3>
                  <div style={{ fontSize: 24, color: '#f5222d', fontWeight: 'bold', margin: '12px 0' }}>
                    {c.type === 2 ? `${c.value}折` : `¥${c.value}`}
                  </div>
                  {c.threshold > 0 && (
                    <div style={{ color: '#999' }}>满 ¥{c.threshold} 可用</div>
                  )}
                  <div style={{ marginTop: 8 }}>
                    <Tag color={c.status === 1 ? 'green' : 'default'}>
                      {c.status === 0 ? '未开始' : c.status === 1 ? '进行中' : '已结束'}
                    </Tag>
                  </div>
                  <Button type="primary" block style={{ marginTop: 12 }}
                          disabled={c.status !== 1}
                          onClick={() => handleReceive(c.id)}>
                    立即领取
                  </Button>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Spin>
    </div>
  );
}
