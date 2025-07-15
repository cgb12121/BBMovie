import React, { useState } from 'react';
import { Box, Typography, TextField, Button, Alert } from '@mui/material';

const PasswordReset: React.FC = () => {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Replace with actual API call
    setSent(true);
  };

  return (
    <Box sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" gutterBottom>
        Reset Password
      </Typography>
      {sent ? (
        <Alert severity="success">If this email exists, a reset link has been sent.</Alert>
      ) : (
        <form onSubmit={handleSubmit}>
          <TextField
            label="Email"
            type="email"
            fullWidth
            required
            margin="normal"
            value={email}
            onChange={e => setEmail(e.target.value)}
          />
          <Button type="submit" variant="contained" fullWidth>
            Send Reset Link
          </Button>
        </form>
      )}
    </Box>
  );
};

export default PasswordReset;