type Listener = () => void;

class MfaService {
  private listeners: Listener[] = [];
  private pending: { resolve: () => void; reject: (e?: any) => void } | null = null;

  onPrompt(listener: Listener): () => void {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  async promptMfa(): Promise<void> {
    if (this.pending) {
      // Already prompting; return the same promise hook
      return new Promise<void>((resolve, reject) => {
        const current = this.pending;
        if (!current) {
          // Race condition safeguard
          resolve();
          return;
        }
        const origResolve = current.resolve;
        const origReject = current.reject;
        current.resolve = () => { try { origResolve(); } finally { resolve(); } };
        current.reject = (e?: any) => { try { origReject(e); } finally { reject(e instanceof Error ? e : new Error(String(e))); } };
      });
    }

    return new Promise<void>((resolve, reject) => {
      this.pending = { resolve, reject };
      // notify all listeners to open prompt
      this.listeners.forEach(l => l());
    });
  }

  complete(success: boolean): void {
    const current = this.pending;
    this.pending = null;
    if (!current) return;
    if (success) current.resolve(); else current.reject(new Error('MFA failed'));
  }
}

const mfaService = new MfaService();
export default mfaService;

