import React, { useEffect, useState } from 'react';
import api from '../../services/api';

interface CsrfProviderProps {
     children: React.ReactNode;
}

const CsrfProvider: React.FC<CsrfProviderProps> = ({ children }) => {
     const [isLoading, setIsLoading] = useState(true);

     useEffect(() => {
          const initializeCsrf = async () => {
               try {
                    await api.get('api/auth/csrf');
               } catch (error) {
                    console.error('Failed to initialize CSRF token:', error);
               } finally {
                    setIsLoading(false);
               }
          };

          initializeCsrf();
     }, []);

     if (isLoading) {
          return null;
     }

     return <>{children}</>;
};

export default CsrfProvider; 