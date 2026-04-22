import React, { useState } from 'react';
import { message } from 'antd';
import authService from '../services/authService';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';

const JwkAdmin: React.FC = () => {
    const [publicKeys, setPublicKeys] = useState<Record<string, unknown>[]>([]);
    const [adminAllKeys, setAdminAllKeys] = useState<Record<string, unknown>[]>([]);
    const [adminActiveKeys, setAdminActiveKeys] = useState<Record<string, unknown>[]>([]);
    const [loading, setLoading] = useState(false);

    const loadPublic = async () => {
        setLoading(true);
        try {
            const keySet = await authService.getPublicJwks();
            setPublicKeys(keySet.keys ?? []);
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to load public JWKs');
        } finally {
            setLoading(false);
        }
    };

    const loadAdminAll = async () => {
        setLoading(true);
        try {
            const response = await authService.getAdminJwksAll();
            if (response.success) {
                setAdminAllKeys(response.data?.keys ?? []);
            } else {
                message.error(response.message || 'Failed to load admin all JWKs');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to load admin all JWKs');
        } finally {
            setLoading(false);
        }
    };

    const loadAdminActive = async () => {
        setLoading(true);
        try {
            const response = await authService.getAdminJwksActive();
            if (response.success) {
                setAdminActiveKeys(response.data?.keys ?? []);
            } else {
                message.error(response.message || 'Failed to load admin active JWKs');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to load admin active JWKs');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-5xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">JWK Management</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        <div className="flex flex-wrap gap-2">
                            <Button onClick={loadPublic} disabled={loading}>Load Public JWKs</Button>
                            <Button onClick={loadAdminAll} disabled={loading} variant="outline">Load Admin All JWKs</Button>
                            <Button onClick={loadAdminActive} disabled={loading} variant="outline">Load Admin Active JWKs</Button>
                        </div>
                        <div className="space-y-4 text-gray-200">
                            <div>
                                <h3 className="text-white mb-1">Public</h3>
                                <pre className="bg-gray-950 p-3 rounded-md overflow-auto text-xs">{JSON.stringify(publicKeys, null, 2)}</pre>
                            </div>
                            <div>
                                <h3 className="text-white mb-1">Admin All</h3>
                                <pre className="bg-gray-950 p-3 rounded-md overflow-auto text-xs">{JSON.stringify(adminAllKeys, null, 2)}</pre>
                            </div>
                            <div>
                                <h3 className="text-white mb-1">Admin Active</h3>
                                <pre className="bg-gray-950 p-3 rounded-md overflow-auto text-xs">{JSON.stringify(adminActiveKeys, null, 2)}</pre>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default JwkAdmin;
