import { Card, Tag, Badge } from 'antd';
import { ProductVO } from '../../api/product';

interface Props {
  product: ProductVO;
  onClick: (id: number) => void;
}

const stockStatusConfig: Record<string, { color: string; text: string }> = {
  sufficient: { color: '#52c41a', text: '现货充足' },
  low: { color: '#faad14', text: '库存紧张' },
  out_of_stock: { color: '#f5222d', text: '已售罄' },
};

export default function ProductCard({ product, onClick }: Props) {
  const stockCfg = stockStatusConfig[product.stockStatus] || stockStatusConfig.out_of_stock;

  return (
    <Card
      hoverable
      style={{ height: '100%' }}
      cover={
        <div style={{
          height: 200, background: '#fafafa', display: 'flex',
          alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
        }}>
          {product.mainImage ? (
            <img src={product.mainImage} alt={product.name}
              style={{ maxHeight: 200, maxWidth: '100%', objectFit: 'cover' }} />
          ) : (
            <span style={{ color: '#999' }}>暂无图片</span>
          )}
        </div>
      }
      onClick={() => onClick(product.id)}
    >
      <Card.Meta
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: 14, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 160 }}>
              {product.name}
            </span>
            <Badge color={stockCfg.color} text={stockCfg.text} />
          </div>
        }
        description={
          <div>
            <div style={{ color: '#f5222d', fontSize: 18, fontWeight: 'bold', margin: '8px 0 4px' }}>
              ¥{(product.minPrice ?? 0).toFixed(2)}
            </div>
            <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginBottom: 4 }}>
              {(product.tags || []).map(tag => (
                <Tag key={tag.id} color={tag.color} style={{ fontSize: 11 }}>{tag.name}</Tag>
              ))}
            </div>
            <span style={{ color: '#999', fontSize: 12 }}>已售 {product.sales}</span>
          </div>
        }
      />
    </Card>
  );
}
