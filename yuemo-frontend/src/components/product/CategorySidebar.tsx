import { Menu } from 'antd';
import { AppstoreOutlined } from '@ant-design/icons';

interface Category {
  id: number;
  name: string;
  children?: Category[];
}

interface Props {
  categories: Category[];
  activeId?: number;
  onSelect: (id: number | null) => void;
}

export default function CategorySidebar({ categories, activeId, onSelect }: Props) {
  const buildItems = (cats: Category[]): any[] => {
    if (!cats) return [];
    return cats.map(cat => ({
      key: String(cat.id),
      label: cat.name,
      icon: <AppstoreOutlined />,
      children: cat.children && cat.children.length > 0 ? buildItems(cat.children) : undefined,
    }));
  };

  return (
    <div style={{ background: '#fff', borderRadius: 8, padding: '8px 0' }}>
      <div style={{ padding: '8px 16px', fontWeight: 600, fontSize: 15, color: '#333', borderBottom: '1px solid #f0f0f0' }}>
        商品分类
      </div>
      <div
        onClick={() => onSelect(null)}
        style={{
          padding: '8px 24px', cursor: 'pointer', color: activeId == null ? '#1677ff' : '#333',
          fontWeight: activeId == null ? 600 : 400, background: activeId == null ? '#e6f4ff' : 'transparent',
          transition: 'all 0.2s',
        }}>
        全部商品
      </div>
      <Menu
        mode="inline"
        selectedKeys={activeId ? [String(activeId)] : []}
        items={buildItems(categories)}
        onClick={({ key }) => onSelect(Number(key))}
        style={{ border: 'none' }}
      />
    </div>
  );
}
