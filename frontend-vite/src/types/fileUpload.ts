// File upload related types

export interface UploadMetadata {
  entityType: 'MOVIE' | 'TRAILER' | 'POSTER';
  storage: 'LOCAL' | 'S3' | 'CLOUDINARY';
  quality?: string;
}

export interface FileUploadProgress {
  progress: number;
  message: string;
  url?: string;
  error?: string;
}

export interface FileUploadResponse {
  url: string;
  publicId?: string;
  message: string;
}

export interface FileUpload {
  file: File;
  id: string;
  progress: number;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
  url?: string;
  publicId?: string;
}

export interface FileValidationResult {
  isValid: boolean;
  error?: string;
}

export interface UploadConfig {
  maxFileSize: number; // in bytes
  allowedFileTypes: string[];
  maxConcurrentUploads: number;
  retryAttempts: number;
  retryDelay: number; // in milliseconds
}

export interface UploadStatistics {
  totalFiles: number;
  completedFiles: number;
  failedFiles: number;
  uploadingFiles: number;
  totalSize: number;
  uploadedSize: number;
}

// Entity types for different upload contexts
// Entity types supported by the backend
export const EntityType = {
  MOVIE: 'MOVIE',
  TRAILER: 'TRAILER',
  POSTER: 'POSTER',
} as const;

export type EntityType = (typeof EntityType)[keyof typeof EntityType];

// Storage types supported by the backend
export const Storage = {
  LOCAL: 'LOCAL',
  S3: 'S3',
  CLOUDINARY: 'CLOUDINARY',
} as const;

export type Storage = (typeof Storage)[keyof typeof Storage];

// File type categories for better organization
export interface FileTypeCategory {
  name: string;
  extensions: string[];
  mimeTypes: string[];
  maxSize: number; // in bytes
  icon: string;
}

export const FILE_TYPE_CATEGORIES: Record<string, FileTypeCategory> = {
  images: {
    name: 'Images',
    extensions: ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg'],
    mimeTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp', 'image/svg+xml'],
    maxSize: 10 * 1024 * 1024, // 10MB
    icon: '🖼️'
  },
  videos: {
    name: 'Videos',
    extensions: ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm', '.mkv'],
    mimeTypes: ['video/mp4', 'video/avi', 'video/quicktime', 'video/x-ms-wmv', 'video/x-flv', 'video/webm', 'video/x-matroska'],
    maxSize: 100 * 1024 * 1024, // 100MB
    icon: '🎥'
  },
  documents: {
    name: 'Documents',
    extensions: ['.pdf', '.doc', '.docx', '.txt', '.md', '.json', '.xml'],
    mimeTypes: ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain', 'text/markdown', 'application/json', 'application/xml'],
    maxSize: 5 * 1024 * 1024, // 5MB
    icon: '📄'
  }
};

// Upload error types
export const UploadErrorType = {
  FILE_TOO_LARGE: 'FILE_TOO_LARGE',
  INVALID_FILE_TYPE: 'INVALID_FILE_TYPE',
  NETWORK_ERROR: 'NETWORK_ERROR',
  SERVER_ERROR: 'SERVER_ERROR',
  AUTHENTICATION_ERROR: 'AUTHENTICATION_ERROR',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR',
} as const;

export type UploadErrorType =
  (typeof UploadErrorType)[keyof typeof UploadErrorType];

export interface UploadError {
  type: UploadErrorType;
  message: string;
  details?: any;
}

// Upload session for managing multiple file uploads
export interface UploadSession {
  id: string;
  createdAt: Date;
  files: FileUpload[];
  config: UploadConfig;
  statistics: UploadStatistics;
  status: 'active' | 'completed' | 'failed' | 'cancelled';
} 
