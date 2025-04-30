import React, { useEffect, useState } from 'react';
import api from '../../services/api';

interface CsrfProviderProps {
     children: React.ReactNode;
}

const getCookie = (name: string): string | null => {
     const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
     return match ? decodeURIComponent(match[2]) : null;
};

const CsrfProvider: React.FC<CsrfProviderProps> = ({ children }) => {
     const [isLoading, setIsLoading] = useState(true);

     useEffect(() => {
          const initializeCsrf = async () => {
               const existingToken = getCookie('XSRF-TOKEN');

               if (!existingToken) {
                    try {
                         await api.get('/api/auth/csrf');
                    } catch (error) {
                         console.error('Failed to initialize CSRF token:', error);
                    }
               }

          setIsLoading(false);
          };

          initializeCsrf();
     }, []);

     if (isLoading) return null;

     return <>{children}</>;
};

export default CsrfProvider;
