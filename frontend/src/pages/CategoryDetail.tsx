import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Typography, Spin, Row, Col, Card, Empty, Result, Button } from 'antd';
import styled from 'styled-components';
import api from '../services/api';

const { Title, Paragraph } = Typography;

const CategoryDetailContainer = styled.div`
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

interface Category {
    id: number;
    name: string;
    description: string;
}

interface Movie {
    id: number;
    title: string;
    description?: string;
    posterUrl?: string;
}

const CategoryDetail: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [category, setCategory] = useState<Category | null>(null);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchCategoryDetails = async () => {
        if (!id) return;
        try {
            setLoading(true);
            setError(null);
            const [categoryResponse, moviesResponse] = await Promise.all([
                api.get(`/api/categories/${id}`),
                api.get(`/api/categories/${id}/movies`)
            ]);
            setCategory(categoryResponse.data);
            setMovies(Array.isArray(moviesResponse.data) ? moviesResponse.data : []);
        } catch (err) {
            console.error('Failed to fetch category details', err);
            setError('We could not load this category at the moment. Please try again later.');
            setCategory(null);
            setMovies([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCategoryDetails();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '2rem' }}>
                <Spin size="large" />
            </div>
        );
    }

    if (error) {
        return (
            <CategoryDetailContainer>
                <Result
                    status="warning"
                    title="Category unavailable"
                    subTitle={error}
                    extra={
                        <Button type="primary" onClick={fetchCategoryDetails}>
                            Retry
                        </Button>
                    }
                />
            </CategoryDetailContainer>
        );
    }

    if (!category) {
        return (
            <CategoryDetailContainer>
                <Empty description="Category not found" />
            </CategoryDetailContainer>
        );
    }

    return (
        <CategoryDetailContainer>
            <Title level={2}>{category.name}</Title>
            <Paragraph>{category.description}</Paragraph>

            <Row gutter={[16, 16]} style={{ marginTop: '2rem' }}>
                {movies.length === 0 ? (
                    <Col span={24} style={{ textAlign: 'center' }}>
                        <Empty description="No movies in this category" />
                    </Col>
                ) : (
                    movies.map(movie => (
                        <Col xs={24} sm={12} md={8} lg={6} key={movie.id}>
                            <MovieCard
                                hoverable
                                cover={movie.posterUrl ? <img alt={movie.title} src={movie.posterUrl} /> : undefined}
                            >
                                <Card.Meta
                                    title={movie.title}
                                    description={movie.description}
                                />
                            </MovieCard>
                        </Col>
                    ))
                )}
            </Row>
        </CategoryDetailContainer>
    );
};

export default CategoryDetail; 