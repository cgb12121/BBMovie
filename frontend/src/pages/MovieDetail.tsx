import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Play, Plus, Star, Clock, Calendar, ArrowLeft } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';
import { ImageWithFallback } from '../components/ImageWithFallback';
import { MovieRow } from '../components/MovieRow';
import api from '../services/api';

interface Movie {
    id: number;
    title: string;
    description?: string;
    rating: number;
    posterUrl: string;
    releaseDate?: string;
    duration?: string;
    genres?: string[];
}

const MovieDetail: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [movie, setMovie] = useState<Movie | null>(null);
    const [similarMovies, setSimilarMovies] = useState<Movie[]>([]);

    useEffect(() => {
        const fetchMovieDetails = async () => {
            try {
                setLoading(true);
                // Fetch movie details - you can update this endpoint based on your API
                const response = await api.get(`/api/movies/${id}`);
                setMovie(response.data);
                
                // Fetch similar movies (or all movies as fallback)
                const moviesResponse = await api.get('/api/movies');
                const allMovies = moviesResponse.data || [];
                setSimilarMovies(allMovies.slice(0, 8));
            } catch (error) {
                console.error('Error fetching movie details:', error);
                setMovie(null);
            } finally {
                setLoading(false);
            }
        };

        if (id) {
            fetchMovieDetails();
        }
    }, [id]);

    if (loading) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center">
                <div className="text-white text-xl">Loading...</div>
            </div>
        );
    }

    if (!movie) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center">
                <div className="text-center space-y-4">
                    <h2 className="text-white text-2xl">Movie not found</h2>
                    <Button onClick={() => navigate('/')}>Go Home</Button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-black">
            {/* Hero Section with Backdrop */}
            <div className="relative h-[70vh] md:h-[80vh]">
                <div className="absolute inset-0">
                    <ImageWithFallback
                        src={movie.posterUrl}
                        alt={movie.title}
                        className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-r from-black via-black/80 to-transparent" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent" />
                </div>

                <div className="relative h-full flex items-center px-4 md:px-12">
                    <div className="max-w-3xl space-y-6">
                        <Button
                            variant="ghost"
                            className="text-white hover:text-gray-300 -ml-4 mb-4"
                            onClick={() => navigate(-1)}
                        >
                            <ArrowLeft className="h-5 w-5 mr-2" />
                            Back
                        </Button>

                        <h1 className="text-white text-5xl md:text-7xl font-bold">{movie.title}</h1>

                        <div className="flex flex-wrap items-center gap-4 text-gray-300">
                            <div className="flex items-center gap-2">
                                <Star className="h-5 w-5 text-yellow-400 fill-yellow-400" />
                                <span className="text-lg font-semibold">{movie.rating}/10</span>
                            </div>
                            {movie.duration && (
                                <div className="flex items-center gap-2">
                                    <Clock className="h-5 w-5" />
                                    <span>{movie.duration}</span>
                                </div>
                            )}
                            {movie.releaseDate && (
                                <div className="flex items-center gap-2">
                                    <Calendar className="h-5 w-5" />
                                    <span>{new Date(movie.releaseDate).getFullYear()}</span>
                                </div>
                            )}
                        </div>

                        {movie.genres && movie.genres.length > 0 && (
                            <div className="flex flex-wrap gap-2">
                                {movie.genres.map((genre, index) => (
                                    <Badge key={index} className="bg-gray-700 text-white">
                                        {genre}
                                    </Badge>
                                ))}
                            </div>
                        )}

                        {movie.description && (
                            <p className="text-gray-200 text-lg max-w-2xl line-clamp-4">
                                {movie.description}
                            </p>
                        )}

                        <div className="flex flex-wrap items-center gap-3 pt-4">
                            <Button
                                size="lg"
                                className="bg-white text-black hover:bg-gray-200 gap-2"
                            >
                                <Play className="h-5 w-5" fill="currentColor" />
                                Play
                            </Button>
                            <Button
                                size="lg"
                                variant="secondary"
                                className="bg-gray-500/50 text-white hover:bg-gray-500/70 gap-2"
                            >
                                <Plus className="h-5 w-5" />
                                Add to Watchlist
                            </Button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Details Section */}
            <div className="relative px-4 md:px-12 py-12 space-y-12">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white text-2xl">About</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        {movie.description && (
                            <div>
                                <h3 className="text-gray-400 text-sm mb-2">Description</h3>
                                <p className="text-white">{movie.description}</p>
                            </div>
                        )}
                        
                        <Separator className="bg-gray-800" />

                        <div className="grid md:grid-cols-3 gap-6">
                            {movie.rating && (
                                <div>
                                    <h3 className="text-gray-400 text-sm mb-2">Rating</h3>
                                    <p className="text-white text-lg font-semibold">{movie.rating}/10</p>
                                </div>
                            )}
                            {movie.releaseDate && (
                                <div>
                                    <h3 className="text-gray-400 text-sm mb-2">Release Date</h3>
                                    <p className="text-white text-lg font-semibold">
                                        {new Date(movie.releaseDate).toLocaleDateString()}
                                    </p>
                                </div>
                            )}
                            {movie.duration && (
                                <div>
                                    <h3 className="text-gray-400 text-sm mb-2">Runtime</h3>
                                    <p className="text-white text-lg font-semibold">{movie.duration}</p>
                                </div>
                            )}
                        </div>
                    </CardContent>
                </Card>

                {/* Similar Movies */}
                {similarMovies.length > 0 && (
                    <div className="space-y-6">
                        <h2 className="text-white text-2xl font-semibold px-4 md:px-0">More Like This</h2>
                        <MovieRow
                            title=""
                            movies={similarMovies}
                            onMovieClick={(movieId) => navigate(`/movies/${movieId}`)}
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export default MovieDetail; 
