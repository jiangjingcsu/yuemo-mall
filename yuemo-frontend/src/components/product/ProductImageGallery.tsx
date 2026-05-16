import { useState } from 'react';

interface Props {
  mainImage: string;
  images: string[];
}

export default function ProductImageGallery({ mainImage, images }: Props) {
  const allImages = images && images.length > 0 ? images : [mainImage];
  const [activeIndex, setActiveIndex] = useState(0);

  return (
    <div>
      <div style={{
        width: 400, height: 400, background: '#fafafa', display: 'flex',
        alignItems: 'center', justifyContent: 'center', borderRadius: 8,
        marginBottom: 12, border: '1px solid #f0f0f0', overflow: 'hidden',
      }}>
        <img src={allImages[activeIndex]} alt="商品图片"
          style={{ maxWidth: 400, maxHeight: 400, objectFit: 'contain' }} />
      </div>
      {allImages.length > 1 && (
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto' }}>
          {allImages.map((img, i) => (
            <div key={i}
              onClick={() => setActiveIndex(i)}
              style={{
                width: 64, height: 64, cursor: 'pointer', borderRadius: 4,
                border: i === activeIndex ? '2px solid #1677ff' : '1px solid #e8e8e8',
                overflow: 'hidden', flexShrink: 0,
              }}>
              <img src={img} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
