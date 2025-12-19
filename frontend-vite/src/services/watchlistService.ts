import api from './api';

export interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

export interface WatchlistCollection {
    id: string;
    name: string;
    description?: string;
    isPublic: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface WatchlistItem {
    id: string;
    movieId: string;
    status: string;
    notes?: string;
    addedAt: string;
    updatedAt: string;
}

export interface Page<T> {
    content: T[];
    page: number;
    size: number;
    totalPages: number;
    totalItems: number;
    hasNext: boolean;
    hasPrevious: boolean;
    nextPage?: number;
    prevPage?: number;
}

export interface CreateCollectionRequest {
    name: string;
    description?: string;
    isPublic: boolean;
}

export interface UpdateCollectionRequest extends CreateCollectionRequest {}

export interface UpsertItemRequest {
    movieId: string;
    status: string;
    notes?: string;
}

type RawCollection = {
    id: string;
    name: string;
    description?: string;
    public: boolean;
    createdAt: string;
    updatedAt: string;
};

type RawItem = {
    id: string;
    movieId: string;
    notes?: string;
    addedAt: string;
    updatedAt: string;
    watchStatus: string;
};

type RawPage<T> = {
    items: T[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
    nextPage?: number;
    prevPage?: number;
};

function mapCollection(raw: RawCollection): WatchlistCollection {
    return {
        id: raw.id,
        name: raw.name,
        description: raw.description ?? '',
        isPublic: raw.public,
        createdAt: raw.createdAt,
        updatedAt: raw.updatedAt,
    };
}

function mapItem(raw: RawItem): WatchlistItem {
    return {
        id: raw.id,
        movieId: raw.movieId,
        status: raw.watchStatus,
        notes: raw.notes ?? undefined,
        addedAt: raw.addedAt,
        updatedAt: raw.updatedAt,
    };
}

function mapPage<T, R>(raw: RawPage<R>, mapper: (value: R) => T): Page<T> {
    return {
        content: raw.items.map(mapper),
        page: raw.page,
        size: raw.size,
        totalItems: raw.totalItems,
        totalPages: raw.totalPages,
        hasNext: raw.hasNext,
        hasPrevious: raw.hasPrevious,
        nextPage: raw.nextPage,
        prevPage: raw.prevPage,
    };
}

const watchlistService = {
    async listCollections(page = 0, size = 20): Promise<Page<WatchlistCollection>> {
        const response = await api.get<ApiResponse<RawPage<RawCollection>>>(
            '/api/v1/watchlist/collections',
            { params: { page, size } }
        );
        return mapPage(response.data.data, mapCollection);
    },

    async createCollection(payload: CreateCollectionRequest): Promise<WatchlistCollection> {
        const response = await api.post<ApiResponse<RawCollection>>('/api/v1/watchlist/collections', payload);
        return mapCollection(response.data.data);
    },

    async updateCollection(id: string, payload: UpdateCollectionRequest): Promise<WatchlistCollection> {
        const response = await api.put<ApiResponse<RawCollection>>(`/api/v1/watchlist/collections/${id}`, payload);
        return mapCollection(response.data.data);
    },

    async deleteCollection(id: string): Promise<void> {
        await api.delete<ApiResponse<string>>(`/api/v1/watchlist/collections/${id}`);
    },

    async listItems(collectionId: string, page = 0, size = 20): Promise<Page<WatchlistItem>> {
        const response = await api.get<ApiResponse<RawPage<RawItem>>>(
            `/api/v1/watchlist/collections/${collectionId}/items`,
            { params: { page, size } }
        );
        return mapPage(response.data.data, mapItem);
    },

    async addItem(collectionId: string, payload: UpsertItemRequest): Promise<WatchlistItem> {
        const response = await api.post<ApiResponse<RawItem>>(
            `/api/v1/watchlist/collections/${collectionId}/items`,
            payload
        );
        return mapItem(response.data.data);
    },

    async updateItem(collectionId: string, movieId: string, payload: UpsertItemRequest): Promise<WatchlistItem> {
        const response = await api.put<ApiResponse<RawItem>>(
            `/api/v1/watchlist/collections/${collectionId}/items/${movieId}`,
            payload
        );
        return mapItem(response.data.data);
    },

    async deleteItem(collectionId: string, movieId: string): Promise<void> {
        await api.delete<ApiResponse<string>>(`/api/v1/watchlist/collections/${collectionId}/items/${movieId}`);
    }
};

export default watchlistService;
