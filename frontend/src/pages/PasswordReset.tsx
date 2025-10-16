import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import { Mail, ArrowLeft, Lock, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import api from '../services/api';

const PasswordReset: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      await api.post('/api/auth/forgot-password', { email });
      setSent(true);
      message.success('Password reset link sent to your email.');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to send reset link. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-black flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md space-y-8">
        {/* Logo */}
        <div className="text-center">
          <h1 className="text-red-600 text-4xl font-bold tracking-wider">BBMOVIE</h1>
        </div>

        {/* Reset Form */}
        <Card className="bg-gray-900 border-gray-800">
          {!sent ? (
            <>
              <CardHeader className="space-y-4">
                <div className="mx-auto bg-red-900/20 border border-red-800 rounded-full p-4 w-fit">
                  <Lock className="h-12 w-12 text-red-500" />
                </div>
                <CardTitle className="text-white text-2xl text-center">Reset Password</CardTitle>
                <CardDescription className="text-center">
                  Enter your email address and we'll send you a link to reset your password
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="email" className="text-white">Email Address</Label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                      <Input
                        id="email"
                        type="email"
                        placeholder="you@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="pl-10 bg-gray-800 border-gray-700 text-white placeholder:text-gray-500"
                        required
                      />
                    </div>
                  </div>

                  <Button 
                    type="submit" 
                    className="w-full bg-red-600 hover:bg-red-700 text-white" 
                    size="lg"
                    disabled={loading}
                  >
                    {loading ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        Sending...
                      </>
                    ) : (
                      'Send Reset Link'
                    )}
                  </Button>

                  <Button
                    type="button"
                    variant="ghost"
                    className="w-full text-gray-400 hover:text-white gap-2"
                    onClick={() => navigate('/login')}
                  >
                    <ArrowLeft className="h-4 w-4" />
                    Back to Login
                  </Button>
                </form>
              </CardContent>
            </>
          ) : (
            <>
              <CardHeader className="space-y-4">
                <div className="mx-auto bg-green-900/20 border border-green-800 rounded-full p-4 w-fit">
                  <Mail className="h-12 w-12 text-green-500" />
                </div>
                <CardTitle className="text-white text-2xl text-center">Check Your Email</CardTitle>
                <CardDescription className="text-center">
                  We've sent a password reset link to<br />
                  <span className="text-white">{email}</span>
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="bg-gray-800 border border-gray-700 rounded-lg p-4">
                  <p className="text-sm text-gray-300 text-center">
                    Click the link in the email to reset your password. The link will expire in 1 hour.
                  </p>
                </div>

                <div className="text-center space-y-4">
                  <p className="text-sm text-gray-400">
                    Didn't receive the email?
                  </p>
                  <Button
                    variant="outline"
                    className="w-full border-gray-700 text-white hover:bg-gray-800"
                    onClick={() => setSent(false)}
                  >
                    Try Again
                  </Button>
                </div>

                <Button
                  className="w-full bg-red-600 hover:bg-red-700 text-white"
                  onClick={() => navigate('/login')}
                >
                  Back to Login
                </Button>
              </CardContent>
            </>
          )}
        </Card>
      </div>
    </div>
  );
};

export default PasswordReset;