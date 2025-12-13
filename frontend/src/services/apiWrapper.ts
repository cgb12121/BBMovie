import api from './api';
import { mockService } from './mockService';
import { MockApiResponse } from '../types/mockData';
import { ApiResponse } from '../types/auth';

// Check if mock mode is enabled
const isMockMode = () => {
  return process.env.REACT_APP_USE_MOCK_DATA === 'true' || 
         process.env.NODE_ENV === 'development' && localStorage.getItem('useMockData') === 'true';
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
    } catch (error) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch movies',
        errors: [error.message || 'Unknown error']
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
    } catch (error) {
      return {
        success: false,
        data: null,
        message: 'Failed to fetch movie',
        errors: [error.message || 'Unknown error']
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
    } catch (error) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch categories',
        errors: [error.message || 'Unknown error']
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
    } catch (error) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch movies by category',
        errors: [error.message || 'Unknown error']
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
    } catch (error) {
      return {
        success: false,
        data: [],
        message: 'Failed to search movies',
        errors: [error.message || 'Unknown error']
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
    } catch (error) {
      return {
        success: false,
        data: [],
        message: 'Failed to fetch similar movies',
        errors: [error.message || 'Unknown error']
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