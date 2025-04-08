import React from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import GlobalStyles from './styles/GlobalStyles';
import Navbar from './components/Navbar';
import { routes } from './routes';
import { AuthProvider } from './hooks/useAuth';
import RouteGuard from './components/auth/RouteGuard';

const MainLayout: React.FC = () => {
    const location = useLocation();
    const hideNavbarPaths = ['/login', '/register'];
    const shouldHideNavbar = hideNavbarPaths.includes(location.pathname);

    return (
        <>
            <GlobalStyles />
            {!shouldHideNavbar && <Navbar />}
            <Routes>
                {routes.map((route) => (
                    <Route
                        key={route.path}
                        path={route.path}
                        element={
                            <RouteGuard
                                isProtected={route.isProtected ?? false}
                                roles={route.roles ?? []}
                            >
                                {route.element}
                            </RouteGuard>
                        }
                    />
                ))}
            </Routes>
        </>
    );
};

const App: React.FC = () => {
    return (
        <ConfigProvider
            theme={{
                token: { colorPrimary: '#1890ff', borderRadius: 4 },
            }}
        >
            <Router>
                <AuthProvider>
                    <MainLayout />
                </AuthProvider>
            </Router>
        </ConfigProvider>
    );
};

export default App;