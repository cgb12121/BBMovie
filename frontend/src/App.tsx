import React from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import GlobalStyles from './styles/GlobalStyles';
import Navbar from './components/Navbar';
import { routes } from './routes';
import MfaProvider from './components/security/MfaProvider';

const MainLayout: React.FC = () => {
  const location = useLocation();
  const hideNavbarPaths = ['/login', '/register', '/verify-email'];
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
                element={route.element} />
            ))}
        </Routes>
      </>
  );
};

const App: React.FC = () => {
   return (
       <ConfigProvider theme={{ token: { colorPrimary: '#1890ff', borderRadius: 4 } }}>
         <Router>
          <GlobalStyles />
            <MainLayout/>
        </Router>
      </ConfigProvider>
  );
};

export default App;
