import React, { useState, useCallback, useRef } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  LinearProgress,
  IconButton,
  Chip,
  Grid,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  Tooltip,
  CircularProgress
} from '@mui/material';
import {
  CloudUpload,
  Delete,
  CheckCircle,
  UploadFile,
  Settings,
  Info,
  Pause,
  PlayArrow,
  Stop,
  Storage as StorageIcon,
  Error as ErrorIcon
} from '@mui/icons-material';
import { useDropzone } from 'react-dropzone';
import { styled } from '@mui/material/styles';
import fileService, { FileUploadProgress } from '../services/fileService';
import { 
  FileUpload, 
  UploadMetadata, 
  UploadConfig, 
  UploadStatistics,
  FILE_TYPE_CATEGORIES 
} from '../types/fileUpload';

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

interface FileUploadManagerProps {
  metadata: UploadMetadata;
  onUploadComplete?: (uploads: FileUpload[]) => void;
  onUploadError?: (error: string) => void;
  maxFiles?: number;
  showSettings?: boolean;
  autoUpload?: boolean;
}

const FileUploadManager: React.FC<FileUploadManagerProps> = ({
  metadata,
  onUploadComplete,
  onUploadError,
  maxFiles = 10,
  showSettings = true,
  autoUpload = false
}) => {
  const [uploads, setUploads] = useState<FileUpload[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showSettingsDialog, setShowSettingsDialog] = useState(false);
  const [uploadConfig, setUploadConfig] = useState<UploadConfig>({
    maxFileSize: 100 * 1024 * 1024, // 100MB
    allowedFileTypes: Object.values(FILE_TYPE_CATEGORIES).flatMap(cat => cat.mimeTypes),
    maxConcurrentUploads: 3,
    retryAttempts: 3,
    retryDelay: 1000
  });
  const [isPaused, setIsPaused] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (uploads.length + acceptedFiles.length > maxFiles) {
      setError(`Maximum ${maxFiles} files allowed`);
      return;
    }

    const newUploads = acceptedFiles.map(file => ({
      file,
      id: Math.random().toString(36).substring(2, 9),
      progress: 0,
      status: 'pending' as const
    }));

    setUploads(prev => [...prev, ...newUploads]);
    setError(null);

    if (autoUpload) {
      uploadFiles(newUploads);
    }
  }, [uploads.length, maxFiles, autoUpload]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'image/*': ['.jpeg', '.jpg', '.png', '.gif', '.webp'],
      'video/*': ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm'],
      'application/pdf': ['.pdf'],
      'text/*': ['.txt', '.md', '.json', '.xml']
    },
    multiple: true,
    disabled: isUploading
  });

  const removeFile = (id: string) => {
    setUploads(prev => prev.filter(upload => upload.id !== id));
  };

  const uploadSingleFile = async (upload: FileUpload): Promise<void> => {
    try {
      // Validate file
      const validation = fileService.validateFile(upload.file);
      if (!validation.isValid) {
        throw new Error(validation.error);
      }

      // Update status to uploading
      setUploads(prev => prev.map(u => 
        u.id === upload.id ? { ...u, status: 'uploading' } : u
      ));

      // Upload with progress tracking
      await fileService.uploadFileWithProgress(
        upload.file,
        metadata,
        (progress: FileUploadProgress) => {
          setUploads(prev => prev.map(u => 
            u.id === upload.id ? { 
              ...u, 
              progress: progress.progress,
              error: progress.error
            } : u
          ));
        }
      );

      // Mark as completed
      setUploads(prev => prev.map(u => 
        u.id === upload.id ? { 
          ...u, 
          status: 'completed', 
          progress: 100
        } : u
      ));

    } catch (err) {
      console.error('Upload error:', err);
      setUploads(prev => prev.map(u => 
        u.id === upload.id ? { 
          ...u, 
          status: 'error', 
          error: err instanceof Error ? err.message : 'Upload failed'
        } : u
      ));
      throw err;
    }
  };

  const uploadFiles = async (filesToUpload: FileUpload[] = uploads.filter(u => u.status === 'pending')) => {
    if (filesToUpload.length === 0) {
      setError('No files to upload');
      return;
    }

    setIsUploading(true);
    setError(null);
    setSuccess(null);
    setIsPaused(false);

    // Create abort controller for cancellation
    abortControllerRef.current = new AbortController();

    try {
      // Upload files with concurrency control
      const chunks = [];
      for (let i = 0; i < filesToUpload.length; i += uploadConfig.maxConcurrentUploads) {
        chunks.push(filesToUpload.slice(i, i + uploadConfig.maxConcurrentUploads));
      }

      for (const chunk of chunks) {
        if (abortControllerRef.current.signal.aborted) {
          break;
        }

        // Wait for pause to be released
        while (isPaused && !abortControllerRef.current.signal.aborted) {
          await new Promise(resolve => setTimeout(resolve, 100));
        }

        if (abortControllerRef.current.signal.aborted) {
          break;
        }

        // Upload chunk concurrently
        await Promise.allSettled(
          chunk.map(upload => uploadSingleFile(upload))
        );
      }

      const completedUploads = uploads.filter(u => u.status === 'completed');
      if (completedUploads.length > 0) {
        setSuccess(`Successfully uploaded ${completedUploads.length} file(s)`);
        onUploadComplete?.(completedUploads);
      }

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Upload failed';
      setError(errorMessage);
      onUploadError?.(errorMessage);
    } finally {
      setIsUploading(false);
      abortControllerRef.current = null;
    }
  };

  const pauseUpload = () => {
    setIsPaused(true);
  };

  const resumeUpload = () => {
    setIsPaused(false);
  };

  const cancelUpload = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    setIsUploading(false);
    setIsPaused(false);
    setUploads(prev => prev.map(u => 
      u.status === 'uploading' ? { ...u, status: 'pending', progress: 0 } : u
    ));
  };

  const getStatistics = (): UploadStatistics => {
    const totalFiles = uploads.length;
    const completedFiles = uploads.filter(u => u.status === 'completed').length;
    const failedFiles = uploads.filter(u => u.status === 'error').length;
    const uploadingFiles = uploads.filter(u => u.status === 'uploading').length;
    const totalSize = uploads.reduce((sum, u) => sum + u.file.size, 0);
    const uploadedSize = uploads
      .filter(u => u.status === 'completed')
      .reduce((sum, u) => sum + u.file.size, 0);

    return {
      totalFiles,
      completedFiles,
      failedFiles,
      uploadingFiles,
      totalSize,
      uploadedSize
    };
  };

  const getStatusIcon = (status: FileUpload['status']) => {
    switch (status) {
      case 'completed':
        return <CheckCircle color="success" />;
      case 'error':
        return <ErrorIcon color="error" />;
      case 'uploading':
        return <CircularProgress size={20} />;
      default:
        return <UploadFile color="disabled" />;
    }
  };

  const statistics = getStatistics();

  return (
    <Box>
      {/* Configuration Display */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Upload Configuration</Typography>
            {showSettings && (
              <IconButton onClick={() => setShowSettingsDialog(true)}>
                <Settings />
              </IconButton>
            )}
          </Box>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <Chip 
                icon={<Info />} 
                label={`Entity: ${metadata.entityType}`} 
                variant="outlined" 
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Chip 
                icon={<StorageIcon />} 
                label={`Storage: ${metadata.storage}`} 
                variant="outlined" 
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Chip 
                label={`Quality: ${metadata.quality || 'Default'}`} 
                variant="outlined" 
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Chip 
                label={`Files: ${uploads.length}/${maxFiles}`} 
                variant="outlined" 
                color={uploads.length >= maxFiles ? 'error' : 'default'}
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
          Maximum {maxFiles} files, {fileService.formatFileSize(uploadConfig.maxFileSize)} each
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
                Files ({uploads.length})
              </Typography>
              <Box>
                {isUploading && (
                  <>
                    {isPaused ? (
                      <Button
                        size="small"
                        onClick={resumeUpload}
                        startIcon={<PlayArrow />}
                        sx={{ mr: 1 }}
                      >
                        Resume
                      </Button>
                    ) : (
                      <Button
                        size="small"
                        onClick={pauseUpload}
                        startIcon={<Pause />}
                        sx={{ mr: 1 }}
                      >
                        Pause
                      </Button>
                    )}
                    <Button
                      size="small"
                      onClick={cancelUpload}
                      startIcon={<Stop />}
                      color="error"
                      sx={{ mr: 1 }}
                    >
                      Cancel
                    </Button>
                  </>
                )}
                <Button
                  variant="contained"
                  onClick={() => uploadFiles()}
                  disabled={isUploading || uploads.every(u => u.status !== 'pending')}
                  startIcon={<CloudUpload />}
                >
                  {isUploading ? 'Uploading...' : 'Upload Files'}
                </Button>
              </Box>
            </Box>

            <List>
              {uploads.map((upload) => (
                <ListItem key={upload.id} divider>
                  <ListItemIcon>
                    {getStatusIcon(upload.status)}
                  </ListItemIcon>
                  <ListItemText
                    primary={upload.file.name}
                    secondary={
                      <Box>
                        <Typography variant="caption" display="block">
                          {fileService.formatFileSize(upload.file.size)}
                        </Typography>
                        {upload.status === 'uploading' && (
                          <LinearProgress 
                            variant="determinate" 
                            value={upload.progress} 
                            sx={{ mt: 1, height: 4, borderRadius: 2 }}
                          />
                        )}
                        {upload.error && (
                          <Typography variant="caption" color="error">
                            {upload.error}
                          </Typography>
                        )}
                      </Box>
                    }
                  />
                  <ListItemSecondaryAction>
                    <Tooltip title="Remove file">
                      <IconButton 
                        edge="end" 
                        onClick={() => removeFile(upload.id)}
                        disabled={upload.status === 'uploading'}
                      >
                        <Delete />
                      </IconButton>
                    </Tooltip>
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      {/* Statistics */}
      {uploads.length > 0 && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Upload Statistics
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={6} md={2}>
                <Box textAlign="center">
                  <Typography variant="h4" color="primary">
                    {statistics.totalFiles}
                  </Typography>
                  <Typography variant="body2">Total</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={2}>
                <Box textAlign="center">
                  <Typography variant="h4" color="success.main">
                    {statistics.completedFiles}
                  </Typography>
                  <Typography variant="body2">Completed</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={2}>
                <Box textAlign="center">
                  <Typography variant="h4" color="warning.main">
                    {statistics.uploadingFiles}
                  </Typography>
                  <Typography variant="body2">Uploading</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={2}>
                <Box textAlign="center">
                  <Typography variant="h4" color="error">
                    {statistics.failedFiles}
                  </Typography>
                  <Typography variant="body2">Failed</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={2}>
                <Box textAlign="center">
                  <Typography variant="h4" color="info.main">
                    {fileService.formatFileSize(statistics.totalSize)}
                  </Typography>
                  <Typography variant="body2">Total Size</Typography>
                </Box>
              </Grid>
              <Grid item xs={6} md={2}>
                <Box textAlign="center">
                  <Typography variant="h4" color="success.main">
                    {fileService.formatFileSize(statistics.uploadedSize)}
                  </Typography>
                  <Typography variant="body2">Uploaded</Typography>
                </Box>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Settings Dialog */}
      <Dialog 
        open={showSettingsDialog} 
        onClose={() => setShowSettingsDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Upload Settings</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Configure upload behavior and limits
          </Typography>
          {/* Add settings form here if needed */}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowSettingsDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FileUploadManager; 