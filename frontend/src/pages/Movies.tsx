import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Typography, Spin, Empty, Result, Button, Space } from 'antd';
import styled from 'styled-components';
import api from '../services/api';

const { Title } = Typography;

const MoviesContainer = styled.div`
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
`;

const MovieCard = styled(Card)`
    height: 100%;
    transition: transform 0.2s;
    
    &:hover {
        transform: translateY(-5px);
    }

    .ant-card-cover img {
        height: 300px;
        object-fit: cover;
    }

    .ant-card-meta-title {
        font-size: 1.1rem;
        margin-bottom: 8px;
    }

    .ant-card-meta-description {
        color: #666;
    }
`;

const MoviesGrid = styled(Row)`
    margin-top: 2rem;
`;

interface Movie {
    id: number;
    title: string;
    rating: number;
    posterUrl: string;
}

const Movies: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchMovies();
    }, []);

    const fetchMovies = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.get('/api/movies');
            setMovies(response.data ?? []);
        } catch (err) {
            console.error('Error fetching movies:', err);
            setError('We could not load movies right now. Please try again shortly.');
            setMovies([]);
        } finally {
            setLoading(false);
        }
    };

    const renderMoviesList = () => {
        if (loading) {
            return (
                <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                    <Spin size="large" />
                </Col>
            );
        }

        if (error) {
            return (
                <Col span={24}>
                    <Result
                        status="warning"
                        title="Unable to fetch movies"
                        subTitle={error}
                        extra={
                            <Button type="primary" onClick={fetchMovies}>
                                Retry
                            </Button>
                        }
                    />
                </Col>
            );
        }

        if (movies.length === 0) {
            return (
                <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                    <Empty description="No movies available" />
                </Col>
            );
        }

        return movies.map((movie) => (
            <Col xs={24} sm={12} md={8} lg={6} key={movie.id}>
                <MovieCard
                    hoverable
                    cover={<img alt={movie.title} src={movie.posterUrl} />}
                >
                    <Card.Meta
                        title={movie.title}
                        description={`Rating: ${movie.rating}/10`}
                    />
                </MovieCard>
            </Col>
        ));
    };

    return (
        <MoviesContainer>
            <SpaceBetweenHeader>
                <Title level={2}>Movies</Title>
                {!loading && !error && (
                    <Button type="link" onClick={fetchMovies}>
                        Refresh
                    </Button>
                )}
            </SpaceBetweenHeader>
            <MoviesGrid gutter={[16, 16]}>
                {renderMoviesList()}
            </MoviesGrid>
        </MoviesContainer>
    );
};

const SpaceBetweenHeader = styled.div`
    display: flex;
    justify-content: space-between;
    align-items: center;
`;

export default Movies;