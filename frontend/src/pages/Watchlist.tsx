// frontend/src/pages/Watchlist.tsx
import React from 'react';
import { Box, Typography } from '@mui/material';

const Watchlist: React.FC = () => {
  // Replace with actual data fetching logic
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        My Watchlist
      </Typography>
      <Typography variant="body1">
        (Your saved movies will appear here.)
      </Typography>
    </Box>
  );
};

export default Watchlist;