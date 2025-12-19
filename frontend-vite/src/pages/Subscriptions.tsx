import React, { useEffect, useMemo, useState } from 'react';
import { Check } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { message, Switch, Modal, Form, Input } from 'antd';
import paymentService, {
    type CancelSubscriptionRequest,
    PAYMENT_PROVIDERS,
    type SubscriptionPlan,
    type SubscriptionPaymentRequest,
    type UserSubscription
} from '../services/paymentService';

const billingCycleOptions = [
    { label: 'Monthly', value: 'MONTHLY' },
    { label: 'Annual', value: 'ANNUAL' }
] as const;

type BillingCycleValue = typeof billingCycleOptions[number]['value'];

const Subscriptions: React.FC = () => {
    const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
    const [plansLoading, setPlansLoading] = useState(false);
    const [selectedPlanId, setSelectedPlanId] = useState<string | undefined>();
    const [billingCycle, setBillingCycle] = useState<BillingCycleValue>('MONTHLY');
    const [provider, setProvider] = useState<string>(PAYMENT_PROVIDERS[0]);
    const [voucherCode, setVoucherCode] = useState('');
    const [quote, setQuote] = useState<{ amount: number; cycle: string } | null>(null);
    const [quoteLoading, setQuoteLoading] = useState(false);
    const [creatingPayment, setCreatingPayment] = useState(false);

    const [subscriptions, setSubscriptions] = useState<UserSubscription[]>([]);
    const [subscriptionsLoading, setSubscriptionsLoading] = useState(false);
    const [cancelModal, setCancelModal] = useState<{ visible: boolean; subscriptionId: string | null }>({
        visible: false,
        subscriptionId: null
    });
    const [cancelReason, setCancelReason] = useState('');

    const selectedPlan = useMemo(
        () => plans.find(plan => plan.id === selectedPlanId),
        [plans, selectedPlanId]
    );

    const loadPlans = async () => {
        try {
            setPlansLoading(true);
            const data = await paymentService.listPlans();
            setPlans(data);
            if (!selectedPlanId && data.length > 0) {
                setSelectedPlanId(data[0].id);
            }
        } catch (error) {
            console.error(error);
            message.error('Failed to load subscription plans');
        } finally {
            setPlansLoading(false);
        }
    };

    const loadSubscriptions = async () => {
        try {
            setSubscriptionsLoading(true);
            const data = await paymentService.mySubscriptions();
            setSubscriptions(data);
        } catch (error) {
            console.error(error);
            message.error('Failed to load your subscriptions');
        } finally {
            setSubscriptionsLoading(false);
        }
    };

    const refreshAll = async () => {
        await Promise.all([loadPlans(), loadSubscriptions()]);
    };

    const updateQuote = async (planId: string, cycle: BillingCycleValue) => {
        try {
            setQuoteLoading(true);
            const plan = plans.find(p => p.id === planId);
            const response = await paymentService.quotePrice(plan?.name ?? '', cycle === 'MONTHLY' ? 'monthly' : 'annual');
            setQuote(response);
        } catch (error) {
            console.error(error);
            message.error('Failed to fetch price quote');
        } finally {
            setQuoteLoading(false);
        }
    };

    const handleCreatePayment = async () => {
        if (!selectedPlan) {
            message.warning('Select a plan first');
            return;
        }

        const payload: SubscriptionPaymentRequest = {
            planId: selectedPlan.id,
            provider,
            billingCycle,
            voucherCode: voucherCode.trim() || undefined
        };

        try {
            setCreatingPayment(true);
            const response = await paymentService.initiatePayment(payload);
            message.success('Redirecting to payment provider...');
            if (response.providerPaymentLink) {
                window.open(response.providerPaymentLink, '_blank', 'noopener');
            }
        } catch (error) {
            console.error(error);
            message.error('Failed to initiate payment');
        } finally {
            setCreatingPayment(false);
        }
    };

    const handleToggleAutoRenew = async (subscription: UserSubscription, autoRenew: boolean) => {
        try {
            const updated = await paymentService.toggleAutoRenew(subscription.id, { autoRenew });
            setSubscriptions(prev =>
                prev.map(item => (item.id === subscription.id ? updated : item))
            );
            message.success(`Auto-renew ${autoRenew ? 'enabled' : 'disabled'}`);
        } catch (error) {
            console.error(error);
            message.error('Failed to update auto-renew setting');
        }
    };

    const handleCancelSubscription = async () => {
        if (!cancelModal.subscriptionId) return;

        const request: CancelSubscriptionRequest = {
            reason: cancelReason.trim() || 'User requested cancellation'
        };

        try {
            const updated = await paymentService.cancelSubscription(cancelModal.subscriptionId, request);
            setSubscriptions(prev => prev.map(item => (item.id === updated.id ? updated : item)));
            message.success('Subscription cancellation requested');
        } catch (error) {
            console.error(error);
            message.error('Failed to cancel subscription');
        } finally {
            setCancelModal({ visible: false, subscriptionId: null });
            setCancelReason('');
        }
    };

    useEffect(() => {
        refreshAll();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (selectedPlanId) {
            updateQuote(selectedPlanId, billingCycle);
        } else {
            setQuote(null);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedPlanId, billingCycle]);

    const renderPlanCard = (plan: SubscriptionPlan) => {
        const isSelected = plan.id === selectedPlanId;
        const isRecommended = plan.planType === 'PREMIUM' || plan.name.toLowerCase().includes('premium');
        
        return (
            <Card
                key={plan.id}
                className={`relative bg-gray-900 border-2 transition-all hover:scale-105 cursor-pointer ${
                    isSelected || isRecommended
                        ? 'border-red-600 shadow-lg shadow-red-600/20'
                        : 'border-gray-800'
                }`}
                onClick={() => setSelectedPlanId(plan.id)}
            >
                {isRecommended && (
                    <div className="absolute -top-4 left-1/2 -translate-x-1/2">
                        <Badge className="bg-red-600 text-white px-4 py-1">
                            Most Popular
                        </Badge>
                    </div>
                )}
                <CardHeader className="text-center space-y-2">
                    <CardTitle className="text-white text-2xl">{plan.name}</CardTitle>
                    <CardDescription>{plan.planType}</CardDescription>
                    <div className="pt-4">
                        <span className="text-white text-4xl">${plan.monthlyPrice}</span>
                        <span className="text-gray-400">/month</span>
                    </div>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ul className="space-y-3">
                        {plan.features.map((feature, index) => (
                            <li key={index} className="flex items-start gap-3">
                                <Check className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                                <span className="text-gray-300 text-sm">{feature}</span>
                            </li>
                        ))}
                    </ul>
                </CardContent>
                <CardFooter>
                    <Button
                        className={`w-full ${
                            isSelected || isRecommended
                                ? 'bg-red-600 hover:bg-red-700 text-white'
                                : 'bg-gray-700 hover:bg-gray-600 text-white'
                        }`}
                        size="lg"
                        onClick={() => setSelectedPlanId(plan.id)}
                    >
                        {isSelected ? 'Selected' : 'Get Started'}
                    </Button>
                </CardFooter>
            </Card>
        );
    };

    const renderSubscriptions = () => {
        if (subscriptionsLoading) {
            return <div className="text-center text-gray-400">Loading...</div>;
        }

        if (subscriptions.length === 0) {
            return <div className="text-center text-gray-400 py-4">No active subscriptions</div>;
        }

        return (
            <div className="space-y-4">
                {subscriptions.map(subscription => (
                    <Card key={subscription.id} className="bg-gray-800 border-gray-700">
                        <CardHeader className="flex flex-row items-center justify-between pb-2">
                            <CardTitle className="text-white text-lg">Subscription {subscription.id}</CardTitle>
                            <div className="flex items-center gap-2">
                                <Switch
                                    checked={subscription.autoRenew}
                                    onChange={(checked) => handleToggleAutoRenew(subscription, checked)}
                                />
                                <Button
                                    variant="destructive"
                                    size="sm"
                                    onClick={() => setCancelModal({ visible: true, subscriptionId: subscription.id })}
                                >
                                    Cancel
                                </Button>
                            </div>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            <div className="grid grid-cols-2 gap-4 text-sm">
                                <div>
                                    <span className="text-gray-400">Plan ID:</span>
                                    <span className="text-white ml-2">{subscription.planId}</span>
                                </div>
                                <div>
                                    <span className="text-gray-400">Provider:</span>
                                    <span className="text-white ml-2">{subscription.provider}</span>
                                </div>
                                <div>
                                    <span className="text-gray-400">Payment Method:</span>
                                    <span className="text-white ml-2">{subscription.paymentMethod ?? 'N/A'}</span>
                                </div>
                                <div>
                                    <span className="text-gray-400">Start Date:</span>
                                    <span className="text-white ml-2">{subscription.startDate ?? 'N/A'}</span>
                                </div>
                                <div>
                                    <span className="text-gray-400">End Date:</span>
                                    <span className="text-white ml-2">{subscription.endDate ?? 'N/A'}</span>
                                </div>
                                <div>
                                    <span className="text-gray-400">Next Payment:</span>
                                    <span className="text-white ml-2">{subscription.nextPaymentDate ?? 'N/A'}</span>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                ))}
            </div>
        );
    };

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4">
            <div className="max-w-7xl mx-auto space-y-12">
                {/* Header */}
                <div className="text-center space-y-4">
                    <h1 className="text-white text-4xl md:text-5xl font-bold">Choose Your Plan</h1>
                    <p className="text-gray-400 text-lg max-w-2xl mx-auto">
                        Select the perfect plan for your streaming needs. Upgrade, downgrade, or cancel anytime.
                    </p>
                </div>

                {/* Pricing Cards */}
                {plansLoading ? (
                    <div className="flex justify-center">
                        <div className="text-white">Loading plans...</div>
                    </div>
                ) : plans.length === 0 ? (
                    <div className="text-center text-gray-400">No plans available</div>
                ) : (
                    <div className="grid md:grid-cols-3 gap-6">
                        {plans.map(plan => renderPlanCard(plan))}
                    </div>
                )}

                {/* Checkout Section */}
                {selectedPlan && (
                    <div className="max-w-2xl mx-auto">
                        <Card className="bg-gray-900 border-gray-800">
                            <CardHeader>
                                <CardTitle className="text-white">Complete Your Purchase</CardTitle>
                                <CardDescription>Choose your billing cycle and payment provider</CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                <div>
                                    <label className="text-sm text-gray-400 block mb-2">Billing Cycle</label>
                                    <select
                                        value={billingCycle}
                                        onChange={(e) => setBillingCycle(e.target.value as BillingCycleValue)}
                                        className="w-full bg-gray-800 text-white border border-gray-700 rounded-md px-3 py-2"
                                    >
                                        {billingCycleOptions.map(option => (
                                            <option key={option.value} value={option.value}>{option.label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="text-sm text-gray-400 block mb-2">Payment Provider</label>
                                    <select
                                        value={provider}
                                        onChange={(e) => setProvider(e.target.value)}
                                        className="w-full bg-gray-800 text-white border border-gray-700 rounded-md px-3 py-2"
                                    >
                                        {PAYMENT_PROVIDERS.map(code => (
                                            <option key={code} value={code}>{code}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="text-sm text-gray-400 block mb-2">Voucher Code (Optional)</label>
                                    <input
                                        type="text"
                                        placeholder="Enter voucher code"
                                        value={voucherCode}
                                        onChange={(e) => setVoucherCode(e.target.value)}
                                        className="w-full bg-gray-800 text-white border border-gray-700 rounded-md px-3 py-2 placeholder-gray-500"
                                    />
                                </div>
                                {quote && (
                                    <div className="pt-4 border-t border-gray-800">
                                        <p className="text-gray-400 text-sm">Total Price</p>
                                        <p className="text-white text-3xl font-bold">
                                            ${quote.amount.toFixed(2)} {selectedPlan.currency}
                                        </p>
                                    </div>
                                )}
                            </CardContent>
                            <CardFooter>
                                <Button
                                    className="w-full bg-red-600 hover:bg-red-700 text-white"
                                    size="lg"
                                    onClick={handleCreatePayment}
                                    disabled={creatingPayment}
                                >
                                    {creatingPayment ? 'Processing...' : 'Proceed to Payment'}
                                </Button>
                            </CardFooter>
                        </Card>
                    </div>
                )}

                {/* Active Subscriptions */}
                {subscriptions.length > 0 && (
                    <div className="max-w-4xl mx-auto">
                        <h2 className="text-white text-2xl font-semibold mb-6">Active Subscriptions</h2>
                        <div className="space-y-4">
                            {renderSubscriptions()}
                        </div>
                    </div>
                )}
            </div>

            <Modal
                title="Cancel Subscription"
                open={cancelModal.visible}
                onOk={handleCancelSubscription}
                onCancel={() => setCancelModal({ visible: false, subscriptionId: null })}
                okText="Confirm"
                okButtonProps={{ danger: true }}
            >
                <Form layout="vertical">
                    <Form.Item label="Reason (optional)">
                        <Input.TextArea
                            rows={3}
                            placeholder="Let us know why you are cancelling"
                            value={cancelReason}
                            onChange={(event) => setCancelReason(event.target.value)}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default Subscriptions;
