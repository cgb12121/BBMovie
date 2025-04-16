import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Typography, Spin } from 'antd';
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

    useEffect(() => {
        fetchMovies();
    }, []);

    const fetchMovies = async () => {
        try {
            setLoading(true);
            const response = await api.get('/movies');
            setMovies(response.data);
        } catch (error) {
            console.error('Error fetching movies:', error);
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

        if (movies.length === 0) {
            return (
                <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                    <Title level={4}>No movies found</Title>
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
            <Title level={2}>Movies</Title>
            <MoviesGrid gutter={[16, 16]}>
                {renderMoviesList()}
            </MoviesGrid>
        </MoviesContainer>
    );
};

export default Movies;