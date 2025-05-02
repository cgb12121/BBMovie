export const getAccessToken = (): string | null => {
     const auth = localStorage.getItem('auth');
     if (auth) {
         return JSON.parse(auth).accessToken;
     }
     return null;
};