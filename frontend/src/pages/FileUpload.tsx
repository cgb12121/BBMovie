import React, { useState, useRef, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { 
  Box, 
  Button, 
  Card, 
  CardContent, 
  Typography, 
  LinearProgress, 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  TextField, 
  Alert, 
  IconButton,
  Chip,
  Grid,
  Paper
} from '@mui/material';
import { 
  CloudUpload, 
  Delete, 
  CheckCircle, 
  Error as ErrorIcon, 
  UploadFile,
  Storage,
  Category
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

// Types
interface UploadMetadata {
  entityType: 'MOVIE' | 'TRAILER' | 'POSTER';
  storage: 'LOCAL' | 'S3' | 'CLOUDINARY';
  quality?: string;
}

interface FileUpload {
  file: File;
  id: string;
  progress: number;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
  url?: string;
}

// Styled components
const DropzoneArea = styled(Box)(({ theme }) => ({
  border: `2px dashed ${theme.palette.primary.main}`,
  borderRadius: theme.spacing(2),
  padding: theme.spacing(4),
  textAlign: 'center',
  cursor: 'pointer',
  transition: 'all 0.3s ease',
  backgroundColor: theme.palette.background.paper,
  '&:hover': {
    borderColor: theme.palette.primary.dark,
    backgroundColor: theme.palette.action.hover,
  },
  '&.drag-active': {
    borderColor: theme.palette.success.main,
    backgroundColor: theme.palette.success.light + '20',
  }
}));

const FileUpload: React.FC = () => {
  const [uploads, setUploads] = useState<FileUpload[]>([]);
  const [metadata, setMetadata] = useState<UploadMetadata>({
    entityType: 'MOVIE',
    storage: 'LOCAL',
    quality: ''
  });
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const newUploads = acceptedFiles.map(file => ({
      file,
      id: Math.random().toString(36).substr(2, 9),
      progress: 0,
      status: 'pending' as const
    }));
    setUploads(prev => [...prev, ...newUploads]);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'image/*': ['.jpeg', '.jpg', '.png', '.gif', '.webp'],
      'video/*': ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm'],
      'application/pdf': ['.pdf'],
      'text/*': ['.txt', '.md', '.json', '.xml']
    },
    multiple: true
  });

  const removeFile = (id: string) => {
    setUploads(prev => prev.filter(upload => upload.id !== id));
  };

  const uploadFile = async (upload: FileUpload) => {
    const formData = new FormData();
    formData.append('file', upload.file);
    formData.append('metadata', JSON.stringify(metadata));

    try {
      // Update status to uploading
      setUploads(prev => prev.map(u => 
        u.id === upload.id ? { ...u, status: 'uploading' } : u
      ));

      // Use the v2 endpoint for real-time progress
      const response = await fetch('/api/file/upload/v2', {
        method: 'POST',
        body: formData,
        headers: {
          // Don't set Content-Type, let browser set it with boundary
        }
      });

      if (!response.ok) {
        throw new Error(`Upload failed: ${response.statusText}`);
      }

      // Handle Server-Sent Events for progress
      const reader = response.body?.getReader();
      if (reader) {
        const decoder = new TextDecoder();
        
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          
          const chunk = decoder.decode(value);
          const lines = chunk.split('\n');
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.slice(6);
              if (data.includes('progress')) {
                const progressMatch = data.match(/progress: (\d+)/);
                if (progressMatch) {
                  const progress = parseInt(progressMatch[1]);
                  setUploads(prev => prev.map(u => 
                    u.id === upload.id ? { ...u, progress } : u
                  ));
                }
              } else if (data.includes('completed')) {
                const urlMatch = data.match(/url: (.+)/);
                if (urlMatch) {
                  setUploads(prev => prev.map(u => 
                    u.id === upload.id ? { 
                      ...u, 
                      status: 'completed', 
                      progress: 100,
                      url: urlMatch[1]
                    } : u
                  ));
                }
              }
            }
          }
        }
      }
    } catch (err) {
      console.error('Upload error:', err);
      setUploads(prev => prev.map(u => 
        u.id === upload.id ? { 
          ...u, 
          status: 'error', 
          error: err instanceof Error ? err.message : 'Upload failed'
        } : u
      ));
    }
  };

  const uploadAllFiles = async () => {
    if (uploads.length === 0) {
      setError('No files to upload');
      return;
    }

    setIsUploading(true);
    setError(null);
    setSuccess(null);

    try {
      const pendingUploads = uploads.filter(u => u.status === 'pending');
      
      // Upload files sequentially to avoid overwhelming the server
      for (const upload of pendingUploads) {
        await uploadFile(upload);
      }
      
      setSuccess(`Successfully uploaded ${pendingUploads.length} file(s)`);
    } catch (err) {
      setError('Upload failed. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  const getStatusIcon = (status: FileUpload['status']) => {
    switch (status) {
      case 'completed':
        return <CheckCircle color="success" />;
      case 'error':
        return <ErrorIcon color="error" />;
      case 'uploading':
        return <UploadFile color="primary" />;
      default:
        return <UploadFile color="disabled" />;
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', p: 3 }}>
      <Typography variant="h4" gutterBottom sx={{ mb: 4 }}>
        File Upload Service
      </Typography>

      {/* Configuration Section */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Upload Configuration
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Entity Type</InputLabel>
                <Select
                  value={metadata.entityType}
                  label="Entity Type"
                  onChange={(e) => setMetadata(prev => ({ 
                    ...prev, 
                    entityType: e.target.value as UploadMetadata['entityType'] 
                  }))}
                >
                  <MenuItem value="MOVIE">Movie</MenuItem>
                  <MenuItem value="TRAILER">Trailer</MenuItem>
                  <MenuItem value="POSTER">Poster</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Storage Type</InputLabel>
                <Select
                  value={metadata.storage}
                  label="Storage Type"
                  onChange={(e) => setMetadata(prev => ({ 
                    ...prev, 
                    storage: e.target.value as UploadMetadata['storage'] 
                  }))}
                >
                  <MenuItem value="LOCAL">Local Storage</MenuItem>
                  <MenuItem value="S3">Amazon S3</MenuItem>
                  <MenuItem value="CLOUDINARY">Cloudinary</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Quality (Optional)"
                value={metadata.quality}
                onChange={(e) => setMetadata(prev => ({ 
                  ...prev, 
                  quality: e.target.value 
                }))}
                placeholder="e.g., HD, 4K, 1080p"
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Dropzone */}
      <DropzoneArea
        {...getRootProps()}
        className={isDragActive ? 'drag-active' : ''}
        sx={{ mb: 3 }}
      >
        <input {...getInputProps()} />
        <CloudUpload sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
        <Typography variant="h6" gutterBottom>
          {isDragActive ? 'Drop files here' : 'Drag & drop files here'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          or click to select files
        </Typography>
        <Typography variant="caption" display="block" sx={{ mt: 1 }}>
          Supports: Images, Videos, PDFs, Text files
        </Typography>
      </DropzoneArea>

      {/* Alerts */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* File List */}
      {uploads.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">
                Files to Upload ({uploads.length})
              </Typography>
              <Button
                variant="contained"
                onClick={uploadAllFiles}
                disabled={isUploading || uploads.every(u => u.status !== 'pending')}
                startIcon={<CloudUpload />}
              >
                {isUploading ? 'Uploading...' : 'Upload All Files'}
              </Button>
            </Box>

            {uploads.map((upload) => (
              <Paper key={upload.id} sx={{ p: 2, mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  {getStatusIcon(upload.status)}
                  <Typography variant="body1" sx={{ ml: 1, flexGrow: 1 }}>
                    {upload.file.name}
                  </Typography>
                  <Chip 
                    label={formatFileSize(upload.file.size)} 
                    size="small" 
                    sx={{ mr: 1 }}
                  />
                  <IconButton 
                    size="small" 
                    onClick={() => removeFile(upload.id)}
                    disabled={upload.status === 'uploading'}
                  >
                    <Delete />
                  </IconButton>
                </Box>

                {upload.status === 'uploading' && (
                  <Box sx={{ width: '100%', mt: 1 }}>
                    <LinearProgress 
                      variant="determinate" 
                      value={upload.progress} 
                      sx={{ height: 8, borderRadius: 4 }}
                    />
                    <Typography variant="caption" sx={{ mt: 0.5 }}>
                      {upload.progress}% complete
                    </Typography>
                  </Box>
                )}

                {upload.status === 'completed' && upload.url && (
                  <Alert severity="success" sx={{ mt: 1 }}>
                    File uploaded successfully! 
                    <Button 
                      size="small" 
                      href={upload.url} 
                      target="_blank" 
                      sx={{ ml: 1 }}
                    >
                      View File
                    </Button>
                  </Alert>
                )}

                {upload.status === 'error' && upload.error && (
                  <Alert severity="error" sx={{ mt: 1 }}>
                    {upload.error}
                  </Alert>
                )}
              </Paper>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Upload Statistics */}
      {uploads.length > 0 && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Upload Statistics
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={6} md={3}>
                <Box textAlign="center">
                  <Typography variant="h4" color="primary">
                    {uploads.length}
                  </Typography>
                  <Typography variant="body2">Total Files</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={3}>
                <Box textAlign="center">
                  <Typography variant="h4" color="success.main">
                    {uploads.filter(u => u.status === 'completed').length}
                  </Typography>
                  <Typography variant="body2">Completed</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={3}>
                <Box textAlign="center">
                  <Typography variant="h4" color="warning.main">
                    {uploads.filter(u => u.status === 'uploading').length}
                  </Typography>
                  <Typography variant="body2">Uploading</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={3}>
                <Box textAlign="center">
                  <Typography variant="h4" color="error">
                    {uploads.filter(u => u.status === 'error').length}
                  </Typography>
                  <Typography variant="body2">Failed</Typography>
                </Box>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default FileUpload; 