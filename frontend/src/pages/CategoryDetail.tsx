import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Typography, Card, Row, Col, Spin, message } from 'antd';
import styled from 'styled-components';
import api from '../services/api';

const { Title } = Typography;

const CategoryDetailContainer = styled.div`
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
`;

const MovieCard = styled(Card)`
    height: 100%;
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover {
        transform: translateY(-5px);
    }
`;

const MoviesGrid = styled(Row)`
    margin-top: 2rem;
`;

interface Category {
    id: number;
    name: string;
    description: string;
}

interface Movie {
    id: number;
    title: string;
    rating: number;
    posterUrl: string;
}

const CategoryDetail: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [loading, setLoading] = useState(true);
    const [category, setCategory] = useState<Category | null>(null);
    const [movies, setMovies] = useState<Movie[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchCategory = async () => {
            try {
                setLoading(true);
                const [categoryResponse, moviesResponse] = await Promise.all([
                    api.get(`/categories/${id}`),
                    api.get(`/categories/${id}/movies`)
                ]);
                setCategory(categoryResponse.data);
                setMovies(moviesResponse.data);
            } catch (error) {
                message.error('Failed to fetch category details');
                console.error('Error fetching category:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchCategory();
    }, [id]);

    const handleMovieClick = (movieId: number) => {
        navigate(`/movies/${movieId}`);
    };

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '2rem' }}>
                <Spin size="large" />
            </div>
        );
    }

    if (!category) {
        return (
            <div style={{ textAlign: 'center', padding: '2rem' }}>
                <Title level={4}>Category not found</Title>
            </div>
        );
    }

    return (
        <CategoryDetailContainer>
            <Title level={2}>{category.name}</Title>
            <Title level={4} type="secondary">{category.description}</Title>
            
            <MoviesGrid gutter={[16, 16]}>
                {movies.length > 0 ? (
                    movies.map(movie => (
                        <Col xs={24} sm={12} md={8} lg={6} key={movie.id}>
                            <MovieCard
                                hoverable
                                onClick={() => handleMovieClick(movie.id)}
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
                        <Title level={4}>No movies in this category</Title>
                    </Col>
                )}
            </MoviesGrid>
        </CategoryDetailContainer>
    );
};

export default CategoryDetail; 