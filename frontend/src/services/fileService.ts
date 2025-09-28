import { UploadMetadata } from '../types/fileUpload';
import api from './api';

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

class FileService {

  private readonly baseUrl = process.env.REACT_APP_API_GATEWAY_URL ?? 'http://localhost:8765';

  /**
   * Upload a single file with real-time progress tracking
   * Uses Server-Sent Events (SSE) for progress updates
   */
  async uploadFileWithProgress(
    file: File, 
    metadata: UploadMetadata,
    onProgress?: (progress: FileUploadProgress) => void
  ): Promise<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      // Track upload progress
      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable) {
          const progress = Math.round((event.loaded / event.total) * 100);
          onProgress?.({
            progress,
            message: `Uploading... ${progress}%`
          });
        }
      });

      // Handle response
      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const response = JSON.parse(xhr.responseText);
            onProgress?.({
              progress: 100,
              message: 'Upload completed successfully!',
              url: response.url
            });
            resolve(response);
          } catch (error) {
            console.error('Invalid response format', error);
            reject(new Error('Invalid response format'));
          }
        } else {
          console.error('Upload failed', xhr.statusText);
          reject(new Error(`Upload failed: ${xhr.statusText}`));
        }
      });

      // Handle errors
      xhr.addEventListener('error', () => {
        reject(new Error('Network error occurred'));
      });

      xhr.addEventListener('abort', () => {
        reject(new Error('Upload was cancelled'));
      });

      // Open and send request
      xhr.open('POST', `${this.baseUrl}/api/file/upload/v1`);
      xhr.withCredentials = true;
      const token = localStorage.getItem('accessToken');
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }
      xhr.send(formData);
    });
  }

  /**
   * Upload file using Server-Sent Events for real-time progress
   * This method uses the v2 endpoint with SSE support
   */
  async uploadFileWithSSE(
    file: File,
    metadata: UploadMetadata,
    onProgress?: (progress: FileUploadProgress) => void
  ): Promise<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      // Set up SSE handling
      xhr.addEventListener('readystatechange', () => {
        if (xhr.readyState === XMLHttpRequest.LOADING) {
          const lines = xhr.responseText.split('\n');
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6);
              try {
                const parsed = JSON.parse(data);
                if (parsed.progress !== undefined) {
                  onProgress?.({
                    progress: parsed.progress,
                    message: parsed.message || `Progress: ${parsed.progress}%`
                  });
                } else if (parsed.completed) {
                  onProgress?.({
                    progress: 100,
                    message: 'Upload completed!',
                    url: parsed.url
                  });
                  resolve(parsed);
                }
              } catch (e) {
                // Ignore parsing errors for non-JSON SSE messages
                console.error('Invalid SSE message', e);
              }
            }
          }
        } else if (xhr.readyState === XMLHttpRequest.DONE) {
          if (xhr.status >= 200 && xhr.status < 300) {
            // Handle final response
            try {
              const response = JSON.parse(xhr.responseText);
              resolve(response);
            } catch (error) {
              console.error('Invalid response format', error);
              reject(new Error('Invalid response format'));
            }
          } else {
            reject(new Error(`Upload failed: ${xhr.statusText}`));
          }
        }
      });

      // Handle errors
      xhr.addEventListener('error', () => {
        reject(new Error('Network error occurred'));
      });

      xhr.addEventListener('abort', () => {
        reject(new Error('Upload was cancelled'));
      });

      // Open and send request
      xhr.open('POST', `${this.baseUrl}/api/file/upload/v2`);
      xhr.withCredentials = true;
      const token = localStorage.getItem('accessToken');
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }
      xhr.send(formData);
    });
  }

  /**
   * Simple file upload without progress tracking
   * Uses the v1 endpoint for basic uploads
   */
  async uploadFile(
    file: File,
    metadata: UploadMetadata
  ): Promise<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));

    const response = await fetch(`${this.baseUrl}/api/file/upload/v1`, {
      method: 'POST',
      body: formData,
      credentials: 'include'
      // Don't set Content-Type header, let browser set it with boundary
    });

    if (!response.ok) {
      throw new Error(`Upload failed: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Test upload endpoint
   * Uses the test endpoint for validation
   */
  async testUpload(
    file: File,
    metadata: UploadMetadata
  ): Promise<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));

    const response = await fetch(`${this.baseUrl}/api/file/upload/test`, {
      method: 'POST',
      body: formData,
      credentials: 'include'
    });

    if (!response.ok) {
      throw new Error(`Test upload failed: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Validate file before upload
   */
  validateFile(file: File): { isValid: boolean; error?: string } {
    // Check file size (max 100MB)
    const maxSize = 100 * 1024 * 1024; // 100MB
    if (file.size > maxSize) {
      return {
        isValid: false,
        error: `File size exceeds maximum limit of ${this.formatFileSize(maxSize)}`
      };
    }

    // Check file type
    const allowedTypes = [
      'image/jpeg',
      'image/png',
      'image/gif',
      'image/webp',
      'video/mp4',
      'video/avi',
      'video/mov',
      'video/wmv',
      'video/flv',
      'video/webm',
      'application/pdf',
      'text/plain',
      'text/markdown',
      'application/json',
      'application/xml'
    ];

    if (!allowedTypes.includes(file.type)) {
      return {
        isValid: false,
        error: 'File type not supported'
      };
    }

    return { isValid: true };
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  /**
   * Get file extension from filename
   */
  getFileExtension(filename: string): string {
    return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2);
  }

  /**
   * Generate a unique filename
   */
  generateUniqueFilename(originalName: string): string {
    const timestamp = Date.now();
    const extension = this.getFileExtension(originalName);
    const nameWithoutExt = originalName.replace(/\.[^/.]+$/, '');
    return `${nameWithoutExt}_${timestamp}.${extension}`;
  }
}

export const fileService = new FileService();
export default fileService; 