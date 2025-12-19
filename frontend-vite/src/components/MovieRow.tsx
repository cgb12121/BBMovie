import { ChevronLeft, ChevronRight } from 'lucide-react';
import { MovieCard } from './MovieCard';
import { useRef } from 'react';
import React from 'react';

interface MovieRowProps {
  title: string;
  movies: Array<{
    id: string | number;
    title: string;
    thumbnail?: string;
    posterUrl?: string;
  }>;
  onMovieClick: (movieId: string) => void;
}

export function MovieRow({ title, movies, onMovieClick }: MovieRowProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  const scroll = (direction: 'left' | 'right') => {
    if (scrollRef.current) {
      const scrollAmount = 600;
      scrollRef.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      });
    }
  };

  if (!movies || movies.length === 0) {
    return null;
  }

  return (
    <div className="space-y-4 px-4 md:px-12">
      <h2 className="text-white text-xl md:text-2xl font-semibold">{title}</h2>
      <div className="relative group/row">
        <button
          onClick={() => scroll('left')}
          className="absolute left-0 top-0 bottom-0 z-10 bg-black/50 text-white p-2 opacity-0 group-hover/row:opacity-100 transition-opacity hover:bg-black/70"
          aria-label="Scroll left"
        >
          <ChevronLeft className="h-8 w-8" />
        </button>
        <div
          ref={scrollRef}
          className="flex gap-4 overflow-x-auto scrollbar-hide scroll-smooth"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
        >
          {movies.map((movie) => (
            <MovieCard key={movie.id} movie={movie} onMovieClick={onMovieClick} />
          ))}
        </div>
        <button
          onClick={() => scroll('right')}
          className="absolute right-0 top-0 bottom-0 z-10 bg-black/50 text-white p-2 opacity-0 group-hover/row:opacity-100 transition-opacity hover:bg-black/70"
          aria-label="Scroll right"
        >
          <ChevronRight className="h-8 w-8" />
        </button>
      </div>
    </div>
  );
}

