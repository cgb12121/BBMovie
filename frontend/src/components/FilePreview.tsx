import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardMedia,
  CardContent,
  Typography,
  IconButton,
  Chip,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  LinearProgress,
  Alert
} from '@mui/material';
import {
  Delete,
  Visibility,
  Download,
  Image,
  VideoFile,
  Description,
  InsertDriveFile,
  PlayArrow,
  Pause
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

interface FilePreviewProps {
  file: File;
  onRemove?: () => void;
  showActions?: boolean;
  maxWidth?: number;
  maxHeight?: number;
}

const PreviewCard = styled(Card)(({ theme }) => ({
  position: 'relative',
  overflow: 'hidden',
  transition: 'transform 0.2s ease-in-out',
  '&:hover': {
    transform: 'scale(1.02)',
  },
}));

const FilePreview: React.FC<FilePreviewProps> = ({
  file,
  onRemove,
  showActions = true,
  maxWidth = 200,
  maxHeight = 200
}) => {
  const [preview, setPreview] = useState<string | null>(null);
  const [isVideo, setIsVideo] = useState(false);
  const [isImage, setIsImage] = useState(false);
  const [showFullPreview, setShowFullPreview] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    generatePreview();
    return () => {
      // Cleanup preview URL
      if (preview !== null && preview.startsWith('blob:')) {
        URL.revokeObjectURL(preview);
      }
    };
  }, [file]);

  const generatePreview = () => {
    try {
      if (file.type.startsWith('image/')) {
        setIsImage(true);
        setIsVideo(false);
        const url = URL.createObjectURL(file);
        setPreview(url);
      } else if (file.type.startsWith('video/')) {
        setIsVideo(true);
        setIsImage(false);
        const url = URL.createObjectURL(file);
        setPreview(url);
      } else {
        setIsImage(false);
        setIsVideo(false);
        setPreview(null);
      }
    } catch (err) {
      setError('Failed to generate preview');
      console.error('Preview generation error:', err);
    }
  };

  const getFileIcon = () => {
    if (isImage) return <Image />;
    if (isVideo) return <VideoFile />;
    if (file.type === 'application/pdf') return <Description />;
    return <InsertDriveFile />;
  };

  const getFileTypeColor = () => {
    if (isImage) return 'success';
    if (isVideo) return 'warning';
    if (file.type === 'application/pdf') return 'error';
    return 'default';
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const handlePlayPause = () => {
    setIsPlaying(!isPlaying);
  };

  const handleDownload = () => {
    const url = URL.createObjectURL(file);
    const a = document.createElement('a');
    a.href = url;
    a.download = file.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const renderPreviewContent = () => {
    if (error) {
      return (
        <Box
          sx={{
            width: maxWidth,
            height: maxHeight,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: 'grey.100'
          }}
        >
          <Alert severity="error" sx={{ maxWidth: '90%' }}>
            {error}
          </Alert>
        </Box>
      );
    }

    if (isImage && preview) {
      return (
        <CardMedia
          component="img"
          image={preview}
          alt={file.name}
          sx={{
            width: maxWidth,
            height: maxHeight,
            objectFit: 'cover'
          }}
        />
      );
    }

    if (isVideo && preview) {
      return (
        <Box sx={{ position: 'relative', width: maxWidth, height: maxHeight }}>
          <video
            src={preview}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover'
            }}
            onPlay={() => setIsPlaying(true)}
            onPause={() => setIsPlaying(false)}
          />
          <Box
            sx={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              bgcolor: 'rgba(0,0,0,0.5)',
              borderRadius: '50%',
              p: 1,
              cursor: 'pointer'
            }}
            onClick={handlePlayPause}
          >
            {isPlaying ? <Pause sx={{ color: 'white' }} /> : <PlayArrow sx={{ color: 'white' }} />}
          </Box>
        </Box>
      );
    }

    // Default file preview
    return (
      <Box
        sx={{
          width: maxWidth,
          height: maxHeight,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: 'grey.100',
          p: 2
        }}
      >
        {getFileIcon()}
        <Typography variant="caption" sx={{ mt: 1, textAlign: 'center' }}>
          {file.name}
        </Typography>
      </Box>
    );
  };

  return (
    <>
      <PreviewCard sx={{ maxWidth: maxWidth }}>
        {renderPreviewContent()}
        
        <CardContent sx={{ p: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <Typography variant="caption" noWrap sx={{ flexGrow: 1 }}>
              {file.name}
            </Typography>
            {onRemove && (
              <Tooltip title="Remove file">
                <IconButton size="small" onClick={onRemove}>
                  <Delete fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </Box>
          
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Chip
              label={formatFileSize(file.size)}
              size="small"
              variant="outlined"
            />
            <Chip
              icon={getFileIcon()}
              label={file.type || 'Unknown'}
              size="small"
              color={getFileTypeColor()}
              variant="outlined"
            />
          </Box>

          {showActions && (
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
              <Tooltip title="Preview">
                <IconButton 
                  size="small" 
                  onClick={() => setShowFullPreview(true)}
                  disabled={!preview}
                >
                  <Visibility fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Download">
                <IconButton size="small" onClick={handleDownload}>
                  <Download fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          )}
        </CardContent>
      </PreviewCard>

      {/* Full Preview Dialog */}
      <Dialog
        open={showFullPreview}
        onClose={() => setShowFullPreview(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          {file.name}
          <Typography variant="caption" display="block" color="text.secondary">
            {formatFileSize(file.size)} • {file.type}
          </Typography>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
            {isImage && preview && (
              <img
                src={preview}
                alt={file.name}
                style={{
                  maxWidth: '100%',
                  maxHeight: '70vh',
                  objectFit: 'contain'
                }}
              />
            )}
            {isVideo && preview && (
              <video
                src={preview}
                controls
                style={{
                  maxWidth: '100%',
                  maxHeight: '70vh'
                }}
              />
            )}
            {!isImage && !isVideo && (
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  p: 4
                }}
              >
                {getFileIcon()}
                <Typography variant="h6" sx={{ mt: 2 }}>
                  {file.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Preview not available for this file type
                </Typography>
              </Box>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDownload} startIcon={<Download />}>
            Download
          </Button>
          <Button onClick={() => setShowFullPreview(false)}>
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default FilePreview; 