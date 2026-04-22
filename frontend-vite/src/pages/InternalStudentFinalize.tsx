import React, { useState } from 'react';
import { message } from 'antd';
import authService from '../services/authService';
import type { VerificationOutcome } from '../types/auth';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';

const outcomes: VerificationOutcome[] = [
    'AUTO_APPROVE',
    'AUTO_REJECT',
    'NEEDS_REVIEW',
    'VERIFIED',
    'REJECTED'
];

const InternalStudentFinalize: React.FC = () => {
    const [applicationId, setApplicationId] = useState('');
    const [status, setStatus] = useState<VerificationOutcome>('NEEDS_REVIEW');
    const [detailMessage, setDetailMessage] = useState('');
    const [loading, setLoading] = useState(false);

    const finalize = async () => {
        if (!applicationId.trim()) {
            message.error('Application ID is required');
            return;
        }
        setLoading(true);
        try {
            const response = await authService.finalizeStudentApplication(applicationId.trim(), status, detailMessage || undefined);
            if (response.success) {
                message.success('Finalize call succeeded');
            } else {
                message.error(response.message || 'Finalize failed');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Finalize failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-3xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Internal Finalize Student Verification</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <p className="text-yellow-400 text-sm">
                            Internal endpoint tool. Use only for backend workflow/manual recovery operations.
                        </p>
                        <div className="space-y-2">
                            <Label className="text-white">Application ID</Label>
                            <Input value={applicationId} onChange={(e) => setApplicationId(e.target.value)} />
                        </div>
                        <div className="space-y-2">
                            <Label className="text-white">Outcome</Label>
                            <select
                                className="w-full bg-gray-800 text-white border border-gray-700 rounded-md p-2"
                                value={status}
                                onChange={(e) => setStatus(e.target.value as VerificationOutcome)}
                            >
                                {outcomes.map((outcome) => (
                                    <option key={outcome} value={outcome}>
                                        {outcome}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="space-y-2">
                            <Label className="text-white">Message</Label>
                            <Input value={detailMessage} onChange={(e) => setDetailMessage(e.target.value)} />
                        </div>
                        <Button onClick={finalize} disabled={loading} className="w-full">
                            {loading ? 'Finalizing...' : 'Finalize Verification'}
                        </Button>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default InternalStudentFinalize;
