import { useEffect, useState } from 'react';
import { List, Tag, Typography } from 'antd';
import { FireOutlined, SearchOutlined } from '@ant-design/icons';
import { productApi } from '../../api/product';

interface Props {
  visible: boolean;
  keyword: string;
  onSelect: (keyword: string) => void;
}

export default function SearchSuggestions({ visible, keyword, onSelect }: Props) {
  const [hots, setHots] = useState<any[]>([]);
  const [suggestions, setSuggestions] = useState<any[]>([]);

  useEffect(() => {
    productApi.getHotKeywords().then(setHots).catch(() => {});
  }, []);

  useEffect(() => {
    if (keyword && keyword.trim().length > 0) {
      productApi.getSuggestions(keyword).then(setSuggestions).catch(() => []);
    } else {
      setSuggestions([]);
    }
  }, [keyword]);

  if (!visible) return null;

  const items = keyword.trim() ? suggestions : hots;

  return (
    <div style={{
      position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 1000,
      background: '#fff', borderRadius: 8, boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
      padding: 12, maxHeight: 300, overflowY: 'auto',
    }}>
      <Typography.Text type="secondary" style={{ fontSize: 12, marginBottom: 8, display: 'block' }}>
        {keyword.trim() ? <><SearchOutlined /> 搜索建议</> : <><FireOutlined /> 热门搜索</>}
      </Typography.Text>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {items.map((item: any) => (
          <Tag
            key={item.id || item.keyword}
            style={{ cursor: 'pointer', padding: '4px 12px' }}
            onClick={() => onSelect(item.keyword)}
            color={keyword.trim() ? 'default' : 'orange'}
          >
            {item.keyword}
            {item.searchCount != null && <span style={{ marginLeft: 4, color: '#999' }}>{item.searchCount}</span>}
          </Tag>
        ))}
      </div>
    </div>
  );
}
