import React, { useEffect, useMemo, useState } from 'react';
import {
    Alert,
    Button,
    Card,
    Col,
    Descriptions,
    Empty,
    Form,
    Input,
    List,
    message,
    Modal,
    Row,
    Select,
    Space,
    Spin,
    Statistic,
    Switch,
    Tag,
    Tooltip,
    Typography
} from 'antd';
import {
    DollarCircleOutlined,
    ReloadOutlined,
    ShoppingCartOutlined,
    StopOutlined
} from '@ant-design/icons';
import paymentService, {
    CancelSubscriptionRequest,
    PAYMENT_PROVIDERS,
    SubscriptionPlan,
    SubscriptionPaymentRequest,
    UserSubscription
} from '../services/paymentService';

const { Title, Paragraph, Text } = Typography;

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
        return (
            <Card
                key={plan.id}
                hoverable
                onClick={() => setSelectedPlanId(plan.id)}
                className={isSelected ? 'plan-card selected' : 'plan-card'}
                style={{ borderColor: isSelected ? '#E50914' : undefined }}
            >
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <Title level={4} style={{ margin: 0 }}>{plan.name}</Title>
                    <Tag color="volcano">{plan.planType}</Tag>
                    <Statistic
                        prefix={<DollarCircleOutlined />}
                        title="Monthly Price"
                        value={plan.monthlyPrice}
                        precision={2}
                        suffix={plan.currency}
                    />
                    {plan.annualFinalPrice && (
                        <Statistic
                            title="Annual Price"
                            value={plan.annualFinalPrice}
                            precision={2}
                            suffix={plan.currency}
                        />
                    )}
                    {plan.annualDiscountPercent && plan.annualDiscountPercent > 0 && (
                        <Alert
                            type="success"
                            message={`Save ${plan.annualDiscountPercent}% on annual billing`}
                            showIcon
                        />
                    )}
                    {plan.features.length > 0 ? (
                        <List
                            size="small"
                            dataSource={plan.features}
                            renderItem={(feature) => <List.Item>{feature}</List.Item>}
                        />
                    ) : (
                        <Paragraph type="secondary">No feature list provided.</Paragraph>
                    )}
                    <Button type={isSelected ? 'primary' : 'default'} block>
                        {isSelected ? 'Selected' : 'Choose Plan'}
                    </Button>
                </Space>
            </Card>
        );
    };

    const renderSubscriptions = () => {
        if (subscriptionsLoading) {
            return <Spin />;
        }

        if (subscriptions.length === 0) {
            return <Empty description="No active subscriptions" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
        }

        return (
            <List
                dataSource={subscriptions}
                renderItem={subscription => (
                    <Card
                        key={subscription.id}
                        style={{ marginBottom: '1rem' }}
                        title={`Subscription ${subscription.id}`}
                        extra={
                            <Space>
                                <Tooltip title="Toggle auto-renew">
                                    <Switch
                                        checkedChildren="Auto-renew"
                                        unCheckedChildren="Auto-renew"
                                        checked={subscription.autoRenew}
                                        onChange={(checked) => handleToggleAutoRenew(subscription, checked)}
                                    />
                                </Tooltip>
                                <Button
                                    danger
                                    icon={<StopOutlined />}
                                    onClick={() => setCancelModal({ visible: true, subscriptionId: subscription.id })}
                                >
                                    Cancel
                                </Button>
                            </Space>
                        }
                    >
                        <Descriptions column={1} size="small">
                            <Descriptions.Item label="Plan ID">{subscription.planId}</Descriptions.Item>
                            <Descriptions.Item label="Provider">{subscription.provider}</Descriptions.Item>
                            <Descriptions.Item label="Payment Method">{subscription.paymentMethod ?? 'N/A'}</Descriptions.Item>
                            <Descriptions.Item label="Start Date">{subscription.startDate ?? 'N/A'}</Descriptions.Item>
                            <Descriptions.Item label="End Date">{subscription.endDate ?? 'N/A'}</Descriptions.Item>
                            <Descriptions.Item label="Next Payment">{subscription.nextPaymentDate ?? 'N/A'}</Descriptions.Item>
                        </Descriptions>
                    </Card>
                )}
            />
        );
    };

    return (
        <div style={{ padding: '2rem', minHeight: '100vh' }}>
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
                    <div>
                        <Title level={2}>Manage Subscriptions</Title>
                        <Paragraph type="secondary">
                            Choose a plan, subscribe via your preferred provider, and manage auto-renew or cancellation from here.
                        </Paragraph>
                    </div>
                    <Button icon={<ReloadOutlined />} onClick={refreshAll}>
                        Refresh
                    </Button>
                </Space>

                <Row gutter={24}>
                    <Col xs={24} lg={16}>
                        <Card
                            title="Available Plans"
                            extra={plansLoading ? <Spin size="small" /> : null}
                        >
                            {plansLoading ? (
                                <Spin />
                            ) : plans.length === 0 ? (
                                <Empty description="No plans available" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                            ) : (
                                <Row gutter={[16, 16]}>
                                    {plans.map(plan => (
                                        <Col key={plan.id} xs={24} sm={12}>
                                            {renderPlanCard(plan)}
                                        </Col>
                                    ))}
                                </Row>
                            )}
                        </Card>
                    </Col>

                    <Col xs={24} lg={8}>
                        <Card title="Checkout">
                            <Space direction="vertical" size="large" style={{ width: '100%' }}>
                                <Select
                                    value={billingCycle}
                                    onChange={(value: BillingCycleValue) => setBillingCycle(value)}
                                    options={billingCycleOptions.map(option => ({ label: option.label, value: option.value }))}
                                    disabled={!selectedPlan}
                                />
                                <Select
                                    value={provider}
                                    onChange={setProvider}
                                    options={PAYMENT_PROVIDERS.map(code => ({ label: code, value: code }))}
                                />
                                <Input
                                    placeholder="Voucher code (optional)"
                                    value={voucherCode}
                                    onChange={(event) => setVoucherCode(event.target.value)}
                                />
                                <Space direction="vertical" style={{ width: '100%' }}>
                                    <Text type="secondary">Estimated price</Text>
                                    {quoteLoading ? (
                                        <Spin size="small" />
                                    ) : quote ? (
                                        <Title level={4} style={{ margin: 0 }}>
                                            {quote.amount.toFixed(2)} {selectedPlan?.currency}
                                        </Title>
                                    ) : (
                                        <Text type="secondary">Select a plan to see pricing.</Text>
                                    )}
                                </Space>
                                <Button
                                    type="primary"
                                    icon={<ShoppingCartOutlined />}
                                    block
                                    disabled={!selectedPlan}
                                    loading={creatingPayment}
                                    onClick={handleCreatePayment}
                                >
                                    Proceed to Payment
                                </Button>
                            </Space>
                        </Card>

                        <Card title="Active Subscriptions" style={{ marginTop: '1.5rem' }}>
                            {renderSubscriptions()}
                        </Card>
                    </Col>
                </Row>
            </Space>

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
