import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Search, X, Loader2, AlertCircle } from 'lucide-react';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { MovieCard } from '../components/MovieCard';
import api from '../services/api';

type Movie = {
    id: string;
    title: string;
    description?: string;
    posterUrl?: string;
    rating?: number;
    year?: number;
    genre?: string[];
};

const SearchResults: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const urlQuery = new URLSearchParams(location.search).get('query') ?? '';
    
    const [searchQuery, setSearchQuery] = useState(urlQuery);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setSearchQuery(urlQuery);
    }, [urlQuery]);

    useEffect(() => {
        if (searchQuery) {
            fetchMovies();
        } else {
            setMovies([]);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchQuery]);

    const fetchMovies = async () => {
        if (!searchQuery.trim()) {
            setMovies([]);
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const response = await api.get('/api/search/similar-search', {
                params: { query: searchQuery }
            });
            setMovies(Array.isArray(response.data) ? response.data : []);
        } catch (error) {
            console.error('Error searching movies:', error);
            setError('We could not load search results. Please try again.');
            setMovies([]);
        } finally {
            setLoading(false);
        }
    };

    const popularSearches = ['Action', 'Sci-Fi', 'Drama', 'Comedy', 'Thriller'];

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-7xl mx-auto space-y-8">
                {/* Search Header */}
                <div className="flex items-center gap-4">
                    <div className="relative flex-1">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                        <Input
                            type="text"
                            placeholder="Search for movies, actors, directors..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="pl-12 pr-12 py-6 bg-gray-900 border-gray-800 text-white placeholder:text-gray-500 text-lg"
                            autoFocus
                        />
                        {searchQuery && (
                            <button
                                onClick={() => setSearchQuery('')}
                                className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300"
                            >
                                <X className="h-5 w-5" />
                            </button>
                        )}
                    </div>
                    <Button
                        variant="ghost"
                        onClick={() => navigate('/')}
                        className="text-gray-400 hover:text-white"
                    >
                        Cancel
                    </Button>
                </div>

                {/* Loading State */}
                {loading && (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="h-12 w-12 text-red-600 animate-spin" />
                    </div>
                )}

                {/* Error State */}
                {error && !loading && (
                    <div className="flex flex-col items-center justify-center py-20 space-y-4">
                        <div className="bg-red-900/20 border border-red-900 rounded-full p-6">
                            <AlertCircle className="h-16 w-16 text-red-500" />
                        </div>
                        <h3 className="text-white text-xl font-semibold">Unable to fetch results</h3>
                        <p className="text-gray-400 text-center max-w-md">{error}</p>
                        <Button onClick={fetchMovies} className="bg-red-600 hover:bg-red-700">
                            Retry
                        </Button>
                    </div>
                )}

                {/* Search Results or Suggestions */}
                {!loading && !error && (
                    <>
                        {!searchQuery ? (
                            <div className="space-y-6">
                                <div>
                                    <h2 className="text-white text-xl font-semibold mb-4">Popular Searches</h2>
                                    <div className="flex flex-wrap gap-2">
                                        {popularSearches.map((term) => (
                                            <Button
                                                key={term}
                                                variant="outline"
                                                onClick={() => setSearchQuery(term)}
                                                className="border-gray-700 text-gray-300 hover:bg-gray-800 hover:text-white"
                                            >
                                                {term}
                                            </Button>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="space-y-6">
                                <div className="flex items-center justify-between">
                                    <h2 className="text-white text-xl font-semibold">
                                        {movies.length} {movies.length === 1 ? 'result' : 'results'} for "{searchQuery}"
                                    </h2>
                                </div>

                                {movies.length === 0 ? (
                                    <div className="text-center py-20">
                                        <Search className="h-16 w-16 text-gray-600 mx-auto mb-4" />
                                        <h3 className="text-white text-xl mb-2">No results found</h3>
                                        <p className="text-gray-400">Try searching with different keywords</p>
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
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default SearchResults;