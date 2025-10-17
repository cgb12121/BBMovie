import type React from "react"
import { useState, useEffect } from "react"
import {
  Link,
  useNavigate,
  useLocation
} from "react-router-dom"
import { Mail, Lock, Eye, EyeOff, AlertCircle, CheckCircle } from 'lucide-react'
import { FcGoogle } from "react-icons/fc"
import { FaFacebookF, FaGithub, FaDiscord, FaTwitter } from "react-icons/fa"
import { Button } from '../components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card'
import { Input } from '../components/ui/input'
import { Label } from '../components/ui/label'
import { Separator } from '../components/ui/separator'
import { Checkbox } from '../components/ui/checkbox'
import { Modal } from "antd"
import api from "../services/api"
import { useDispatch } from "react-redux"
import { setCredentials } from "../redux/authSlice"
import authService from "../services/authService"

interface LoginFormData {
  email: string
  password: string
  // remember?: boolean
}

const OAUTH_BASE_URL = process.env.OAUTH_BASE_URL;

const Login: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const location = useLocation()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notificationVisible, setNotificationVisible] = useState(false)
  const [socialLoading, setSocialLoading] = useState<string | null>(null)
  const [hasHandledOAuthRedirect, setHasHandledOAuthRedirect] = useState(false)

  const query = new URLSearchParams(location.search)
  const status = query.get("status")
  const messageFromQuery = query.get("message")

  // Create icon instances (react-icons must be called as functions)
  const googleIcon = FcGoogle({ size: 20 }) as JSX.Element;
  const facebookIcon = FaFacebookF({ size: 20 }) as JSX.Element;
  const githubIcon = FaGithub({ size: 20 }) as JSX.Element;
  const discordIcon = FaDiscord({ size: 20 }) as JSX.Element;
  const XIcon = FaTwitter({ size: 20 }) as JSX.Element;

  // Redirect if already logged in
  useEffect(() => {
    const isLoggedIn = !!localStorage.getItem("user")
    if (isLoggedIn) navigate("/")
  }, [navigate])

  // Handle OAuth2 redirect and fetch user data
  useEffect(() => {
    const isOAuthSuccess = status === "success" && (messageFromQuery?.includes("oauth2") ?? true);
    if (!isOAuthSuccess || hasHandledOAuthRedirect) return;

    setHasHandledOAuthRedirect(true);
    setNotificationVisible(true);

    let isSubscribed = true;
    const fetchUserData = async () => {
      try {
        const { data } = await api.get("/api/auth/oauth2-callback");
        if (!isSubscribed) return;
        onLoginSuccess(data.data);
      } catch (err: any) {
        if (!isSubscribed) return;
        console.error("Failed to fetch user data:", err);
        setError("Failed to load user data after OAuth2 login.");
        setNotificationVisible(false);
      }
    };

    fetchUserData();
    const timer = window.setTimeout(() => {
      if (!isSubscribed) return;
      setNotificationVisible(false);
      navigate("/", { replace: true });
    }, 5000);

    return () => {
      isSubscribed = false;
      window.clearTimeout(timer);
    };
  }, [status, messageFromQuery, hasHandledOAuthRedirect, navigate]);
  
  const onFinish = async (values: LoginFormData) => {
    setLoading(true);
    setError(null);
  
    try {
      const response = await authService.getInstance().login(values);
      if (response.success) {
        const { userResponse, authResponse, userAgentResponse } = response.data;
        dispatch(setCredentials({ user: userResponse, auth: authResponse, userAgent: userAgentResponse }));
        navigate("/");
      } else {
        setError(response.message || "Login failed");
      }
    } catch (err: any) {
      console.error(err);
      const msg = err?.response?.data?.message ?? "Login failed. Please check your credentials.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const onLoginSuccess = (userData: any) => {
    const { userResponse, authResponse, userAgentResponse } = userData;
  
    // Validate required data
    if (!authResponse?.accessToken) {
      setError("Invalid login response: missing access token");
      return;
    }
  
    const auth = authService.getInstance();
    
    auth.setTokens(authResponse.accessToken);
    if (userAgentResponse) {
      auth.setDeviceInfo(userAgentResponse);
    }
  
    dispatch(setCredentials({ 
      user: userResponse, 
      auth: authResponse, 
      userAgent: userAgentResponse 
    }));  
  
    localStorage.setItem("user", JSON.stringify(userResponse));
    localStorage.setItem("auth", JSON.stringify(authResponse));
    localStorage.setItem("userAgent", JSON.stringify(userAgentResponse));
    localStorage.setItem("accessToken", authResponse.accessToken);
  
    setTimeout(() => {
      navigate("/", { replace: true });
    }, 100);
  }

  const handleSocialLogin = (provider: string) => {
    try {
      setSocialLoading(provider)
      window.location.href = `${OAUTH_BASE_URL}/${provider.toLowerCase()}`
    } catch (err) {
      console.error("Failed to fetch user data:", err);
      setError("Failed to load user data after OAuth2 login.");
      navigate("/login", { replace: true });
    }
  }

  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({ email: '', password: '' });

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFinish(formData);
  };

  return (
    <div className="min-h-screen bg-black flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md space-y-8">
        {/* Logo */}
        <div className="text-center">
          <h1 className="text-red-600 text-4xl font-bold tracking-wider">BBMOVIE</h1>
          <p className="text-gray-400 mt-2">Sign in to continue watching</p>
        </div>

        {/* Login Card */}
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader>
            <CardTitle className="text-white text-2xl">Welcome Back</CardTitle>
            <CardDescription>Enter your credentials to access your account</CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <div className="mb-4 p-3 bg-red-900/20 border border-red-900 rounded-md flex items-start gap-2">
                <AlertCircle className="h-5 w-5 text-red-600 mt-0.5" />
                <div className="flex-1">
                  <p className="text-red-600 text-sm font-medium">Login Failed</p>
                  <p className="text-red-400 text-sm">{error}</p>
                </div>
                <button onClick={() => setError(null)} className="text-red-400 hover:text-red-300">×</button>
              </div>
            )}

            <form onSubmit={handleLoginSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email" className="text-white">Email</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="you@example.com"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="pl-10 bg-gray-800 border-gray-700 text-white placeholder:text-gray-500"
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="text-white">Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="pl-10 pr-10 bg-gray-800 border-gray-700 text-white placeholder:text-gray-500"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300"
                  >
                    {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Checkbox id="remember" />
                  <label htmlFor="remember" className="text-sm text-gray-400 cursor-pointer">
                    Remember me
                  </label>
                </div>
                <Link to="/password-reset" className="text-sm text-red-600 hover:text-red-500">
                  Forgot password?
                </Link>
              </div>

              <Button 
                type="submit" 
                disabled={loading}
                className="w-full bg-red-600 hover:bg-red-700 text-white" 
                size="lg"
              >
                {loading ? 'Signing in...' : 'Sign In'}
              </Button>
            </form>

            <div className="relative my-6">
              <Separator className="bg-gray-800" />
              <span className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-gray-900 px-2 text-sm text-gray-400">
                or continue with
              </span>
            </div>

            <div className="grid grid-cols-5 gap-2">
              <Button
                type="button"
                variant="outline"
                className="border-gray-700 hover:bg-gray-800"
                onClick={() => handleSocialLogin("Google")}
                disabled={!!socialLoading}
              >
                {googleIcon}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="border-gray-700 hover:bg-gray-800 text-blue-600"
                onClick={() => handleSocialLogin("Facebook")}
                disabled={!!socialLoading}
              >
                {facebookIcon}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="border-gray-700 hover:bg-gray-800 text-white"
                onClick={() => handleSocialLogin("Github")}
                disabled={!!socialLoading}
              >
                {githubIcon}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="border-gray-700 hover:bg-gray-800 text-indigo-500"
                onClick={() => handleSocialLogin("Discord")}
                disabled={!!socialLoading}
              >
                {discordIcon}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="border-gray-700 hover:bg-gray-800 text-blue-400"
                onClick={() => handleSocialLogin("x")}
                disabled={!!socialLoading}
              >
                {XIcon}
              </Button>
            </div>
          </CardContent>
          
          <CardFooter className="flex-col">
            <div className="text-center text-sm">
              <span className="text-gray-400">Don't have an account? </span>
              <Link to="/register" className="text-red-600 hover:text-red-500">
                Sign up
              </Link>
            </div>
          </CardFooter>
        </Card>
      </div>

      {/* Notification Modal */}
      <Modal
        open={notificationVisible && !!status}
        onCancel={() => setNotificationVisible(false)}
        footer={null}
        width={400}
        centered
        closable
      >
        <div className="text-center space-y-4 py-4">
          <div className="flex justify-center">
            {status === "success" ? (
              <CheckCircle className="h-16 w-16 text-green-600" />
            ) : (
              <AlertCircle className="h-16 w-16 text-red-600" />
            )}
          </div>
          <h3 className="text-xl font-semibold">{status === "success" ? "Success" : "Error"}</h3>
          <p className="text-gray-400">
            {messageFromQuery ?? (status === "success"
              ? "Operation completed successfully."
              : "An error occurred.")}
          </p>
          <Button 
            onClick={() => setNotificationVisible(false)}
            className="w-full bg-red-600 hover:bg-red-700"
          >
            {status === "success" ? "Continue" : "Try Again"}
          </Button>
        </div>
      </Modal>
    </div>
  )
}

export default Login
