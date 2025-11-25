import { getAccessToken } from '../utils/AccessTokenUtil';

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
    private baseUrl = process.env.REACT_APP_AI_SERVICE_URL || 'http://localhost:8080';

    async streamChat(
        sessionId: string,
        request: ChatRequest,
        onChunk: (chunk: ChatStreamChunk) => void,
        onError: (error: any) => void,
        onComplete: () => void
    ): Promise<void> {
        try {
            const token = getAccessToken();
            const response = await fetch(`${this.baseUrl}/api/v1/chat/${sessionId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: JSON.stringify(request),
            });

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
}

export const aiService = new AiService();
