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
import DevSecureStream from '../pages/DevSecureStream';
import Watchlist from '../pages/Watchlist';
import PasswordReset from '../pages/PasswordReset';
import Movies from '../pages/Movies';
import NotFound from '../pages/NotFound';
import Subscriptions from '../pages/Subscriptions';
import Settings from '../pages/Settings';
import StudentVerificationApply from '../pages/StudentVerificationApply';
import StudentApplicationsAdmin from '../pages/StudentApplicationsAdmin';
import InternalStudentFinalize from '../pages/InternalStudentFinalize';
import JwkAdmin from '../pages/JwkAdmin';
import StudentApplicationLookup from '../pages/StudentApplicationLookup';
import PersonalizationRecommendations from '../pages/PersonalizationRecommendations';
import WatchHistoryResume from '../pages/WatchHistoryResume';

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

          {/* Dev / debug routes */}
          <Route path="dev/secure-stream" element={<PrivateRoute><DevSecureStream /></PrivateRoute>} />
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
      path: '/password-reset',
      element: <PasswordReset />,
      name: 'Password Reset'
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
      element: <PrivateRoute><Profile/></PrivateRoute>,
      name: 'Profile'
  },
  {
      path: '/watchlist',
      element: <PrivateRoute><Watchlist/></PrivateRoute>,
      name: 'Watchlist'
  },
  {
      path: '/subscriptions',
      element: <PrivateRoute><Subscriptions/></PrivateRoute>,
      name: 'Subscriptions'
  },
  {
      path: '/devices',
      element: <PrivateRoute><DeviceManagement/></PrivateRoute>,
      name: 'Devices'
  },
  {
      path: '/settings',
      element: <PrivateRoute><Settings/></PrivateRoute>,
      name: 'Settings'
  },
  {
      path: '/student/apply',
      element: <PrivateRoute><StudentVerificationApply/></PrivateRoute>,
      name: 'Student Verification Apply'
  },
  {
      path: '/student/application',
      element: <PrivateRoute><StudentApplicationLookup/></PrivateRoute>,
      name: 'Student Application Lookup'
  },
  {
      path: '/personalization',
      element: <PrivateRoute><PersonalizationRecommendations/></PrivateRoute>,
      name: 'Personalization Recommendations'
  },
  {
      path: '/watch-history',
      element: <PrivateRoute><WatchHistoryResume/></PrivateRoute>,
      name: 'Watch History Resume'
  },
  {
      path: '/admin/student-applications',
      element: <PrivateRoute requiredRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']}><StudentApplicationsAdmin/></PrivateRoute>,
      name: 'Student Applications Admin'
  },
  {
      path: '/admin/jwks',
      element: <PrivateRoute requiredRoles={['ROLE_ADMIN']}><JwkAdmin/></PrivateRoute>,
      name: 'JWK Admin'
  },
  {
      path: '/internal/student-finalize',
      element: <PrivateRoute requiredRoles={['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']}><InternalStudentFinalize/></PrivateRoute>,
      name: 'Internal Student Finalize'
  },
  {
      path: '/upload',
      element: <PrivateRoute><FileUpload /></PrivateRoute>,
      name: 'File Upload'
  },
  {
      path: '/files',
      element: <PrivateRoute><FileManagement /></PrivateRoute>,
      name: 'File Management'
  },
  {
      path: '/dev/secure-stream',
      element: <PrivateRoute><DevSecureStream/></PrivateRoute>,
      name: 'Dev Secure Stream'
  },
  {
      path: '*',
      element: <NotFound />,
      name: 'Not Found Catch All'
  }
]; 

export default AppRoutes; 
