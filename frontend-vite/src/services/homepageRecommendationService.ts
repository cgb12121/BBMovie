import api from './api';
import { apiCall } from './apiWrapper';

export interface HomepageTrendingEntry {
    movieId: string;
    score: number;
}

export interface HomepageTrendingResponse {
    items: HomepageTrendingEntry[];
}

export interface HomeMovie {
    id: number;
    title: string;
    rating: number;
    posterUrl: string;
    description?: string;
}

const toHomeMovie = (movie: any): HomeMovie | null => {
    if (!movie) return null;
    const id = movie.id ?? movie.movieId ?? movie._id;
    const numericId = Number(id);
    const title = movie.title ?? movie.name ?? '';
    if (!id || !title || Number.isNaN(numericId)) return null;

    return {
        id: numericId,
        title,
        rating: Number(movie.rating ?? movie.voteAverage ?? 0),
        posterUrl: movie.posterUrl ?? movie.thumbnail ?? movie.poster_path ?? '',
        description: movie.description ?? movie.overview
    };
};

class HomepageRecommendationService {
    async getTrending(limit = 10): Promise<HomepageTrendingEntry[]> {
        const response = await api.get<HomepageTrendingResponse>('/api/homepage/v1/trending', {
            params: { limit }
        });
        return response.data?.items ?? [];
    }

    async getTrendingMovies(limit = 10): Promise<HomeMovie[]> {
        try {
            const entries = await this.getTrending(limit);
            if (!entries.length) return [];

            const detailResponses = await Promise.allSettled(
                entries.map((entry) => apiCall.getMovieById(entry.movieId))
            );

            const movies: HomeMovie[] = detailResponses
                .map((result) => {
                    if (result.status !== 'fulfilled') return null;
                    const data = result.value;
                    if (!data.success) return null;
                    return toHomeMovie(data.data);
                })
                .filter((movie): movie is HomeMovie => !!movie);

            return movies;
        } catch {
            return [];
        }
    }
}

export const homepageRecommendationService = new HomepageRecommendationService();
