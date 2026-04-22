import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import watchHistoryService, { type WatchHistoryItem } from '../services/watchHistoryService';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';

const formatDuration = (seconds: number): string => {
    if (!Number.isFinite(seconds) || seconds <= 0) return '-';
    const minutes = Math.floor(seconds / 60);
    const sec = Math.floor(seconds % 60);
    return `${minutes}:${String(sec).padStart(2, '0')} (${seconds.toFixed(1)}s)`;
};

const formatUpdatedAt = (epochSeconds: number): string => {
    if (!Number.isFinite(epochSeconds)) return '-';
    return new Date(epochSeconds * 1000).toLocaleString();
};

const WatchHistoryResume: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [items, setItems] = useState<WatchHistoryItem[]>([]);

    const load = async () => {
        setLoading(true);
        try {
            const data = await watchHistoryService.listAllItems(50);
            setItems(data);
            message.success(`Loaded ${data.length} item(s)`);
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to load watch history');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-6xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Watch History (Resume)</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        <p className="text-gray-400 text-sm">
                            Resume items from watch-history service. This is different from watchlist collections.
                        </p>
                        <Button onClick={load} disabled={loading}>
                            {loading ? 'Loading...' : 'Load My Items'}
                        </Button>
                    </CardContent>
                </Card>

                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Items</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {items.length === 0 ? (
                            <div className="text-gray-400 text-sm">No resume rows loaded.</div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead>
                                        <tr className="border-b border-gray-700 text-gray-300">
                                            <th className="py-2 px-2">Movie ID</th>
                                            <th className="py-2 px-2">Position</th>
                                            <th className="py-2 px-2">Duration</th>
                                            <th className="py-2 px-2">Done</th>
                                            <th className="py-2 px-2">Updated</th>
                                            <th className="py-2 px-2"></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {items.map((item) => (
                                            <tr key={`${item.movieId}-${item.updatedAtEpochSec}`} className="border-b border-gray-800 text-gray-200">
                                                <td className="py-2 px-2 font-mono">{item.movieId}</td>
                                                <td className="py-2 px-2">{formatDuration(Number(item.positionSec))}</td>
                                                <td className="py-2 px-2">{formatDuration(Number(item.durationSec))}</td>
                                                <td className="py-2 px-2">{item.completed ? 'yes' : 'no'}</td>
                                                <td className="py-2 px-2">{formatUpdatedAt(Number(item.updatedAtEpochSec))}</td>
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

export default WatchHistoryResume;
