import api from './api';

export interface SubscriptionPlan {
    id: string;
    name: string;
    description?: string;
    planType: string;
    monthlyPrice: number;
    annualOriginalPrice?: number;
    annualFinalPrice?: number;
    annualDiscountPercent?: number;
    annualDiscountAmount?: number;
    currency: string;
    features: string[];
}

export interface PricingQuote {
    amount: number;
    cycle: string;
}

export interface SubscriptionPaymentRequest {
    planId: string;
    provider: string;
    billingCycle: 'MONTHLY' | 'ANNUAL';
    voucherCode?: string;
}

export interface PaymentCreationResponse {
    provider: string;
    providerTransactionId: string;
    serverTransactionId: string;
    serverStatus: string;
    providerPaymentLink: string;
}

export interface UserSubscription {
    id: string;
    planId: string;
    active: boolean;
    autoRenew: boolean;
    startDate?: string;
    endDate?: string;
    lastPaymentDate?: string;
    nextPaymentDate?: string;
    provider: string;
    paymentMethod?: string;
}

export interface ToggleAutoRenewRequest {
    autoRenew: boolean;
}

export interface CancelSubscriptionRequest {
    reason: string;
}

interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

type RawPlan = {
    id: string;
    name: string;
    description?: string;
    planType: string;
    monthlyPrice: number;
    annualOriginalPrice?: number;
    annualDiscountPercent?: number;
    annualDiscountAmount?: number;
    annualFinalPrice?: number;
    features?: string;
    currency: string | { currencyCode?: string };
};

type RawUserSubscription = {
    id: string;
    planId: string;
    active: boolean;
    autoRenew: boolean;
    startDate?: string;
    endDate?: string;
    lastPaymentDate?: string;
    nextPaymentDate?: string;
    paymentProvider?: string;
    paymentMethod?: string;
};

type RawPaymentCreation = {
    provider: string;
    providerTransactionId: string;
    serverTransactionId: string;
    serverStatus: string;
    providerPaymentLink: string;
};

const PROVIDERS = ['PAYPAL', 'STRIPE', 'VNPAY', 'ZALOPAY', 'MOMO'] as const;
export const PAYMENT_PROVIDERS = [...PROVIDERS];

const parseCurrency = (currency: RawPlan['currency']): string => {
    if (!currency) return 'USD';
    if (typeof currency === 'string') return currency.toUpperCase();
    return currency.currencyCode?.toUpperCase() ?? 'USD';
};

const parseFeatures = (features?: string): string[] => {
    if (!features) return [];
    return features
        .split(/\r?\n|;/)
        .map(feature => feature.trim())
        .filter(Boolean);
};

const mapPlan = (raw: RawPlan): SubscriptionPlan => ({
    id: raw.id,
    name: raw.name,
    description: raw.description,
    planType: raw.planType,
    monthlyPrice: Number(raw.monthlyPrice ?? 0),
    annualOriginalPrice: raw.annualOriginalPrice ? Number(raw.annualOriginalPrice) : undefined,
    annualFinalPrice: raw.annualFinalPrice ? Number(raw.annualFinalPrice) : undefined,
    annualDiscountPercent: raw.annualDiscountPercent ? Number(raw.annualDiscountPercent) : undefined,
    annualDiscountAmount: raw.annualDiscountAmount ? Number(raw.annualDiscountAmount) : undefined,
    currency: parseCurrency(raw.currency),
    features: parseFeatures(raw.features)
});

const mapSubscription = (raw: RawUserSubscription): UserSubscription => ({
    id: raw.id,
    planId: raw.planId,
    active: raw.active,
    autoRenew: raw.autoRenew,
    startDate: raw.startDate,
    endDate: raw.endDate,
    lastPaymentDate: raw.lastPaymentDate,
    nextPaymentDate: raw.nextPaymentDate,
    provider: raw.paymentProvider ?? 'UNKNOWN',
    paymentMethod: raw.paymentMethod
});

const mapPaymentCreation = (raw: RawPaymentCreation): PaymentCreationResponse => ({
    provider: raw.provider,
    providerTransactionId: raw.providerTransactionId,
    serverTransactionId: raw.serverTransactionId,
    serverStatus: raw.serverStatus,
    providerPaymentLink: raw.providerPaymentLink,
});

const paymentService = {
    async listPlans(): Promise<SubscriptionPlan[]> {
        const response = await api.get<RawPlan[]>('/api/v1/subscriptions/plans');
        return (response.data ?? []).map(mapPlan);
    },

    async quotePrice(plan: string, cycle: 'monthly' | 'annual' = 'monthly'): Promise<PricingQuote> {
        const response = await api.get<number | string>('/api/v1/subscriptions/quote', {
            params: { plan, cycle }
        });
        const amount = typeof response.data === 'string' ? Number(response.data) : Number(response.data ?? 0);
        return { amount, cycle };
    },

    async initiatePayment(request: SubscriptionPaymentRequest): Promise<PaymentCreationResponse> {
        const payload = {
            provider: request.provider,
            subscriptionPlanId: request.planId,
            billingCycle: request.billingCycle,
            voucherCode: request.voucherCode || undefined
        };
        const response = await api.post<ApiResponse<RawPaymentCreation>>('/api/v1/subscriptions/initiate', payload);
        return mapPaymentCreation(response.data.data);
    },

    async mySubscriptions(): Promise<UserSubscription[]> {
        const response = await api.get<ApiResponse<RawUserSubscription[]>>('/api/v1/subscriptions/mine');
        return (response.data.data ?? []).map(mapSubscription);
    },

    async toggleAutoRenew(id: string, req: ToggleAutoRenewRequest): Promise<UserSubscription> {
        const response = await api.post<ApiResponse<RawUserSubscription>>(`/api/v1/subscriptions/${id}/auto-renew`, req);
        return mapSubscription(response.data.data);
    },

    async cancelSubscription(id: string, req: CancelSubscriptionRequest): Promise<UserSubscription> {
        const response = await api.post<ApiResponse<RawUserSubscription>>(`/api/v1/subscriptions/${id}/cancel`, req);
        return mapSubscription(response.data.data);
    }
};

export default paymentService;
