import { Input, Select, Space, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { productApi } from '../../api/product';

interface Brand {
  id: number;
  name: string;
}

interface Props {
  brandId?: number;
  priceMin?: number;
  priceMax?: number;
  onBrandChange: (id: number | undefined) => void;
  onPriceRangeChange: (min: number | undefined, max: number | undefined) => void;
}

export default function ProductFilters({ brandId, priceMin, priceMax, onBrandChange, onPriceRangeChange }: Props) {
  const [brands, setBrands] = useState<Brand[]>([]);

  useEffect(() => {
    productApi.getBrands().then(setBrands).catch(() => {});
  }, []);

  return (
    <Space wrap style={{ marginBottom: 12 }}>
      <Select
        allowClear
        placeholder="选择品牌"
        value={brandId}
        onChange={(v) => onBrandChange(v || undefined)}
        style={{ minWidth: 120 }}
        options={brands.map(b => ({ label: b.name, value: b.id }))}
      />
      <Input
        placeholder="最低价"
        type="number"
        value={priceMin ?? ''}
        onChange={e => onPriceRangeChange(e.target.value ? Number(e.target.value) : undefined, priceMax)}
        style={{ width: 100 }}
      />
      <span>-</span>
      <Input
        placeholder="最高价"
        type="number"
        value={priceMax ?? ''}
        onChange={e => onPriceRangeChange(priceMin, e.target.value ? Number(e.target.value) : undefined)}
        style={{ width: 100 }}
      />
    </Space>
  );
}
