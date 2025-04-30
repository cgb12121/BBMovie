import { RouteObject } from 'react-router-dom';
import Home from '../pages/Home';
import Registration from '../pages/Registration';
import EmailVerification from '../pages/EmailVerification';
import Login from '../pages/Login';
import Movies from '../pages/Movies';
import MovieDetail from '../pages/MovieDetail';
import Categories from '../pages/Categories';
import CategoryDetail from '../pages/CategoryDetail';
import NotFound from '../pages/NotFound';
import Forbidden from '../pages/Forbidden';
import SearchResults from '../pages/SearchResults';

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
        element: <SearchResults />
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
]; 