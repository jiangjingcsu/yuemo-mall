import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Button, InputNumber, Spin, message, Tag, Descriptions, Tabs } from 'antd';
import { ShoppingCartOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { productApi, ProductDetailVO, SkuVO, ReviewSummaryVO } from '../../api/product';
import { cartApi } from '../../api/cart';
import ProductImageGallery from '../../components/product/ProductImageGallery';
import SkuSelector from '../../components/product/SkuSelector';
import ReviewSummary from '../../components/product/ReviewSummary';

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<ProductDetailVO | null>(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [selectedSku, setSelectedSku] = useState<SkuVO | null>(null);
  const [reviewSummary, setReviewSummary] = useState<ReviewSummaryVO | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    Promise.all([
      productApi.getDetail(Number(id)),
      productApi.getReviewSummary(Number(id)),
    ])
      .then(([productData, reviewData]) => {
        setProduct(productData);
        setReviewSummary(reviewData);
        // Auto-select first available SKU
        if (productData.skus && productData.skus.length > 0) {
          setSelectedSku(productData.skus[0]);
        }
      })
      .finally(() => setLoading(false));
  }, [id]);

  const displayPrice = selectedSku?.price ?? product?.minPrice ?? 0;
  const displayStock = selectedSku?.stock ?? product?.totalStock ?? 0;

  const handleAddToCart = async () => {
    const token = localStorage.getItem('token');
    if (!token) { navigate('/login'); return; }
    try {
      await cartApi.add(Number(id), quantity);
      message.success('已加入购物车');
    } catch { /* handled */ }
  };

  if (loading) return <Spin style={{ display: 'block', margin: '100px auto' }} />;
  if (!product) return <Card>商品不存在</Card>;

  const reviewTab = reviewSummary ? (
    <div>
      <ReviewSummary summary={reviewSummary} />
      {/* Reviews list would be paginated here */}
    </div>
  ) : null;

  return (
    <Card>
      <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
        <ProductImageGallery
          mainImage={product.mainImage}
          images={(selectedSku?.image && selectedSku.image !== product.mainImage)
            ? [selectedSku.image, ...product.images]
            : product.images}
        />
        <div style={{ flex: 1, minWidth: 300 }}>
          <h1 style={{ marginBottom: 4 }}>{product.name}</h1>
          <p style={{ color: '#999', marginBottom: 8 }}>{product.title}</p>

          {/* Tags */}
          <div style={{ marginBottom: 12 }}>
            {(product.tags || []).map(tag => (
              <Tag key={tag.id} color={tag.color}>{tag.name}</Tag>
            ))}
          </div>

          {/* Price */}
          <div style={{ background: '#fff2f0', padding: 16, borderRadius: 8, margin: '16px 0' }}>
            <span style={{ fontSize: 28, color: '#f5222d', fontWeight: 'bold' }}>
              ¥{displayPrice?.toFixed(2)}
            </span>
            {product.minPrice !== product.maxPrice && (
              <span style={{ color: '#999', marginLeft: 8 }}>
                ¥{product.minPrice?.toFixed(2)} - ¥{product.maxPrice?.toFixed(2)}
              </span>
            )}
          </div>

          {/* SKU Selector */}
          <SkuSelector
            specGroups={product.specGroups || []}
            skus={product.skus || []}
            onSkuChange={(sku) => setSelectedSku(sku)}
          />

          {/* Info */}
          <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="库存">
              {displayStock > 0 ? (
                <span style={{ color: displayStock < 10 ? '#faad14' : '#52c41a' }}>
                  {displayStock} 件{displayStock < 10 ? '（库存紧张）' : ''}
                </span>
              ) : (
                <span style={{ color: '#f5222d' }}>已售罄</span>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="销量">已售 {product.sales} 件</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={product.status === 1 ? 'green' : 'default'}>
                {product.status === 1 ? '在售' : '已下架'}
              </Tag>
            </Descriptions.Item>
            {product.brandName && (
              <Descriptions.Item label="品牌">{product.brandName}</Descriptions.Item>
            )}
            {product.categoryName && (
              <Descriptions.Item label="分类">{product.categoryName}</Descriptions.Item>
            )}
          </Descriptions>

          {/* Action buttons */}
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <InputNumber
              min={1}
              max={displayStock}
              value={quantity}
              onChange={(v) => setQuantity(v || 1)}
              disabled={displayStock === 0}
            />
            <Button
              icon={<ShoppingCartOutlined />}
              size="large"
              onClick={handleAddToCart}
              disabled={displayStock === 0}
            >
              加入购物车
            </Button>
            <Button
              type="primary"
              icon={<ThunderboltOutlined />}
              size="large"
              danger
              disabled={displayStock === 0}
            >
              立即购买
            </Button>
          </div>
        </div>
      </div>

      {/* Description and Reviews */}
      <Tabs
        style={{ marginTop: 32 }}
        items={[
          {
            key: 'desc',
            label: '商品描述',
            children: (
              <div style={{ padding: '16px 0' }}>
                {product.description ? (
                  <div dangerouslySetInnerHTML={{ __html: product.description }} />
                ) : (
                  <p style={{ color: '#999' }}>暂无商品描述</p>
                )}
              </div>
            ),
          },
          {
            key: 'reviews',
            label: `商品评价 (${reviewSummary?.totalCount || 0})`,
            children: reviewTab,
          },
        ]}
      />
    </Card>
  );
}
