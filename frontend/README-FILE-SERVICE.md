# File Service Frontend

This document describes the frontend implementation for the BBMovie file service module, providing a comprehensive file upload and management interface.

## Overview

The file service frontend provides a modern, user-friendly interface for uploading and managing files in the BBMovie application. It supports multiple storage backends (Local, S3, Cloudinary) and different entity types (Movie, Trailer, Poster).

## Features

### 🚀 Core Features
- **Drag & Drop Upload**: Intuitive file upload with drag-and-drop support
- **Real-time Progress**: Live upload progress tracking with Server-Sent Events
- **Multiple Storage Support**: Upload to Local, S3, or Cloudinary storage
- **File Preview**: Preview images and videos before upload
- **Batch Upload**: Upload multiple files simultaneously
- **File Management**: Browse, search, and manage uploaded files
- **Upload History**: Track upload history and statistics

### 📁 Supported File Types
- **Images**: JPEG, PNG, GIF, WebP, BMP, SVG
- **Videos**: MP4, AVI, MOV, WMV, FLV, WebM, MKV
- **Documents**: PDF, DOC, DOCX, TXT, MD, JSON, XML

### 🎯 Entity Types
- **Movie**: Main movie files
- **Trailer**: Movie trailer videos
- **Poster**: Movie poster images

## Architecture

### Components Structure

```
src/
├── pages/
│   ├── FileUpload.tsx          # Main upload page
│   └── FileManagement.tsx      # File management dashboard
├── components/
│   ├── FileUploadManager.tsx   # Reusable upload manager
│   └── FilePreview.tsx         # File preview component
├── services/
│   └── fileService.ts          # API service layer
└── types/
    └── fileUpload.ts           # TypeScript definitions
```

### Key Components

#### 1. FileUpload.tsx
The main upload page that provides:
- Drag-and-drop file selection
- Upload configuration (entity type, storage, quality)
- Real-time progress tracking
- File validation and error handling
- Upload statistics

#### 2. FileUploadManager.tsx
A reusable component that can be embedded in other parts of the application:
- Configurable upload behavior
- Progress tracking with pause/resume functionality
- Concurrent upload control
- Error handling and retry logic

#### 3. FilePreview.tsx
File preview component with:
- Image and video thumbnails
- File type detection and icons
- Full-screen preview modal
- Download functionality

#### 4. FileManagement.tsx
Comprehensive file management dashboard:
- File browser with search and filters
- Upload history tracking
- File statistics and analytics
- Bulk operations support

## API Integration

### Backend Endpoints

The frontend integrates with the following backend endpoints:

```typescript
// File upload endpoints
POST /file/upload/v1     // Basic upload without progress
POST /file/upload/v2     // Upload with SSE progress tracking
POST /file/upload/test   // Test upload endpoint
```

### Service Layer

The `fileService.ts` provides a clean abstraction over the backend API:

```typescript
// Upload with progress tracking
await fileService.uploadFileWithProgress(file, metadata, onProgress);

// Upload with Server-Sent Events
await fileService.uploadFileWithSSE(file, metadata, onProgress);

// Simple upload
await fileService.uploadFile(file, metadata);

// File validation
const validation = fileService.validateFile(file);
```

## Usage Examples

### Basic File Upload

```tsx
import FileUploadManager from '../components/FileUploadManager';

const MyComponent = () => {
  const metadata = {
    entityType: 'MOVIE',
    storage: 'LOCAL',
    quality: 'HD'
  };

  return (
    <FileUploadManager
      metadata={metadata}
      onUploadComplete={(uploads) => console.log('Uploads completed:', uploads)}
      onUploadError={(error) => console.error('Upload failed:', error)}
      maxFiles={10}
      autoUpload={true}
    />
  );
};
```

### File Preview

```tsx
import FilePreview from '../components/FilePreview';

const MyComponent = () => {
  return (
    <FilePreview
      file={selectedFile}
      onRemove={() => handleRemoveFile()}
      showActions={true}
      maxWidth={200}
      maxHeight={200}
    />
  );
};
```

## Configuration

### Environment Variables

```env
REACT_APP_API_URL=http://localhost:8080
```

### Upload Configuration

```typescript
interface UploadConfig {
  maxFileSize: number;           // Maximum file size in bytes
  allowedFileTypes: string[];    // Allowed MIME types
  maxConcurrentUploads: number;  // Concurrent upload limit
  retryAttempts: number;         // Retry attempts on failure
  retryDelay: number;           // Delay between retries
}
```

## Styling and Theming

The frontend uses Material-UI (MUI) for consistent styling and theming:

- **Responsive Design**: Works on desktop, tablet, and mobile
- **Dark/Light Theme**: Supports theme switching
- **Custom Components**: Styled components for enhanced UX
- **Accessibility**: WCAG compliant with proper ARIA labels

## Error Handling

### Upload Errors
- File size validation
- File type validation
- Network error handling
- Server error responses
- Authentication errors

### User Feedback
- Progress indicators
- Error messages with actionable suggestions
- Success confirmations
- Loading states

## Performance Optimizations

### File Handling
- Client-side file validation
- Chunked uploads for large files
- Image compression for previews
- Lazy loading of file lists

### Memory Management
- Automatic cleanup of blob URLs
- Efficient file preview generation
- Optimized re-renders with React.memo

## Security Considerations

### File Validation
- Client-side file type checking
- File size limits
- Malicious file detection
- Secure file naming

### Authentication
- JWT token validation
- CSRF protection
- Secure file upload endpoints

## Testing

### Unit Tests
- Component rendering tests
- Service layer tests
- Utility function tests

### Integration Tests
- API integration tests
- Upload flow tests
- Error handling tests

## Deployment

### Build Process
```bash
npm install
npm run build
```

### Environment Setup
1. Set `REACT_APP_API_URL` to your backend URL
2. Configure CORS on the backend
3. Set up authentication tokens

## Troubleshooting

### Common Issues

1. **Upload Fails**
   - Check network connectivity
   - Verify file size limits
   - Ensure proper authentication

2. **Preview Not Working**
   - Check file type support
   - Verify browser compatibility
   - Check console for errors

3. **Progress Not Updating**
   - Verify SSE endpoint is working
   - Check browser console for errors
   - Ensure proper event handling

### Debug Mode

Enable debug logging by setting:
```typescript
localStorage.setItem('debug', 'file-service:*');
```

## Future Enhancements

### Planned Features
- **Resumable Uploads**: Resume interrupted uploads
- **Chunked Uploads**: Split large files into chunks
- **Image Processing**: Client-side image optimization
- **Video Thumbnails**: Generate video preview thumbnails
- **Bulk Operations**: Select and manage multiple files
- **Advanced Search**: Full-text search in file metadata

### Performance Improvements
- **Virtual Scrolling**: For large file lists
- **Web Workers**: For file processing
- **Service Workers**: For offline support
- **Caching**: Intelligent file caching

## Contributing

### Development Setup
1. Clone the repository
2. Install dependencies: `npm install`
3. Start development server: `npm start`
4. Run tests: `npm test`

### Code Style
- Use TypeScript for type safety
- Follow React best practices
- Use Material-UI components
- Write comprehensive tests

### Pull Request Process
1. Create feature branch
2. Implement changes with tests
3. Update documentation
4. Submit pull request

## License

This project is part of the BBMovie application and follows the same licensing terms. 