import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, Home } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';

const Forbidden: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-black flex items-center justify-center px-4">
            <Card className="bg-gray-900 border-red-900/50 max-w-md w-full">
                <CardHeader className="text-center space-y-4">
                    <div className="mx-auto bg-red-900/20 border-2 border-red-800 rounded-full p-6 w-fit">
                        <ShieldAlert className="h-16 w-16 text-red-500" />
                    </div>
                    <div className="space-y-2">
                        <CardTitle className="text-white text-3xl">Access Denied</CardTitle>
                        <CardDescription className="text-lg">
                            Error 403 - Forbidden
                        </CardDescription>
                    </div>
                </CardHeader>
                <CardContent className="space-y-6">
                    <p className="text-gray-300 text-center">
                        You don't have permission to access this content. This could be because:
                    </p>

                    <ul className="space-y-2 text-gray-400 text-sm">
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-1">•</span>
                            <span>Your subscription doesn't include this content</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-1">•</span>
                            <span>This content is not available in your region</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-1">•</span>
                            <span>You need to sign in to access this page</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-red-600 mt-1">•</span>
                            <span>Your account may have been suspended</span>
                        </li>
                    </ul>

                    <div className="flex flex-col gap-3 pt-4">
                        <Button
                            className="w-full bg-red-600 hover:bg-red-700 text-white gap-2"
                            size="lg"
                            onClick={() => navigate('/subscriptions')}
                        >
                            View Subscription Plans
                        </Button>
                        <Button
                            variant="outline"
                            className="w-full border-gray-700 text-white hover:bg-gray-800 gap-2"
                            onClick={() => navigate('/')}
                        >
                            <Home className="h-4 w-4" />
                            Go to Home
                        </Button>
                    </div>

                    <div className="text-center pt-4">
                        <p className="text-sm text-gray-400">
                            Need help?{' '}
                            <button className="text-red-600 hover:text-red-500">
                                Contact Support
                            </button>
                        </p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
};

export default Forbidden; 