import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Home, Search } from 'lucide-react';
import { Button } from '../components/ui/button';

const NotFound: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-black flex items-center justify-center px-4">
            <div className="text-center space-y-8 max-w-md">
                {/* 404 Text */}
                <div className="space-y-4">
                    <h1 className="text-white text-9xl font-bold">404</h1>
                    <div className="h-1 w-32 bg-red-600 mx-auto" />
                </div>

                {/* Message */}
                <div className="space-y-4">
                    <h2 className="text-white text-3xl font-bold">Page Not Found</h2>
                    <p className="text-gray-400 text-lg">
                        Sorry, we couldn't find the page you're looking for. It might have been removed, 
                        renamed, or didn't exist in the first place.
                    </p>
                </div>

                {/* Actions */}
                <div className="flex flex-col sm:flex-row gap-4 justify-center">
                    <Button
                        className="bg-red-600 hover:bg-red-700 text-white gap-2"
                        size="lg"
                        onClick={() => navigate('/')}
                    >
                        <Home className="h-5 w-5" />
                        Go to Home
                    </Button>
                    <Button
                        variant="outline"
                        className="border-gray-700 text-white hover:bg-gray-800 gap-2"
                        size="lg"
                        onClick={() => navigate('/search')}
                    >
                        <Search className="h-5 w-5" />
                        Search Movies
                    </Button>
                </div>

                {/* Suggestions */}
                <div className="pt-8 space-y-3">
                    <p className="text-gray-500 text-sm">Popular pages:</p>
                    <div className="flex flex-wrap gap-2 justify-center">
                        <button
                            onClick={() => navigate('/')}
                            className="text-sm text-gray-400 hover:text-red-500 transition-colors"
                        >
                            Home
                        </button>
                        <span className="text-gray-700">•</span>
                        <button
                            onClick={() => navigate('/movies')}
                            className="text-sm text-gray-400 hover:text-red-500 transition-colors"
                        >
                            Browse Movies
                        </button>
                        <span className="text-gray-700">•</span>
                        <button
                            onClick={() => navigate('/categories')}
                            className="text-sm text-gray-400 hover:text-red-500 transition-colors"
                        >
                            Categories
                        </button>
                        <span className="text-gray-700">•</span>
                        <button
                            onClick={() => navigate('/watchlist')}
                            className="text-sm text-gray-400 hover:text-red-500 transition-colors"
                        >
                            My List
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default NotFound; 