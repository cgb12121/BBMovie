import api from './api';
import { mockService } from './mockService';
import type { MockApiResponse } from '../types/mockData';
import type { ApiResponse } from '../types/auth';

// Helper to extract a safe error message from unknown
const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  try {
    return JSON.stringify(error);
  } catch {
    return String(error);
  }
};

// Check if mock mode is enabled
const isMockMode = () => {
  const useMockEnv = import.meta.env.VITE_USE_MOCK_DATA;
  const isDev = import.meta.env.MODE === 'development';
  return useMockEnv === 'true' || (isDev && localStorage.getItem('useMockData') === 'true');
};

// Type guard to check if response is mock
const isMockResponse = <T>(response: ApiResponse<T> | MockApiResponse<T>): response is MockApiResponse<T> => {
  return (response as MockApiResponse<T>).data !== undefined && 
         (response as MockApiResponse<T>).success !== undefined;
};

// Transform mock response to regular API response format
const transformMockResponse = <T>(mockResponse: MockApiResponse<T>): ApiResponse<T> => {
  return {
    success: mockResponse.success,
    data: mockResponse.data,
    message: mockResponse.message,
    errors: mockResponse.errors
  };
};

// Wrapper function for API calls that can use mock data
export const apiCall = {
  async getMovies(): Promise<ApiResponse<any[]>> {
    if (isMockMode()) {
      const mockResponse = await mockService.getMovies();
      return transformMockResponse(mockResponse);
    }
    
    try {
      const response = await api.get('/api/movies');
      return {
        success: true,
        data: response.data,
        message: null,
        errors: null
      };
    } catch (error: unknown) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch movies',
        errors: [getErrorMessage(error)]
      };
    }
  },

  async getMovieById(id: string): Promise<ApiResponse<any>> {
    if (isMockMode()) {
      const mockResponse = await mockService.getMovieById(id);
      return transformMockResponse(mockResponse);
    }
    
    try {
      const response = await api.get(`/api/movies/${id}`);
      return {
        success: true,
        data: response.data,
        message: null,
        errors: null
      };
    } catch (error: unknown) {
      return {
        success: false,
        data: null,
        message: 'Failed to fetch movie',
        errors: [getErrorMessage(error)]
      };
    }
  },

  async getCategories(): Promise<ApiResponse<any[]>> {
    if (isMockMode()) {
      const mockResponse = await mockService.getCategories();
      return transformMockResponse(mockResponse);
    }
    
    try {
      const response = await api.get('/api/categories');
      return {
        success: true,
        data: response.data,
        message: null,
        errors: null
      };
    } catch (error: unknown) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch categories',
        errors: [getErrorMessage(error)]
      };
    }
  },

  async getMoviesByCategory(categoryId: string): Promise<ApiResponse<any[]>> {
    if (isMockMode()) {
      const mockResponse = await mockService.getMoviesByCategory(categoryId);
      return transformMockResponse(mockResponse);
    }
    
    try {
      const response = await api.get(`/api/categories/${categoryId}/movies`);
      return {
        success: true,
        data: response.data,
        message: null,
        errors: null
      };
    } catch (error: unknown) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch movies by category',
        errors: [getErrorMessage(error)]
      };
    }
  },

  async searchMovies(query: string, limit?: number): Promise<ApiResponse<any[]>> {
    if (isMockMode()) {
      const mockResponse = await mockService.searchMovies(query);
      return transformMockResponse(mockResponse);
    }
    
    try {
      const params = new URLSearchParams();
      if (query) params.append('query', query);
      if (limit) params.append('limit', limit.toString());
      
      const response = await api.get(`/api/search/similar-search?${params.toString()}`);
      return {
        success: true,
        data: response.data,
        message: null,
        errors: null
      };
    } catch (error: unknown) {
      return {
        success: false,
        data: [],
        message: 'Failed to search movies',
        errors: [getErrorMessage(error)]
      };
    }
  },

  async getSimilarMovies(movieId: string): Promise<ApiResponse<any[]>> {
    if (isMockMode()) {
      const mockResponse = await mockService.getSimilarMovies(movieId);
      return transformMockResponse(mockResponse);
    }
    
    try {
      const response = await api.get(`/api/movies/${movieId}/similar`);
      return {
        success: true,
        data: response.data,
        message: null,
        errors: null
      };
    } catch (error: unknown) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch similar movies',
        errors: [getErrorMessage(error)]
      };
    }
  }
};

// Function to toggle mock mode
export const toggleMockMode = (enable: boolean) => {
  if (enable) {
    localStorage.setItem('useMockData', 'true');
  } else {
    localStorage.removeItem('useMockData');
  }
};

// Check if mock mode is active
export const isMockModeActive = () => isMockMode();