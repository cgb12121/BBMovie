import api from './api';

export interface RecommendationItem {
    movieId: string;
    score: number;
    reason?: string;
}

interface RecommendationResponse {
    items: RecommendationItem[];
}

class PersonalizationService {
    async getUserRecommendations(userId: string, limit = 20): Promise<RecommendationItem[]> {
        const response = await api.get<RecommendationResponse>(
            `/api/personalization/v1/users/${encodeURIComponent(userId)}/recommendations`,
            { params: { limit } }
        );
        return Array.isArray(response.data?.items) ? response.data.items : [];
    }
}

const personalizationService = new PersonalizationService();
export default personalizationService;
