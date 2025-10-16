import { Play, Plus } from 'lucide-react';
import { ImageWithFallback } from './ImageWithFallback';

interface MovieCardProps {
  movie: {
    id: string | number;
    title: string;
    thumbnail?: string;
    posterUrl?: string;
  };
  onMovieClick: (movieId: string) => void;
}

export function MovieCard({ movie, onMovieClick }: MovieCardProps) {
  const imageUrl = movie.thumbnail || movie.posterUrl || '';
  
  return (
    <div 
      className="group relative flex-shrink-0 w-[200px] md:w-[280px] cursor-pointer transition-transform duration-300 hover:scale-105"
      onClick={() => onMovieClick(String(movie.id))}
    >
      <div className="relative aspect-[2/3] overflow-hidden rounded-md">
        <ImageWithFallback
          src={imageUrl}
          alt={movie.title}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300">
          <div className="absolute bottom-0 left-0 right-0 p-4 space-y-2">
            <h3 className="text-white font-medium">{movie.title}</h3>
            <div className="flex items-center gap-2">
              <button 
                className="flex items-center gap-1 bg-white text-black px-3 py-1 rounded hover:bg-gray-200 transition-colors"
                onClick={(e) => {
                  e.stopPropagation();
                  onMovieClick(String(movie.id));
                }}
              >
                <Play className="h-4 w-4" fill="currentColor" />
                <span className="text-sm">Play</span>
              </button>
              <button 
                className="flex items-center justify-center bg-gray-800/80 text-white p-2 rounded-full hover:bg-gray-700 transition-colors"
                onClick={(e) => e.stopPropagation()}
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

