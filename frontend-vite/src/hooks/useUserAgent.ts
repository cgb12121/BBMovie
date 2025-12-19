import { useState, useEffect } from 'react';
import authService from '../services/authService';

interface UserAgentResponse {
  deviceName: string;
  deviceOs: string;
  deviceIpAddress: string;
  browser: string;
  browserVersion: string;
}

const STORAGE_KEY = 'user_agent_info';

export const useUserAgent = () => {
  const [userAgent, setUserAgent] = useState<UserAgentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchUserAgent = async () => {
    try {
      setLoading(true);
      const data = await authService.getUserAgent();
      setUserAgent(data);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
      setError(null);
    } catch (err) {
      setError(err as Error);
      const storedData = localStorage.getItem(STORAGE_KEY);
      if (storedData) {
        setUserAgent(JSON.parse(storedData));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const storedData = localStorage.getItem(STORAGE_KEY);
    if (storedData) {
      setUserAgent(JSON.parse(storedData));
      setLoading(false);
    }
    
    fetchUserAgent();
  }, []);

  return {
    userAgent,
    loading,
    error,
    refreshUserAgent: fetchUserAgent
  };
}; 
