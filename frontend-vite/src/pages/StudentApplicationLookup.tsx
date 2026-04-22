import React, { useState } from 'react';
import { message } from 'antd';
import authService from '../services/authService';
import type { StudentApplicationObject } from '../types/auth';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

const StudentApplicationLookup: React.FC = () => {
    const [applicationId, setApplicationId] = useState('');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<StudentApplicationObject | null>(null);

    const lookup = async () => {
        if (!applicationId.trim()) {
            message.error('Application ID is required');
            return;
        }
        setLoading(true);
        try {
            const response = await authService.getStudentApplication(applicationId.trim());
            if (response.success) {
                setResult(response.data);
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
            <div className="max-w-3xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Student Application Lookup</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex gap-2">
                            <Input
                                placeholder="Enter your application ID"
                                value={applicationId}
                                onChange={(e) => setApplicationId(e.target.value)}
                            />
                            <Button onClick={lookup} disabled={loading}>
                                {loading ? 'Loading...' : 'Lookup'}
                            </Button>
                        </div>
                        {result && (
                            <div className="rounded-md border border-gray-800 p-3 text-gray-200 text-sm space-y-1">
                                <div>ID: {result.id}</div>
                                <div>Email: {result.email}</div>
                                <div>Status: {result.status}</div>
                                <div>Student: {String(result.student)}</div>
                                <div>Submitted At: {result.applyStudentStatusDate ?? 'N/A'}</div>
                                <div>Document: {result.studentDocumentUrl ?? 'N/A'}</div>
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default StudentApplicationLookup;
