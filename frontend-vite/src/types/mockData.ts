// Mock data types for development

export interface MockMovie {
  id: number;
  title: string;
  description?: string;
  rating: number;
  posterUrl: string;
  releaseDate?: string;
  duration?: string;
  genres?: string[];
  thumbnail?: string;
  backdropUrl?: string;
  director?: string;
  cast?: string[];
  trailerUrl?: string;
  year?: number;
}

export interface MockCategory {
  id: number;
  name: string;
  description: string;
  image?: string;
}

export interface MockApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  errors: string[] | null;
}