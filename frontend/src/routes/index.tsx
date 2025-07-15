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

export default AppRoutes; 