-- =============================================================
-- ClickHouse Schema for Revenue Analytics
-- Run this manually on your ClickHouse instance once
-- =============================================================

-- Database
CREATE DATABASE IF NOT EXISTS bbmovie_analytics;

-- =============================================================
-- Raw event table (ingested from NATS)
-- =============================================================
CREATE TABLE IF NOT EXISTS bbmovie_analytics.revenue_events
(
    event_type      LowCardinality(String),
    transaction_id  String,
    user_id         String,
    subscription_id String,
    plan_id         String,
    plan_type       LowCardinality(String),
    amount          Decimal64(2),
    currency        LowCardinality(String),
    provider        LowCardinality(String),
    billing_cycle   LowCardinality(String),
    event_timestamp DateTime64(3, 'UTC'),
    ingested_at     DateTime64(3, 'UTC') DEFAULT now64()
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_timestamp)
ORDER BY (event_type, event_timestamp, user_id)
TTL event_timestamp + INTERVAL 2 YEAR;

-- =============================================================
-- Materialized view: MRR (Monthly Recurring Revenue)
-- =============================================================
CREATE TABLE IF NOT EXISTS bbmovie_analytics.mrr_daily
(
    date        Date,
    plan_type   LowCardinality(String),
    provider    LowCardinality(String),
    billing_cycle LowCardinality(String),
    mrr         Decimal64(2)
)
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (date, plan_type, provider, billing_cycle);

-- =============================================================
-- Materialized view: Subscription lifecycle
-- =============================================================
CREATE TABLE IF NOT EXISTS bbmovie_analytics.subscription_events
(
    date            Date,
    event_type      LowCardinality(String),
    plan_type       LowCardinality(String),
    provider        LowCardinality(String),
    billing_cycle   LowCardinality(String),
    count           UInt64,
    revenue         Decimal64(2)
)
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (date, event_type, plan_type, provider, billing_cycle);

-- =============================================================
-- Indexes for performance
-- =============================================================
ALTER TABLE bbmovie_analytics.revenue_events ADD INDEX IF NOT EXISTS idx_user_id (user_id) TYPE bloom_filter GRANULARITY 4;
ALTER TABLE bbmovie_analytics.revenue_events ADD INDEX IF NOT EXISTS idx_subscription_id (subscription_id) TYPE bloom_filter GRANULARITY 4;
