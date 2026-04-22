import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Button } from '../components/ui/button';
import { message } from 'antd';
import authService from '../services/authService';
import type { StudentVerificationRequest } from '../types/auth';

const StudentVerificationApply: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [document, setDocument] = useState<File | null>(null);
    const [formData, setFormData] = useState<StudentVerificationRequest>({
        studentId: '',
        fullName: '',
        universityName: '',
        universityDomain: '',
        universityCountry: '',
        graduationYear: new Date().getFullYear(),
        universityEmail: ''
    });

    const onSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        if (!document) {
            message.error('Please attach a verification document');
            return;
        }
        setLoading(true);
        try {
            const response = await authService.applyStudentVerification(formData, document);
            if (response.success) {
                message.success(response.data.message || 'Application submitted successfully');
            } else {
                message.error(response.message || 'Failed to submit application');
            }
        } catch (error: any) {
            message.error(error?.response?.data?.message || 'Failed to submit application');
        } finally {
            setLoading(false);
        }
    };

    const setField = <K extends keyof StudentVerificationRequest>(key: K, value: StudentVerificationRequest[K]) => {
        setFormData((prev) => ({ ...prev, [key]: value }));
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-3xl mx-auto space-y-6">
                <Card className="bg-gray-900 border-gray-800">
                    <CardHeader>
                        <CardTitle className="text-white">Student Verification Application</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <form onSubmit={onSubmit} className="space-y-4">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label className="text-white">Student ID</Label>
                                    <Input value={formData.studentId} onChange={(e) => setField('studentId', e.target.value)} required />
                                </div>
                                <div className="space-y-2">
                                    <Label className="text-white">Full Name</Label>
                                    <Input value={formData.fullName} onChange={(e) => setField('fullName', e.target.value)} required />
                                </div>
                                <div className="space-y-2">
                                    <Label className="text-white">University Name</Label>
                                    <Input value={formData.universityName} onChange={(e) => setField('universityName', e.target.value)} required />
                                </div>
                                <div className="space-y-2">
                                    <Label className="text-white">University Domain</Label>
                                    <Input value={formData.universityDomain} onChange={(e) => setField('universityDomain', e.target.value)} required />
                                </div>
                                <div className="space-y-2">
                                    <Label className="text-white">University Country</Label>
                                    <Input value={formData.universityCountry} onChange={(e) => setField('universityCountry', e.target.value)} required />
                                </div>
                                <div className="space-y-2">
                                    <Label className="text-white">Graduation Year</Label>
                                    <Input
                                        type="number"
                                        value={formData.graduationYear}
                                        onChange={(e) => setField('graduationYear', Number(e.target.value))}
                                        required
                                    />
                                </div>
                                <div className="space-y-2 md:col-span-2">
                                    <Label className="text-white">University Email</Label>
                                    <Input
                                        type="email"
                                        value={formData.universityEmail}
                                        onChange={(e) => setField('universityEmail', e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="space-y-2 md:col-span-2">
                                    <Label className="text-white">Verification Document (PDF/JPG/PNG)</Label>
                                    <Input type="file" onChange={(e) => setDocument(e.target.files?.[0] ?? null)} required />
                                </div>
                            </div>
                            <Button type="submit" disabled={loading} className="w-full bg-red-600 hover:bg-red-700">
                                {loading ? 'Submitting...' : 'Submit Application'}
                            </Button>
                        </form>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default StudentVerificationApply;
