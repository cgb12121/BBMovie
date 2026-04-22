import api from './api';

export interface WatchHistoryItem {
    movieId: string;
    positionSec: number;
    durationSec: number;
    completed: boolean;
    updatedAtEpochSec: number;
}

interface WatchHistoryPage {
    items: WatchHistoryItem[];
    nextCursor?: string | null;
}

class WatchHistoryService {
    async listAllItems(limit = 50): Promise<WatchHistoryItem[]> {
        const merged: WatchHistoryItem[] = [];
        let cursor = '0';
        let guard = 0;

        while (guard < 200) {
            const response = await api.get<WatchHistoryPage>('/api/watch-history/v1/items', {
                params: { cursor, limit }
            });
            const pageItems = Array.isArray(response.data?.items) ? response.data.items : [];
            merged.push(...pageItems);

            const nextCursor = response.data?.nextCursor;
            if (!nextCursor || nextCursor === '0') {
                break;
            }

            cursor = String(nextCursor);
            guard += 1;
        }

        return merged.sort((a, b) => Number(b.updatedAtEpochSec) - Number(a.updatedAtEpochSec));
    }
}

const watchHistoryService = new WatchHistoryService();
export default watchHistoryService;
