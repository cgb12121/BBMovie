import api from './api';

export interface ChatStreamChunk {
    type: 'assistant' | 'user' | 'tool' | 'system' | 'rag_result';
    content: string;
    thinking?: string;
    metadata?: Record<string, any>;
    ragResults?: any[];
}

export interface ChatRequest {
    message: string;
    assistantType: string;
    aiMode: string;
}

class AiService {
    async streamChat(
        sessionId: string,
        request: ChatRequest,
        onChunk: (chunk: ChatStreamChunk) => void,
        onError: (error: any) => void,
        onComplete: () => void
    ): Promise<void> {
        try {
            // Build plain string headers for fetch from axios defaults
            const commonHeaders = api.defaults.headers.common ?? {};
            const mergedHeaders: Record<string, string> = {
                'Content-Type': 'application/json',
                ...Object.fromEntries(
                    Object.entries(commonHeaders).map(([key, value]) => [
                        key,
                        String(value),
                    ])
                ),
            };

            // We still need to use fetch for SSE (Server Sent Events) as axios doesn't support it well
            const response = await fetch(
                `${api.defaults.baseURL || import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8080'}/api/v1/chat/${sessionId}`,
                {
                    method: 'POST',
                    headers: mergedHeaders,
                    body: JSON.stringify(request),
                }
            );

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            if (!response.body) {
                throw new Error('Response body is null');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const data = line.slice(5).trim();
                        if (data) {
                            try {
                                const chunk: ChatStreamChunk = JSON.parse(data);
                                onChunk(chunk);
                            } catch (e) {
                                console.error('Error parsing chunk:', e);
                            }
                        }
                    }
                }
            }

            onComplete();

        } catch (error) {
            onError(error);
        }
    }

    // Upload files to file-service first - using the main api instance
    async uploadFiles(files: File[]): Promise<any[]> {
        try {
            const formData = new FormData();

            files.forEach((file) => {
                formData.append('files', file);
            });

            // Use the main api instance for file upload, which is now configured for AI service endpoints
            const response = await api.post('/internal/files/upload', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            return response.data;
        } catch (error) {
            console.error('File upload error:', error);
            throw error;
        }
    }

    async streamChatWithAttachments(
        sessionId: string,
        message: string,
        files: File[],
        assistantType: string = 'user',
        aiMode: string = 'normal',
        onChunk: (chunk: ChatStreamChunk) => void,
        onError: (error: any) => void,
        onComplete: () => void
    ): Promise<void> {
        try {
            // First, upload files to file-service
            let fileMetadata: any[] = [];
            if (files.length > 0) {
                fileMetadata = await this.uploadFiles(files);
            }

            // Then send the chat message with file references
            const chatData = {
                message: message,
                assistantType: assistantType,
                aiMode: aiMode,
                attachments: fileMetadata.map(file => ({
                    id: file.id,
                    filename: file.filename,
                    originalName: file.originalName,
                    fileType: file.fileType,
                    fileSize: file.fileSize
                }))
            };

            // Build plain string headers for fetch from axios defaults
            const commonHeaders = api.defaults.headers.common ?? {};
            const mergedHeaders: Record<string, string> = {
                'Content-Type': 'application/json',
                ...Object.fromEntries(
                    Object.entries(commonHeaders).map(([key, value]) => [
                        key,
                        String(value),
                    ])
                ),
            };

            // Using fetch for SSE response handling for the chat endpoint
            const response = await fetch(
                `${api.defaults.baseURL || import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8080'}/api/v1/chat/${sessionId}`,
                {
                    method: 'POST',
                    headers: mergedHeaders,
                    body: JSON.stringify(chatData),
                }
            );

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            if (!response.body) {
                throw new Error('Response body is null');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const data = line.slice(5).trim();
                        if (data) {
                            try {
                                const chunk: ChatStreamChunk = JSON.parse(data);
                                onChunk(chunk);
                            } catch (e) {
                                console.error('Error parsing chunk:', e);
                            }
                        }
                    }
                }
            }

            onComplete();

        } catch (error) {
            onError(error);
        }
    }

    // Health check for AI service - using the main api instance
    async healthCheck(): Promise<boolean> {
        try {
            const response = await api.get('/health');
            return response.status === 200;
        } catch (error) {
            console.error('Health check failed:', error);
            return false;
        }
    }

    // Health check for Rust AI service - using the main api instance
    async rustServiceHealthCheck(): Promise<boolean> {
        try {
            const response = await api.get('/api/health');
            return response.status === 200;
        } catch (error) {
            console.error('Rust AI service health check failed:', error);
            return false;
        }
    }
}

export const aiService = new AiService();
