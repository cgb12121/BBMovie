import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Grid, List, Loader2, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { MovieCard } from '../components/MovieCard';
import api from '../services/api';

interface Movie {
    id: number;
    title: string;
    rating: number;
    posterUrl: string;
    genre?: string[];
    year?: number;
}

const Movies: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [selectedGenre, setSelectedGenre] = useState<string>('all');
    const [sortBy, setSortBy] = useState<string>('recent');
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

    useEffect(() => {
        fetchMovies();
    }, []);

    const fetchMovies = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.get('/api/movies');
            setMovies(response.data ?? []);
        } catch (err) {
            console.error('Error fetching movies:', err);
            setError('We could not load movies right now. Please try again shortly.');
            setMovies([]);
        } finally {
            setLoading(false);
        }
    };

    // Extract unique genres from movies
    const allGenres = Array.from(new Set(movies.flatMap(m => m.genre || [])));

    // Filter movies by genre
    const filteredMovies = movies.filter(movie => {
        if (selectedGenre === 'all') return true;
        return movie.genre?.includes(selectedGenre);
    });

    // Sort movies
    const sortedMovies = [...filteredMovies].sort((a, b) => {
        if (sortBy === 'recent') return (b.year || 0) - (a.year || 0);
        if (sortBy === 'title') return a.title.localeCompare(b.title);
        if (sortBy === 'rating') return b.rating - a.rating;
        return 0;
    });

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-7xl mx-auto space-y-8">
                {/* Header */}
                <div className="space-y-4">
                    <h1 className="text-white text-3xl md:text-4xl font-bold">Browse Movies</h1>
                    <p className="text-gray-400">Explore our complete collection of movies</p>
                </div>

                {/* Filters */}
                <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
                    <div className="flex flex-wrap items-center gap-3">
                        <Button
                            variant={selectedGenre === 'all' ? 'default' : 'outline'}
                            onClick={() => setSelectedGenre('all')}
                            className={selectedGenre === 'all' ? 'bg-red-600 hover:bg-red-700' : 'border-gray-700 text-gray-300 hover:bg-gray-800'}
                        >
                            All
                        </Button>
                        {allGenres.slice(0, 6).map((genre) => (
                            <Button
                                key={genre}
                                variant={selectedGenre === genre ? 'default' : 'outline'}
                                onClick={() => setSelectedGenre(genre)}
                                className={selectedGenre === genre ? 'bg-red-600 hover:bg-red-700' : 'border-gray-700 text-gray-300 hover:bg-gray-800'}
                            >
                                {genre}
                            </Button>
                        ))}
                    </div>

                    <div className="flex items-center gap-3">
                        <Select value={sortBy} onValueChange={setSortBy}>
                            <SelectTrigger className="w-[180px] bg-gray-900 border-gray-700 text-white">
                                <SelectValue placeholder="Sort by" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="recent">Most Recent</SelectItem>
                                <SelectItem value="title">Title (A-Z)</SelectItem>
                                <SelectItem value="rating">Rating</SelectItem>
                            </SelectContent>
                        </Select>

                        <div className="flex items-center gap-1 bg-gray-900 rounded-md p-1">
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setViewMode('grid')}
                                className={viewMode === 'grid' ? 'bg-gray-800 text-white' : 'text-gray-400'}
                            >
                                <Grid className="h-4 w-4" />
                            </Button>
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setViewMode('list')}
                                className={viewMode === 'list' ? 'bg-gray-800 text-white' : 'text-gray-400'}
                            >
                                <List className="h-4 w-4" />
                            </Button>
                        </div>
                    </div>
                </div>

                {/* Results Count */}
                <div className="flex items-center justify-between">
                    <p className="text-gray-400">
                        {sortedMovies.length} {sortedMovies.length === 1 ? 'movie' : 'movies'} found
                    </p>
                </div>

                {/* Loading State */}
                {loading && (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="h-12 w-12 text-red-600 animate-spin" />
                    </div>
                )}

                {/* Error State */}
                {error && (
                    <div className="flex flex-col items-center justify-center py-20 space-y-4">
                        <div className="bg-red-900/20 border border-red-900 rounded-full p-6">
                            <AlertCircle className="h-16 w-16 text-red-500" />
                        </div>
                        <h3 className="text-white text-xl font-semibold">Unable to fetch movies</h3>
                        <p className="text-gray-400 text-center max-w-md">{error}</p>
                        <Button onClick={fetchMovies} className="bg-red-600 hover:bg-red-700">
                            Retry
                        </Button>
                    </div>
                )}

                {/* Movies Grid */}
                {!loading && !error && (
                    <>
                        {sortedMovies.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-20 space-y-4">
                                <p className="text-gray-400 text-lg">No movies available</p>
                            </div>
                        ) : (
                            <div className={viewMode === 'grid' 
                                ? 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4'
                                : 'flex flex-col gap-4'
                            }>
                                {sortedMovies.map((movie) => (
                                    <MovieCard
                                        key={movie.id}
                                        movie={{
                                            id: movie.id,
                                            title: movie.title,
                                            thumbnail: movie.posterUrl
                                        }}
                                        onMovieClick={(id) => navigate(`/movies/${id}`)}
                                    />
                                ))}
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default Movies;
