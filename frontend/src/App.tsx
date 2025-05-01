import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import GlobalStyles from './styles/GlobalStyles';
import Navbar from './components/Navbar';
import { routes } from './routes';
import CsrfProvider from './components/security/CsrfProvider';

const App: React.FC = () => {
   return (
    <CsrfProvider>
      <ConfigProvider theme={{ token: { colorPrimary: '#1890ff', borderRadius: 4 } }}>
        <Router>
          <GlobalStyles />
          <Navbar />
          <Routes>
            {routes.map((route) => (
              <Route 
                key={route.path}
                path={route.path} 
                element={route.element} />
            ))}
          </Routes>
        </Router>
      </ConfigProvider>
    </CsrfProvider>
  );
};

export default App;
