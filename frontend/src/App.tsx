import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import RegistrationForm from './components/auth/RegistrationForm';
import EmailVerification from './components/auth/EmailVerification';
import { ConfigProvider } from 'antd';

const App: React.FC = () => {
    return (
        <ConfigProvider
            theme={{
                token: {
                    colorPrimary: '#1890ff',
                    borderRadius: 4,
                },
            }}
        >
            <Router>
                <Routes>
                    <Route path="/register" element={<RegistrationForm />} />
                    <Route path="/verify-email" element={<EmailVerification />} />
                    {/* Add other routes here */}
                </Routes>
            </Router>
        </ConfigProvider>
    );
};

export default App;