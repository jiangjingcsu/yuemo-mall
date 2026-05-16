import { Rate, Progress } from 'antd';
import { ReviewSummaryVO } from '../../api/product';

interface Props {
  summary: ReviewSummaryVO;
}

export default function ReviewSummary({ summary }: Props) {
  if (!summary || summary.totalCount === 0) {
    return <div style={{ color: '#999', padding: '16px 0' }}>暂无评价</div>;
  }

  const maxCount = Math.max(...Object.values(summary.ratingDistribution || {}), 1);

  return (
    <div style={{ display: 'flex', gap: 32, padding: '16px 0', flexWrap: 'wrap' }}>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 36, fontWeight: 'bold', color: '#f5222d' }}>
          {summary.averageRating?.toFixed(1) || '0.0'}
        </div>
        <Rate disabled value={summary.averageRating} allowHalf style={{ fontSize: 14 }} />
        <div style={{ color: '#999', marginTop: 4 }}>{summary.totalCount} 条评价</div>
      </div>
      <div style={{ flex: 1, minWidth: 200 }}>
        {[5, 4, 3, 2, 1].map(star => {
          const count = summary.ratingDistribution?.[star] || 0;
          return (
            <div key={star} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
              <span style={{ width: 30, textAlign: 'right' }}>{star}星</span>
              <Progress
                percent={maxCount > 0 ? (count / summary.totalCount) * 100 : 0}
                showInfo={false}
                size="small"
                strokeColor={star >= 4 ? '#52c41a' : star >= 3 ? '#faad14' : '#f5222d'}
                style={{ flex: 1 }}
              />
              <span style={{ width: 30, color: '#999' }}>{count}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
