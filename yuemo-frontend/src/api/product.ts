import request from '../utils/request';

export interface ProductVO {
  id: number;
  name: string;
  categoryId: number;
  brandId: number;
  brandName: string;
  title: string;
  minPrice: number;
  totalStock: number;
  stockStatus: 'sufficient' | 'low' | 'out_of_stock';
  mainImage: string;
  sales: number;
  status: number;
  tags: TagVO[];
  createTime: string;
}

export interface ProductDetailVO {
  id: number;
  name: string;
  categoryId: number;
  categoryName: string;
  brandId: number;
  brandName: string;
  title: string;
  description: string;
  minPrice: number;
  maxPrice: number;
  totalStock: number;
  mainImage: string;
  images: string[];
  sales: number;
  status: number;
  tags: TagVO[];
  specGroups: SpecGroupVO[];
  skus: SkuVO[];
  reviewSummary: ReviewSummaryVO;
}

export interface SkuVO {
  id: number;
  skuCode: string;
  specIds: number[];
  specText: string;
  price: number;
  stock: number;
  image: string;
}

export interface SpecGroupVO {
  templateId: number;
  name: string;
  values: SpecValueVO[];
}

export interface SpecValueVO {
  id: number;
  value: string;
}

export interface TagVO {
  id: number;
  name: string;
  color: string;
}

export interface ReviewSummaryVO {
  averageRating: number;
  totalCount: number;
  ratingDistribution: Record<number, number>;
}

export interface ProductSearchParams {
  keyword?: string;
  categoryId?: number;
  brandId?: number;
  priceMin?: number;
  priceMax?: number;
  sortBy?: 'price_asc' | 'price_desc' | 'sales_desc' | 'time_desc' | 'relevance';
  page: number;
  size: number;
}

export const productApi = {
  getList: (params: ProductSearchParams) =>
    request.get<{ records: ProductVO[]; total: number; current: number; pages: number }>('/product/list', { params }),

  getDetail: (id: number) => request.get<ProductDetailVO>(`/product/${id}`),

  getCategories: () => request.get<{ id: number; name: string }[]>('/product/category/list'),

  getBrands: () => request.get<{ id: number; name: string }[]>('/product/brand/list'),

  getHotKeywords: () => request.get<{ id: number; keyword: string }[]>('/product/search/hot'),

  getSuggestions: (keyword: string) => request.get<{ id: number; keyword: string }[]>('/product/search/suggest', { params: { keyword } }),

  getReviews: (productId: number, page: number, size: number) =>
    request.get<{ records: { id: number; rating: number; content: string; images: string; reply: string; createTime: string }[]; total: number }>(`/product/${productId}/reviews`, { params: { page, size } }),

  getReviewSummary: (productId: number) =>
    request.get<ReviewSummaryVO>(`/product/${productId}/review-summary`),
};
