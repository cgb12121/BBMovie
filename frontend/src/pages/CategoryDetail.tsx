import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Loader2, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/button';
import { MovieCard } from '../components/MovieCard';
import { ImageWithFallback } from '../components/ImageWithFallback';
import api from '../services/api';

interface Category {
    id: number;
    name: string;
    description: string;
    image?: string;
}

interface Movie {
    id: number;
    title: string;
    description?: string;
    posterUrl?: string;
    rating?: number;
    year?: number;
    genre?: string[];
}

const CategoryDetail: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [category, setCategory] = useState<Category | null>(null);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchCategoryDetails = async () => {
        if (!id) return;
        try {
            setLoading(true);
            setError(null);
            const [categoryResponse, moviesResponse] = await Promise.all([
                api.get(`/api/categories/${id}`),
                api.get(`/api/categories/${id}/movies`)
            ]);
            setCategory(categoryResponse.data);
            setMovies(Array.isArray(moviesResponse.data) ? moviesResponse.data : []);
        } catch (err) {
            console.error('Failed to fetch category details', err);
            setError('We could not load this category at the moment. Please try again later.');
            setCategory(null);
            setMovies([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCategoryDetails();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    // Loading State
    if (loading) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center">
                <Loader2 className="h-12 w-12 text-red-600 animate-spin" />
            </div>
        );
    }

    // Error State
    if (error) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center px-4">
                <div className="flex flex-col items-center justify-center space-y-4 max-w-md text-center">
                    <div className="bg-red-900/20 border border-red-900 rounded-full p-6">
                        <AlertCircle className="h-16 w-16 text-red-500" />
                    </div>
                    <h3 className="text-white text-xl font-semibold">Category Unavailable</h3>
                    <p className="text-gray-400">{error}</p>
                    <Button onClick={fetchCategoryDetails} className="bg-red-600 hover:bg-red-700">
                        Retry
                    </Button>
                </div>
            </div>
        );
    }

    // Not Found State
    if (!category) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center">
                <p className="text-gray-400">Category not found</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-black">
            {/* Hero Section */}
            <div className="relative h-[50vh]">
                <div className="absolute inset-0">
                    {category.image ? (
                        <ImageWithFallback
                            src={category.image}
                            alt={category.name}
                            className="w-full h-full object-cover"
                        />
                    ) : (
                        <div className="w-full h-full bg-gradient-to-br from-red-900/20 to-black" />
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-black/70 to-black/30" />
                </div>

                <div className="relative h-full flex flex-col justify-end px-4 md:px-12 pb-12">
                    <Button
                        variant="ghost"
                        className="text-white hover:text-gray-300 gap-2 mb-6 self-start"
                        onClick={() => navigate('/categories')}
                    >
                        <ChevronLeft className="h-5 w-5" />
                        Back to Categories
                    </Button>

                    <div className="space-y-4">
                        <h1 className="text-white text-4xl md:text-6xl font-bold">{category.name}</h1>
                        <p className="text-gray-300 text-lg max-w-2xl">{category.description}</p>
                        <p className="text-gray-400">
                            {movies.length} {movies.length === 1 ? 'movie' : 'movies'} available
                        </p>
                    </div>
                </div>
            </div>

            {/* Movies Grid */}
            <div className="px-4 md:px-12 py-12">
                {movies.length === 0 ? (
                    <div className="text-center py-20">
                        <p className="text-gray-400 text-lg">No movies found in this category</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                        {movies.map((movie) => (
                            <MovieCard
                                key={movie.id}
                                movie={{
                                    id: movie.id,
                                    title: movie.title,
                                    thumbnail: movie.posterUrl || ''
                                }}
                                onMovieClick={(id) => navigate(`/movies/${id}`)}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default CategoryDetail; 