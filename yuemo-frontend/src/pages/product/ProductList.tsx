import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Row, Col, Input, Spin, Empty, Pagination, Layout, Result, Button } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { productApi, ProductVO } from '../../api/product';
import ProductCard from '../../components/product/ProductCard';
import CategorySidebar from '../../components/product/CategorySidebar';
import ProductSortBar from '../../components/product/ProductSortBar';
import ProductFilters from '../../components/product/ProductFilters';
import SearchSuggestions from '../../components/product/SearchSuggestions';

const { Sider, Content } = Layout;

export default function ProductList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState<ProductVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [categories, setCategories] = useState<any[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const navigate = useNavigate();

  // Read filters from URL
  const keyword = searchParams.get('keyword') || '';
  const categoryId = searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined;
  const brandId = searchParams.get('brandId') ? Number(searchParams.get('brandId')) : undefined;
  const priceMin = searchParams.get('priceMin') ? Number(searchParams.get('priceMin')) : undefined;
  const priceMax = searchParams.get('priceMax') ? Number(searchParams.get('priceMax')) : undefined;
  const sortBy = searchParams.get('sortBy') || 'sales_desc';
  const page = searchParams.get('page') ? Number(searchParams.get('page')) : 1;
  const size = 20;

  const updateParams = useCallback((updates: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([k, v]) => {
      if (v === undefined || v === '') next.delete(k);
      else next.set(k, v);
    });
    if (updates.keyword !== undefined || updates.categoryId !== undefined ||
        updates.brandId !== undefined || updates.priceMin !== undefined ||
        updates.priceMax !== undefined || updates.sortBy !== undefined) {
      next.delete('page');
    }
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);

  const [error, setError] = useState<string | null>(null);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await productApi.getList({
        keyword: keyword || undefined,
        categoryId,
        brandId,
        priceMin,
        priceMax,
        sortBy: sortBy as any,
        page,
        size,
      });
      setProducts(data.records || []);
      setTotal(data.total || 0);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '加载商品失败，请稍后重试';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [keyword, categoryId, brandId, priceMin, priceMax, sortBy, page]);

  useEffect(() => { fetchProducts(); }, [fetchProducts]);
  useEffect(() => {
    productApi.getCategories().then(setCategories).catch(() => {});
  }, []);

  const handleSearch = (val: string) => {
    updateParams({ keyword: val || undefined });
    setShowSuggestions(false);
  };

  return (
    <div>
      {/* Search bar with suggestions */}
      <div style={{ position: 'relative', marginBottom: 24, maxWidth: 500 }}>
        <Input.Search
          placeholder="搜索商品"
          allowClear
          enterButton={<><SearchOutlined /> 搜索</>}
          size="large"
          value={keyword}
          onChange={(e) => {
            updateParams({ keyword: e.target.value });
            setShowSuggestions(true);
          }}
          onSearch={handleSearch}
          onFocus={() => setShowSuggestions(true)}
          onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        />
        <SearchSuggestions
          visible={showSuggestions}
          keyword={keyword}
          onSelect={(kw) => handleSearch(kw)}
        />
      </div>

      {/* Sort bar */}
      <ProductSortBar
        sortBy={sortBy}
        hasKeyword={!!keyword}
        onSortChange={(s) => updateParams({ sortBy: s })}
      />

      {/* Filters */}
      <ProductFilters
        brandId={brandId}
        priceMin={priceMin}
        priceMax={priceMax}
        onBrandChange={(id) => updateParams({ brandId: id !== undefined ? String(id) : undefined })}
        onPriceRangeChange={(min, max) => updateParams({
          priceMin: min !== undefined ? String(min) : undefined,
          priceMax: max !== undefined ? String(max) : undefined,
        })}
      />

      <Layout style={{ background: 'transparent', gap: 16 }}>
        {/* Category sidebar */}
        <Sider width={200} style={{ background: 'transparent' }}>
          <CategorySidebar
            categories={categories}
            activeId={categoryId}
            onSelect={(id) => updateParams({ categoryId: id !== null ? String(id) : undefined })}
          />
        </Sider>

        {/* Product grid */}
        <Content>
          <Spin spinning={loading}>
            {error ? (
              <Result
                status="error"
                title="加载失败"
                subTitle={error}
                extra={<Button type="primary" onClick={() => fetchProducts()}>重试</Button>}
              />
            ) : products.length === 0 && !loading ? (
              <Empty description="暂无商品" style={{ padding: 60 }} />
            ) : (
              <>
                <Row gutter={[16, 16]}>
                  {products.map(p => (
                    <Col key={p.id} xs={24} sm={12} md={8} lg={8} xl={6}>
                      <ProductCard product={p} onClick={(id) => navigate(`/product/${id}`)} />
                    </Col>
                  ))}
                </Row>
                {total > size && (
                  <div style={{ textAlign: 'center', marginTop: 24 }}>
                    <Pagination
                      current={page}
                      total={total}
                      pageSize={size}
                      onChange={(p) => updateParams({ page: String(p) })}
                      showTotal={(t) => `共 ${t} 件商品`}
                      showSizeChanger={false}
                    />
                  </div>
                )}
              </>
            )}
          </Spin>
        </Content>
      </Layout>
    </div>
  );
}
