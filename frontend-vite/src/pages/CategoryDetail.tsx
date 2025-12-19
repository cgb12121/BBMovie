import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Loader2, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/button';
import { MovieCard } from '../components/MovieCard';
import { ImageWithFallback } from '../components/ImageWithFallback';
import { apiCall } from '../services/apiWrapper';

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

            // For mock mode, we'll use the getMoviesByCategory API wrapper
            const moviesResponse = await apiCall.getMoviesByCategory(id);

            if (moviesResponse.success) {
                setMovies(moviesResponse.data);

                // For mock, create a default category based on the category ID
                const categories = [
                    { id: 1, name: "Action", description: "High-octane action packed movies", image: "https://image.tmdb.org/t/p/w1280/fIotZDG10hKkXJPPaJ44D3z2J4.jpg" },
                    { id: 2, name: "Drama", description: "Emotionally charged stories", image: "https://image.tmdb.org/t/p/w1280/1Dk3RJj3Qh5M1jJU3mzPZ1C4Vp.jpg" },
                    { id: 3, name: "Sci-Fi", description: "Science fiction movies", image: "https://image.tmdb.org/t/p/w1280/6MKU3N2jv69M8JqU8w19VQccUYA.jpg" },
                    { id: 4, name: "Comedy", description: "Light-hearted and funny movies", image: "https://image.tmdb.org/t/p/w1280/54nZyqQlC2YJgNEfVzLHnD9729.jpg" },
                    { id: 5, name: "Horror", description: "Scary and suspenseful movies", image: "https://image.tmdb.org/t/p/w1280/72diYJBobJ5P81H4lkq4H2P0XGA.jpg" },
                    { id: 6, name: "Romance", description: "Heartwarming love stories", image: "https://image.tmdb.org/t/p/w1280/3Jct8JAhHqQw5x0J8r5M2YJcY4.jpg" }
                ];

                const foundCategory = categories.find(cat => cat.id === parseInt(id));
                if (foundCategory) {
                    setCategory(foundCategory);
                } else {
                    setCategory({
                        id: parseInt(id),
                        name: `Category ${id}`,
                        description: `Movies in category ${id}`,
                        image: 'https://image.tmdb.org/t/p/w1280/fIotZDG10hKkXJPPaJ44D3z2J4.jpg' // Default category image
                    });
                }
            } else {
                setError(moviesResponse.message || 'Failed to load category movies');
                setMovies([]);
            }
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
