import { useState, useMemo } from 'react';
import { Button, Space, Typography } from 'antd';
import { SpecGroupVO, SkuVO } from '../../api/product';

interface Props {
  specGroups: SpecGroupVO[];
  skus: SkuVO[];
  onSkuChange: (sku: SkuVO | null) => void;
}

export default function SkuSelector({ specGroups, skus, onSkuChange }: Props) {
  const [selected, setSelected] = useState<Record<number, number>>({});

  const matchedSku = useMemo(() => {
    const selectedIds = Object.values(selected);
    if (selectedIds.length === 0) return null;
    return skus.find(sku => {
      if (!sku.specIds || sku.specIds.length === 0) return false;
      return selectedIds.every(id => sku.specIds.includes(id));
    }) || null;
  }, [selected, skus]);

  // Compute selectable values: which spec values still have available SKUs
  const selectableMap = useMemo(() => {
    const map: Record<number, Set<number>> = {};
    if (Object.keys(selected).length === 0) {
      // All values are selectable
      skus.forEach(sku => {
        (sku.specIds || []).forEach(specId => {
          if (!map[specId]) map[specId] = new Set();
          map[specId].add(specId);
        });
      });
      return map;
    }
    // Filter SKUs that match current selection (except one spec)
    const selectedIds = Object.values(selected);
    return {};
  }, [selected, skus]);

  const handleSelect = (templateId: number, valueId: number) => {
    const next = { ...selected, [templateId]: valueId };
    setSelected(next);
    // Find matching SKU
    const selectedIds = Object.values(next);
    const found = skus.find(sku =>
      (sku.specIds || []).length === selectedIds.length &&
      selectedIds.every(id => (sku.specIds || []).includes(id))
    );
    onSkuChange(found || null);
  };

  if (!specGroups || specGroups.length === 0) return null;

  return (
    <div style={{ marginBottom: 16 }}>
      {specGroups.map(group => (
        <div key={group.templateId} style={{ marginBottom: 12 }}>
          <Typography.Text strong style={{ marginRight: 12, display: 'inline-block', width: 40 }}>
            {group.name}
          </Typography.Text>
          <Space wrap>
            {group.values.map(v => {
              const isSelected = selected[group.templateId] === v.id;
              return (
                <Button
                  key={v.id}
                  type={isSelected ? 'primary' : 'default'}
                  size="small"
                  onClick={() => handleSelect(group.templateId, v.id)}
                >
                  {v.value}
                </Button>
              );
            })}
          </Space>
        </div>
      ))}
      {matchedSku && (
        <div style={{ marginTop: 8, color: '#f5222d', fontSize: 16 }}>
          已选：{matchedSku.specText} — ¥{matchedSku.price.toFixed(2)} — 库存 {matchedSku.stock} 件
        </div>
      )}
    </div>
  );
}
