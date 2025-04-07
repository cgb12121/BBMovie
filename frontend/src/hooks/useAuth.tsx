import React, { createContext, useContext, useState, useEffect, FC, ReactNode, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { setToken, removeToken } from '../utils/auth';

interface User {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    roles: string[];
}

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    login: (email: string, password: string) => Promise<void>;
    logout: () => void;
    register: (data: RegisterData) => Promise<void>;
    updateProfile: (data: UpdateProfileData) => Promise<void>;
    loading: boolean;
    error: string | null;
}

interface RegisterData {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
}

interface UpdateProfileData {
    firstName?: string;
    lastName?: string;
    email?: string;
    currentPassword?: string;
    newPassword?: string;
}

interface AuthProviderProps {
    children: ReactNode;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider: FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        const checkAuth = async () => {
            try {
                const response = await api.get('/auth/me');
                setUser(response.data);
            } catch (error) {
                setUser(null);
            } finally {
                setLoading(false);
            }
        };

        checkAuth();
    }, []);

    const login = async (email: string, password: string) => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.post('/auth/login', { email, password });
            const { token, user } = response.data;
            setToken(token);
            setUser(user);
            navigate('/');
        } catch (error: any) {
            setError(error.response?.data?.message || 'Login failed');
            throw error;
        } finally {
            setLoading(false);
        }
    };

    const logout = () => {
        removeToken();
        setUser(null);
        navigate('/login');
    };

    const register = async (data: RegisterData) => {
        try {
            setLoading(true);
            setError(null);
            await api.post('/auth/register', data);
            navigate('/login');
        } catch (error: any) {
            setError(error.response?.data?.message || 'Registration failed');
            throw error;
        } finally {
            setLoading(false);
        }
    };

    const updateProfile = async (data: UpdateProfileData) => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.put('/auth/profile', data);
            setUser(response.data);
        } catch (error: any) {
            setError(error.response?.data?.message || 'Profile update failed');
            throw error;
        } finally {
            setLoading(false);
        }
    };

    const value = useMemo(() => ({
        user,
        isAuthenticated: !!user,
        login,
        logout,
        register,
        updateProfile,
        loading,
        error
    }), [user, loading, error]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};