import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import personalizationService, { type RecommendationItem } from '../services/personalizationService';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

const PersonalizationRecommendations: React.FC = () => {
    const navigate = useNavigate();
    const prefilledUserId = useMemo(() => {
        try {
            const user = JSON.parse(localStorage.getItem('user') ?? '{}');
            return (user?.id ?? '').toString();
        } catch {
            return '';
        }
    }, []);

    const [userId, setUserId] = useState(prefilledUserId);
    const [limit, setLimit] = useState(20);
    const [loading, setLoading] = useState(false);
    const [items, setItems] = useState<RecommendationItem[]>([]);

    const load = async () => {
        if (!userId.trim()) {
            message.error('User ID is required');
            return;
        }
        setLoading(true);
        try {
            const data = await personalizationService.getUserRecommendations(userId.trim(), Math.max(1, Math.min(100, limit)));
            setItems(data);
            message.success(`Loaded ${data.length} recommendation(s)`);
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to load recommendations');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-6xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Personalized Recommendations</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                            <Input
                                placeholder="User ID (UUID)"
                                value={userId}
                                onChange={(e) => setUserId(e.target.value)}
                            />
                            <Input
                                type="number"
                                min={1}
                                max={100}
                                value={limit}
                                onChange={(e) => setLimit(Number(e.target.value || 20))}
                            />
                            <Button onClick={load} disabled={loading}>
                                {loading ? 'Loading...' : 'Load Recommendations'}
                            </Button>
                        </div>
                    </CardContent>
                </Card>

                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Items</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {items.length === 0 ? (
                            <div className="text-gray-400 text-sm">No recommendations loaded.</div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead>
                                        <tr className="border-b border-gray-700 text-gray-300">
                                            <th className="py-2 px-2">Movie ID</th>
                                            <th className="py-2 px-2">Score</th>
                                            <th className="py-2 px-2">Reason</th>
                                            <th className="py-2 px-2"></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {items.map((item) => (
                                            <tr key={`${item.movieId}-${item.score}`} className="border-b border-gray-800 text-gray-200">
                                                <td className="py-2 px-2 font-mono">{item.movieId}</td>
                                                <td className="py-2 px-2">{Number(item.score || 0).toFixed(4)}</td>
                                                <td className="py-2 px-2">{item.reason || ''}</td>
                                                <td className="py-2 px-2">
                                                    <Button size="sm" variant="outline" onClick={() => navigate(`/movies/${item.movieId}`)}>
                                                        Open Movie
                                                    </Button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default PersonalizationRecommendations;
