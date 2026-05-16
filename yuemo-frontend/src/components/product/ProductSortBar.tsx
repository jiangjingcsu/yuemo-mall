import { Segmented } from 'antd';
import type { SegmentedValue } from 'antd/es/segmented';

interface Props {
  sortBy: string;
  hasKeyword: boolean;
  onSortChange: (sort: string) => void;
}

const sortOptions = (hasKeyword: boolean) => [
  { label: '综合', value: 'sales_desc' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' },
  { label: '销量', value: 'sales_desc' },
  { label: '最新', value: 'time_desc' },
  ...(hasKeyword ? [{ label: '相关性', value: 'relevance' }] : []),
];

export default function ProductSortBar({ sortBy, hasKeyword, onSortChange }: Props) {
  const handleChange = (val: SegmentedValue) => {
    onSortChange(val as string);
  };

  return (
    <Segmented
      options={sortOptions(hasKeyword)}
      value={sortBy}
      onChange={handleChange}
      style={{ marginBottom: 16 }}
    />
  );
}
