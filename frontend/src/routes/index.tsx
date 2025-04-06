import React from 'react';
import { RouteObject } from 'react-router-dom';
import Home from '../pages/Home';
import RegistrationForm from '../components/auth/RegistrationForm';
import EmailVerification from '../components/auth/EmailVerification';
import LoginForm from '../components/auth/LoginForm';
import Movies from '../pages/Movies';
import MovieDetail from '../pages/MovieDetail';
import Categories from '../pages/Categories';
import CategoryDetail from '../pages/CategoryDetail';
import Profile from '../pages/Profile';
import NotFound from '../pages/NotFound';

export type AppRoute = RouteObject & {
    name?: string;
    isProtected?: boolean;
    roles?: string[];
};

export const routes: AppRoute[] = [
    {
        path: '/',
        element: <Home />,
        name: 'Home'
    },
    {
        path: '/login',
        element: <LoginForm />,
        name: 'Login'
    },
    {
        path: '/register',
        element: <RegistrationForm />,
        name: 'Register'
    },
    {
        path: '/verify-email',
        element: <EmailVerification />,
        name: 'Email Verification'
    },
    {
        path: '/movies',
        element: <Movies />,
        name: 'Movies'
    },
    {
        path: '/movies/:id',
        element: <MovieDetail />,
        name: 'Movie Detail'
    },
    {
        path: '/categories',
        element: <Categories />,
        name: 'Categories'
    },
    {
        path: '/categories/:id',
        element: <CategoryDetail />,
        name: 'Category Detail'
    },
    {
        path: '/profile',
        element: <Profile />,
        name: 'Profile',
        isProtected: true
    },
    {
        path: '*',
        element: <NotFound />,
        name: 'Not Found'
    }
]; 