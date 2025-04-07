import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Typography, Spin } from 'antd';
import styled from 'styled-components';
import api from '../services/api';
import SearchBar from '../components/SearchBar';

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
`;

const MovieGrid = styled(Row)`
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

    const handleSearch = async (value: string) => {
        try {
            setLoading(true);
            const response = await api.get('/movies/search', {
                params: { query: value }
            });
            setMovies(response.data);
        } catch (error) {
            console.error('Error searching movies:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <MoviesContainer>
            <Title level={2}>Movies</Title>
            <SearchBar
                placeholder="Search movies..."
                onSearch={handleSearch}
                loading={loading}
            />
            
            <MovieGrid gutter={[16, 16]}>
                {loading ? (
                    <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                        <Spin size="large" />
                    </Col>
                ) : movies.length > 0 ? (
                    movies.map((movie) => (
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
                    ))
                ) : (
                    <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                        <Title level={4}>No movies found</Title>
                    </Col>
                )}
            </MovieGrid>
        </MoviesContainer>
    );
};

export default Movies;