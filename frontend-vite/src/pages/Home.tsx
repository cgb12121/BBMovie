import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Play, Info } from 'lucide-react';
import { Button } from '../components/ui/button';
import { MovieRow } from '../components/MovieRow';
import { ImageWithFallback } from '../components/ImageWithFallback';
import { apiCall } from '../services/apiWrapper';

interface Movie {
  id: number;
  title: string;
  rating: number;
  posterUrl: string;
  description?: string;
}

const Home: React.FC = () => {
  const navigate = useNavigate();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchMovies();
  }, []);

  const fetchMovies = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiCall.getMovies();
      if (response.success) {
        setMovies(response.data ?? []);
      } else {
        setError(response.message || 'Unable to load movies. Please try again later.');
        setMovies([]);
      }
    } catch (err) {
      console.error('Error fetching movies:', err);
      setError('Unable to load movies. Please try again later.');
      setMovies([]);
    } finally {
      setLoading(false);
    }
  };

  const handleMovieClick = (movieId: string) => {
    navigate(`/movies/${movieId}`);
  };

  const featuredMovie = movies.length > 0 ? movies[0] : null;
  const trendingMovies = movies.slice(0, 8);
  const topRatedMovies = [...movies].sort((a, b) => b.rating - a.rating).slice(0, 8);
  const newReleases = movies.slice(8, 16);

  if (loading) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <div className="text-center space-y-4">
          <p className="text-white text-xl">{error}</p>
          <Button onClick={fetchMovies}>Retry</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black">
      {/* Hero Section */}
      {featuredMovie && (
        <div className="relative h-[80vh] md:h-screen">
          <div className="absolute inset-0">
            <ImageWithFallback
              src={featuredMovie.posterUrl}
              alt={featuredMovie.title}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-black via-black/50 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent" />
          </div>

          <div className="relative h-full flex items-center px-4 md:px-12">
            <div className="max-w-2xl space-y-4 md:space-y-6">
              <h1 className="text-white text-4xl md:text-6xl font-bold">{featuredMovie.title}</h1>
              {featuredMovie.description && (
                <p className="text-gray-200 text-sm md:text-base line-clamp-3">
                  {featuredMovie.description}
                </p>
              )}
              <div className="flex items-center gap-2 text-gray-300">
                <span className="text-yellow-400">★ {featuredMovie.rating}</span>
              </div>
              <div className="flex items-center gap-3">
                <Button
                  size="lg"
                  className="bg-white text-black hover:bg-gray-200 gap-2"
                  onClick={() => handleMovieClick(String(featuredMovie.id))}
                >
                  <Play className="h-5 w-5" fill="currentColor" />
                  Play
                </Button>
                <Button
                  size="lg"
                  variant="secondary"
                  className="bg-gray-500/50 text-white hover:bg-gray-500/70 gap-2"
                  onClick={() => handleMovieClick(String(featuredMovie.id))}
                >
                  <Info className="h-5 w-5" />
                  More Info
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Movie Rows */}
      <div className="relative -mt-32 z-10 space-y-12 pb-20">
        {trendingMovies.length > 0 && (
          <MovieRow
            title="Trending Now"
            movies={trendingMovies}
            onMovieClick={handleMovieClick}
          />
        )}
        {topRatedMovies.length > 0 && (
          <MovieRow
            title="Top Rated"
            movies={topRatedMovies}
            onMovieClick={handleMovieClick}
          />
        )}
        {newReleases.length > 0 && (
          <MovieRow
            title="New Releases"
            movies={newReleases}
            onMovieClick={handleMovieClick}
          />
        )}
        {movies.length > 16 && (
          <MovieRow
            title="Popular on BBMovie"
            movies={movies.slice(16, 24)}
            onMovieClick={handleMovieClick}
          />
        )}
      </div>
    </div>
  );
};

export default Home;
