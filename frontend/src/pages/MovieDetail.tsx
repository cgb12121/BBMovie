import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Typography, Rate, Divider, Spin, Row, Col, Card } from 'antd';
import styled from 'styled-components';

const { Title, Text, Paragraph } = Typography;

const MovieDetailContainer = styled.div`
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
`;

const MovieHeader = styled.div`
    display: flex;
    gap: 2rem;
    margin-bottom: 2rem;
    
    @media (max-width: 768px) {
        flex-direction: column;
    }
`;

const MoviePoster = styled.img`
    width: 300px;
    height: 450px;
    object-fit: cover;
    border-radius: 8px;
`;

const MovieInfo = styled.div`
    flex: 1;
`;

const ReviewCard = styled(Card)`
    margin-bottom: 1rem;
`;

const MovieDetail: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [loading, setLoading] = useState(true);
    const [movie, setMovie] = useState<any>(null);
    const [reviews, setReviews] = useState<any[]>([]);

    useEffect(() => {
        // TODO: Fetch movie details and reviews
        const fetchMovie = async () => {
            try {
                setLoading(true);
                // Mock data for now
                setMovie({
                    id,
                    title: 'Sample Movie',
                    description: 'This is a sample movie description.',
                    rating: 8.5,
                    releaseDate: '2023-01-01',
                    duration: '120 min',
                    genres: ['Action', 'Adventure'],
                    posterUrl: 'https://via.placeholder.com/300x450'
                });
                setReviews([
                    {
                        id: 1,
                        author: 'John Doe',
                        rating: 9,
                        comment: 'Great movie!'
                    }
                ]);
            } catch (error) {
                console.error('Error fetching movie:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchMovie();
    }, [id]);

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '2rem' }}>
                <Spin size="large" />
            </div>
        );
    }

    if (!movie) {
        return (
            <div style={{ textAlign: 'center', padding: '2rem' }}>
                <Title level={4}>Movie not found</Title>
            </div>
        );
    }

    return (
        <MovieDetailContainer>
            <MovieHeader>
                <MoviePoster src={movie.posterUrl} alt={movie.title} />
                <MovieInfo>
                    <Title level={2}>{movie.title}</Title>
                    <Rate disabled defaultValue={movie.rating / 2} />
                    <Text type="secondary">({movie.rating}/10)</Text>
                    
                    <Divider />
                    
                    <Row gutter={[16, 16]}>
                        <Col span={12}>
                            <Text strong>Release Date: </Text>
                            <Text>{movie.releaseDate}</Text>
                        </Col>
                        <Col span={12}>
                            <Text strong>Duration: </Text>
                            <Text>{movie.duration}</Text>
                        </Col>
                        <Col span={24}>
                            <Text strong>Genres: </Text>
                            <Text>{movie.genres.join(', ')}</Text>
                        </Col>
                    </Row>
                    
                    <Divider />
                    
                    <Title level={4}>Description</Title>
                    <Paragraph>{movie.description}</Paragraph>
                </MovieInfo>
            </MovieHeader>
            
            <Divider />
            
            <Title level={3}>Reviews</Title>
            {reviews.length > 0 ? (
                reviews.map(review => (
                    <ReviewCard key={review.id}>
                        <Card.Meta
                            title={review.author}
                            description={
                                <>
                                    <Rate disabled defaultValue={review.rating} />
                                    <Paragraph>{review.comment}</Paragraph>
                                </>
                            }
                        />
                    </ReviewCard>
                ))
            ) : (
                <Text type="secondary">No reviews yet</Text>
            )}
        </MovieDetailContainer>
    );
};

export default MovieDetail; 