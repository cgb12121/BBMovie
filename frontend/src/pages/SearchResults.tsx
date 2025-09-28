import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Typography, Tabs, Card, Row, Col, Spin, Empty, Result, Button } from 'antd';
import styled from 'styled-components';
import api from '../services/api';

const { Title } = Typography;
const { TabPane } = Tabs;

const SearchContainer = styled.div`
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
    margin-top: 80px;
`;

const ResultsGrid = styled(Row)`
    margin-top: 2rem;
`;

const ResultCard = styled(Card)`
    height: 100%;
    transition: transform 0.2s;
    
    &:hover {
        transform: translateY(-5px);
    }
`;

type Movie = {
    id: string;
    title: string;
    description?: string;
    posterUrl?: string;
    rating?: number;
};

type SearchState<T> = {
    data: T[];
    loading: boolean;
    error: string | null;
};

const SearchResults: React.FC = () => {
    const location = useLocation();
    const [activeTab, setActiveTab] = useState<'movies'>('movies');
    const [moviesState, setMoviesState] = useState<SearchState<Movie>>({
        data: [],
        loading: false,
        error: null
    });

    const searchQuery = new URLSearchParams(location.search).get('query') ?? '';

    useEffect(() => {
        if (searchQuery) {
            fetchMovies();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchQuery, activeTab]);

    const fetchMovies = async () => {
        if (!searchQuery.trim()) {
            setMoviesState({ data: [], loading: false, error: null });
            return;
        }

        setMoviesState(prev => ({ ...prev, loading: true, error: null }));
        try {
            const response = await api.get('/api/search/similar-search', {
                params: { query: searchQuery }
            });
            setMoviesState({
                data: Array.isArray(response.data) ? response.data : [],
                loading: false,
                error: null
            });
        } catch (error) {
            console.error('Error searching movies:', error);
            setMoviesState({
                data: [],
                loading: false,
                error: 'We could not load search results. Please try again.'
            });
        }
    };

    const renderMovies = () => {
        if (moviesState.loading) {
            return (
                <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                    <Spin size="large" />
                </Col>
            );
        }

        if (moviesState.error) {
            return (
                <Col span={24}>
                    <Result
                        status="warning"
                        title="Unable to fetch movies"
                        subTitle={moviesState.error}
                        extra={
                            <Button type="primary" onClick={fetchMovies}>
                                Retry
                            </Button>
                        }
                    />
                </Col>
            );
        }

        if (moviesState.data.length === 0) {
            return (
                <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                    <Empty description="No movies found" />
                </Col>
            );
        }

        return moviesState.data.map(movie => (
            <Col xs={24} sm={12} md={8} lg={6} key={movie.id}>
                <ResultCard
                    hoverable
                    cover={movie.posterUrl ? <img alt={movie.title} src={movie.posterUrl} /> : undefined}
                >
                    <Card.Meta
                        title={movie.title}
                        description={
                            movie.description || (typeof movie.rating === 'number' ? `Rating: ${movie.rating}/10` : null)
                        }
                    />
                </ResultCard>
            </Col>
        ));
    };

    return (
        <SearchContainer>
            <Title level={2}>Search Results for "{searchQuery}"</Title>
            <Tabs activeKey={activeTab} onChange={(key) => setActiveTab(key as 'movies')}>
                <TabPane tab="Movies" key="movies">
                    <ResultsGrid gutter={[16, 16]}>
                        {renderMovies()}
                    </ResultsGrid>
                </TabPane>
            </Tabs>
        </SearchContainer>
    );
};

export default SearchResults;