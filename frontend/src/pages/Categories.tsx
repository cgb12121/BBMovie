import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronRight, Loader2 } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { ImageWithFallback } from '../components/ImageWithFallback';
import api from '../services/api';

interface Category {
    id: number;
    name: string;
    image: string;
    description?: string;
    movieCount?: number;
}

const Categories: React.FC = () => {
    const [loading, setLoading] = useState(true);
    const [categories, setCategories] = useState<Category[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        try {
            setLoading(true);
            const response = await api.get('/categories');
            setCategories(response.data);
        } catch (error) {
            console.error('Error fetching categories:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCategoryClick = (categoryId: number) => {
        navigate(`/categories/${categoryId}`);
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-7xl mx-auto space-y-8">
                {/* Header */}
                <div className="space-y-4">
                    <h1 className="text-white text-3xl md:text-4xl font-bold">Browse by Category</h1>
                    <p className="text-gray-400">Discover movies by your favorite genres</p>
                </div>

                {/* Loading State */}
                {loading && (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="h-12 w-12 text-red-600 animate-spin" />
                    </div>
                )}

                {/* Categories Grid */}
                {!loading && (
                    <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        {categories.map((category) => (
                            <Card
                                key={category.id}
                                onClick={() => handleCategoryClick(category.id)}
                                className="group bg-gray-900 border-gray-800 hover:border-red-600 transition-all cursor-pointer overflow-hidden"
                            >
                                <CardContent className="p-0">
                                    <div className="relative aspect-video overflow-hidden">
                                        <ImageWithFallback
                                            src={category.image}
                                            alt={category.name}
                                            className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                                        />
                                        <div className="absolute inset-0 bg-gradient-to-t from-black via-black/50 to-transparent" />
                                        <div className="absolute bottom-0 left-0 right-0 p-6 space-y-2">
                                            <div className="flex items-center justify-between">
                                                <h3 className="text-white text-2xl font-bold">{category.name}</h3>
                                                <ChevronRight className="h-6 w-6 text-white group-hover:translate-x-1 transition-transform" />
                                            </div>
                                            {category.description && (
                                                <p className="text-gray-300 text-sm">{category.description}</p>
                                            )}
                                            {category.movieCount !== undefined && (
                                                <Badge className="bg-red-600 text-white">
                                                    {category.movieCount} {category.movieCount === 1 ? 'movie' : 'movies'}
                                                </Badge>
                                            )}
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}

                {/* Empty State */}
                {!loading && categories.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-20 space-y-4">
                        <p className="text-gray-400 text-lg">No categories available</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Categories; 
