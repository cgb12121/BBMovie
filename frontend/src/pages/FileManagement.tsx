import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Tabs,
  Tab,
  Card,
  CardContent,
  Grid,
  Button,
  TextField,
  InputAdornment,
  IconButton,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Tooltip,
  LinearProgress
} from '@mui/material';
import {
  Search,
  FilterList,
  Add,
  Refresh,
  Delete,
  Download,
  Visibility,
  CloudUpload,
  Storage,
  Category,
  CalendarToday,
  Storage as StorageIcon
} from '@mui/icons-material';
import FileUploadManager from '../components/FileUploadManager';
import FilePreview from '../components/FilePreview';
import { UploadMetadata, FileUpload } from '../types/fileUpload';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`file-tabpanel-${index}`}
      aria-labelledby={`file-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

interface FileRecord {
  id: string;
  fileName: string;
  extension: string;
  size: number;
  tempDir: string;
  tempStoreFor: string;
  uploadedBy: string;
  isRemoved: boolean;
  createdAt: string;
  removedAt?: string;
  url?: string;
  publicId?: string;
}

const FileManagement: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [uploadMetadata, setUploadMetadata] = useState<UploadMetadata>({
    entityType: 'MOVIE',
    storage: 'LOCAL',
    quality: ''
  });
  const [files, setFiles] = useState<FileRecord[]>([]);
  const [filteredFiles, setFilteredFiles] = useState<FileRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterEntityType, setFilterEntityType] = useState<string>('all');
  const [filterStorage, setFilterStorage] = useState<string>('all');
  const [showUploadDialog, setShowUploadDialog] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Mock data - replace with actual API calls
  useEffect(() => {
    loadFiles();
  }, []);

  useEffect(() => {
    filterFiles();
  }, [files, searchTerm, filterEntityType, filterStorage]);

  const loadFiles = async () => {
    setLoading(true);
    try {
      // Mock data - replace with actual API call
      const mockFiles: FileRecord[] = [
        {
          id: '1',
          fileName: 'movie_poster.jpg',
          extension: 'jpg',
          size: 2048576,
          tempDir: '/uploads/posters',
          tempStoreFor: 'MOVIE',
          uploadedBy: 'user1',
          isRemoved: false,
          createdAt: '2024-01-15T10:30:00Z',
          url: 'https://example.com/posters/movie_poster.jpg'
        },
        {
          id: '2',
          fileName: 'trailer.mp4',
          extension: 'mp4',
          size: 52428800,
          tempDir: '/uploads/trailers',
          tempStoreFor: 'TRAILER',
          uploadedBy: 'user1',
          isRemoved: false,
          createdAt: '2024-01-14T15:45:00Z',
          url: 'https://example.com/trailers/trailer.mp4'
        },
        {
          id: '3',
          fileName: 'document.pdf',
          extension: 'pdf',
          size: 1048576,
          tempDir: '/uploads/documents',
          tempStoreFor: 'POSTER',
          uploadedBy: 'user2',
          isRemoved: true,
          createdAt: '2024-01-13T09:20:00Z',
          removedAt: '2024-01-14T11:30:00Z'
        }
      ];
      setFiles(mockFiles);
    } catch (err) {
      setError('Failed to load files');
      console.error('Load files error:', err);
    } finally {
      setLoading(false);
    }
  };

  const filterFiles = () => {
    let filtered = files;

    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(file =>
        file.fileName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        file.extension.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Entity type filter
    if (filterEntityType !== 'all') {
      filtered = filtered.filter(file => file.tempStoreFor === filterEntityType);
    }

    // Storage filter
    if (filterStorage !== 'all') {
      filtered = filtered.filter(file => file.tempDir.includes(filterStorage.toLowerCase()));
    }

    setFilteredFiles(filtered);
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleUploadComplete = (uploads: FileUpload[]) => {
    console.log('Uploads completed:', uploads);
    setShowUploadDialog(false);
    loadFiles(); // Refresh file list
  };

  const handleUploadError = (error: string) => {
    setError(error);
  };

  const deleteFile = async (fileId: string) => {
    try {
      // Mock API call - replace with actual delete endpoint
      console.log('Deleting file:', fileId);
      setFiles(prev => prev.filter(file => file.id !== fileId));
    } catch (err) {
      setError('Failed to delete file');
      console.error('Delete file error:', err);
    }
  };

  const downloadFile = (file: FileRecord) => {
    if (file.url) {
      const a = document.createElement('a');
      a.href = file.url;
      a.download = file.fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStorageType = (tempDir: string) => {
    if (tempDir.includes('s3')) return 'S3';
    if (tempDir.includes('cloudinary')) return 'CLOUDINARY';
    return 'LOCAL';
  };

  const getEntityTypeColor = (entityType: string) => {
    switch (entityType) {
      case 'MOVIE': return 'primary';
      case 'TRAILER': return 'warning';
      case 'POSTER': return 'success';
      default: return 'default';
    }
  };

  const getStorageColor = (storage: string) => {
    switch (storage) {
      case 'S3': return 'info';
      case 'CLOUDINARY': return 'secondary';
      case 'LOCAL': return 'default';
      default: return 'default';
    }
  };

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto', p: 3 }}>
      <Typography variant="h4" gutterBottom sx={{ mb: 4 }}>
        File Management
      </Typography>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="File Browser" />
          <Tab label="Upload Files" />
          <Tab label="Upload History" />
        </Tabs>
      </Box>

      {/* File Browser Tab */}
      <TabPanel value={tabValue} index={0}>
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h6">Files</Typography>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={() => setShowUploadDialog(true)}
              >
                Upload Files
              </Button>
            </Box>

            {/* Search and Filters */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  placeholder="Search files..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <Search />
                      </InputAdornment>
                    ),
                  }}
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Entity Type</InputLabel>
                  <Select
                    value={filterEntityType}
                    label="Entity Type"
                    onChange={(e) => setFilterEntityType(e.target.value)}
                  >
                    <MenuItem value="all">All Types</MenuItem>
                    <MenuItem value="MOVIE">Movie</MenuItem>
                    <MenuItem value="TRAILER">Trailer</MenuItem>
                    <MenuItem value="POSTER">Poster</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Storage</InputLabel>
                  <Select
                    value={filterStorage}
                    label="Storage"
                    onChange={(e) => setFilterStorage(e.target.value)}
                  >
                    <MenuItem value="all">All Storage</MenuItem>
                    <MenuItem value="LOCAL">Local</MenuItem>
                    <MenuItem value="S3">S3</MenuItem>
                    <MenuItem value="CLOUDINARY">Cloudinary</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} md={2}>
                <Button
                  fullWidth
                  variant="outlined"
                  startIcon={<Refresh />}
                  onClick={loadFiles}
                  disabled={loading}
                >
                  Refresh
                </Button>
              </Grid>
            </Grid>

            {/* Error Alert */}
            {error && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                {error}
              </Alert>
            )}

            {/* Loading Progress */}
            {loading && (
              <Box sx={{ mb: 2 }}>
                <LinearProgress />
                <Typography variant="caption" sx={{ mt: 1 }}>
                  Loading files...
                </Typography>
              </Box>
            )}

            {/* Files Table */}
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>File</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Storage</TableCell>
                    <TableCell>Size</TableCell>
                    <TableCell>Uploaded By</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredFiles.map((file) => (
                    <TableRow key={file.id}>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                            {file.fileName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={file.tempStoreFor}
                          size="small"
                          color={getEntityTypeColor(file.tempStoreFor)}
                          icon={<Category />}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getStorageType(file.tempDir)}
                          size="small"
                          color={getStorageColor(getStorageType(file.tempDir))}
                          icon={<StorageIcon />}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {formatFileSize(file.size)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {file.uploadedBy}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {formatDate(file.createdAt)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={file.isRemoved ? 'Removed' : 'Active'}
                          size="small"
                          color={file.isRemoved ? 'error' : 'success'}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          {file.url && (
                            <>
                              <Tooltip title="Download">
                                <IconButton size="small" onClick={() => downloadFile(file)}>
                                  <Download fontSize="small" />
                                </IconButton>
                              </Tooltip>
                              <Tooltip title="View">
                                <IconButton size="small" href={file.url} target="_blank">
                                  <Visibility fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            </>
                          )}
                          <Tooltip title="Delete">
                            <IconButton 
                              size="small" 
                              onClick={() => deleteFile(file.id)}
                              color="error"
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {filteredFiles.length === 0 && !loading && (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <Typography variant="h6" color="text.secondary">
                  No files found
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Try adjusting your search or filters
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </TabPanel>

      {/* Upload Files Tab */}
      <TabPanel value={tabValue} index={1}>
        <FileUploadManager
          metadata={uploadMetadata}
          onUploadComplete={handleUploadComplete}
          onUploadError={handleUploadError}
          maxFiles={20}
          showSettings={true}
          autoUpload={false}
        />
      </TabPanel>

      {/* Upload History Tab */}
      <TabPanel value={tabValue} index={2}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Recent Uploads
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Track your recent file uploads and their status
            </Typography>
            
            {/* Upload statistics */}
            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid item xs={12} md={3}>
                <Card variant="outlined">
                  <CardContent sx={{ textAlign: 'center' }}>
                    <Typography variant="h4" color="primary">
                      {files.filter(f => !f.isRemoved).length}
                    </Typography>
                    <Typography variant="body2">Active Files</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={3}>
                <Card variant="outlined">
                  <CardContent sx={{ textAlign: 'center' }}>
                    <Typography variant="h4" color="success.main">
                      {files.filter(f => f.tempStoreFor === 'MOVIE').length}
                    </Typography>
                    <Typography variant="body2">Movies</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={3}>
                <Card variant="outlined">
                  <CardContent sx={{ textAlign: 'center' }}>
                    <Typography variant="h4" color="warning.main">
                      {files.filter(f => f.tempStoreFor === 'TRAILER').length}
                    </Typography>
                    <Typography variant="body2">Trailers</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={3}>
                <Card variant="outlined">
                  <CardContent sx={{ textAlign: 'center' }}>
                    <Typography variant="h4" color="info.main">
                      {files.filter(f => f.tempStoreFor === 'POSTER').length}
                    </Typography>
                    <Typography variant="body2">Posters</Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            {/* Recent uploads list */}
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>File</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {files
                    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                    .slice(0, 10)
                    .map((file) => (
                    <TableRow key={file.id}>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                          {file.fileName}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={file.tempStoreFor}
                          size="small"
                          color={getEntityTypeColor(file.tempStoreFor)}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {formatDate(file.createdAt)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={file.isRemoved ? 'Removed' : 'Active'}
                          size="small"
                          color={file.isRemoved ? 'error' : 'success'}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      </TabPanel>

      {/* Upload Dialog */}
      <Dialog
        open={showUploadDialog}
        onClose={() => setShowUploadDialog(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>Upload Files</DialogTitle>
        <DialogContent>
          <FileUploadManager
            metadata={uploadMetadata}
            onUploadComplete={handleUploadComplete}
            onUploadError={handleUploadError}
            maxFiles={10}
            showSettings={false}
            autoUpload={true}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowUploadDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FileManagement; 
