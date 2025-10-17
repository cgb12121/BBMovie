import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Home from '../pages/Home';
import Login from '../pages/Login';
import Profile from '../pages/Profile';
import MovieDetail from '../pages/MovieDetail';
import Categories from '../pages/Categories';
import CategoryDetail from '../pages/CategoryDetail';
import SearchResults from '../pages/SearchResults';
import Forbidden from '../pages/Forbidden';
import EmailVerification from '../pages/EmailVerification';
import DeviceManagement from '../pages/DeviceManagement';
import FileUpload from '../pages/FileUpload';
import FileManagement from '../pages/FileManagement';
import Registration from '../pages/Registration';
import { Layout } from 'antd';
import PrivateRoute from '../components/security/PrivateRoute';
import Watchlist from '../pages/Watchlist';
import PasswordReset from '../pages/PasswordReset';
import Movies from '../pages/Movies';
import NotFound from '../pages/NotFound';
import Subscriptions from '../pages/Subscriptions';

const AppRoutes: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Registration />} />
        <Route path="/email-verification" element={<EmailVerification />} />
        <Route path="/password-reset" element={<PasswordReset />} />
        <Route path="/forbidden" element={<Forbidden />} />

        {/* Protected routes with layout */}
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="movies/:id" element={<MovieDetail />} />
          <Route path="categories" element={<Categories />} />
          <Route path="categories/:id" element={<CategoryDetail />} />
          <Route path="search" element={<SearchResults />} />
          <Route path="watchlist" element={<PrivateRoute><Watchlist /></PrivateRoute>} />
          <Route path="subscriptions" element={<PrivateRoute><Subscriptions /></PrivateRoute>} />
          <Route path="profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
          <Route path="devices" element={<PrivateRoute><DeviceManagement /></PrivateRoute>} />
          
          {/* File service routes */}
          <Route path="upload" element={<PrivateRoute><FileUpload /></PrivateRoute>} />
          <Route path="files" element={<PrivateRoute><FileManagement /></PrivateRoute>} />
        </Route>

        {/* Catch all route */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
};

interface AppRoute {
  path: string;
  element: React.ReactNode;
  name: string;
}

export const routes: AppRoute[] = [
  {
      path: '/',
      element: <Home />,
      name: 'Home'
  },
  {
      path: '/login',
      element: <Login />,
      name: 'Login'
  },
  {
      path: '/register',
      element: <Registration />,
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
      path: '/search',
      element: <SearchResults />,
      name: 'Search Results'
  },
  {
      path: '/not-found',
      element: <NotFound />,
      name: 'Not Found'
  },
  {
      path: '/forbidden',
      element: <Forbidden/>,
      name: 'Forbidden'
  },
  {
      path: '/profile',
      element: <Profile/>,
      name: 'Profile'
  },
]; 

export default AppRoutes; 
