import React, { useEffect, useState } from 'react';
import { message } from 'antd';
import authService from '../services/authService';
import type { StudentApplicationObject } from '../types/auth';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

const StudentApplicationsAdmin: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [applications, setApplications] = useState<StudentApplicationObject[]>([]);
    const [applicationId, setApplicationId] = useState('');

    const loadAll = async () => {
        setLoading(true);
        try {
            const response = await authService.getAllStudentApplications();
            if (response.success) {
                setApplications(response.data ?? []);
            } else {
                message.error(response.message || 'Failed to load applications');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to load applications');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAll().catch(() => undefined);
    }, []);

    const runDecision = async (userId: string, approve: boolean) => {
        try {
            const response = await authService.decideStudentApplication(userId, approve);
            if (response.success) {
                message.success(response.data.message || 'Decision applied');
                await loadAll();
            } else {
                message.error(response.message || 'Decision failed');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Decision failed');
        }
    };

    const fetchOne = async () => {
        if (!applicationId.trim()) return;
        setLoading(true);
        try {
            const response = await authService.getStudentApplication(applicationId.trim());
            if (response.success && response.data) {
                setApplications([response.data]);
            } else {
                message.error(response.message || 'Application not found');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Application not found');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-5xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Student Applications (Admin)</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex flex-col md:flex-row gap-3">
                            <Input
                                placeholder="Search by application ID"
                                value={applicationId}
                                onChange={(e) => setApplicationId(e.target.value)}
                            />
                            <Button onClick={fetchOne} disabled={loading}>Find One</Button>
                            <Button onClick={loadAll} disabled={loading} variant="outline">Load All</Button>
                        </div>
                        <div className="space-y-3">
                            {applications.map((app) => (
                                <div key={app.id} className="border border-gray-800 rounded-md p-3 text-gray-200">
                                    <div className="text-sm">ID: {app.id}</div>
                                    <div className="text-sm">Email: {app.email}</div>
                                    <div className="text-sm">Status: {app.status}</div>
                                    <div className="text-sm">Student: {String(app.student)}</div>
                                    <div className="mt-3 flex gap-2">
                                        <Button size="sm" onClick={() => runDecision(app.id, true)}>Approve</Button>
                                        <Button size="sm" variant="destructive" onClick={() => runDecision(app.id, false)}>Reject</Button>
                                    </div>
                                </div>
                            ))}
                            {applications.length === 0 && (
                                <div className="text-gray-400 text-sm">No applications found.</div>
                            )}
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default StudentApplicationsAdmin;
