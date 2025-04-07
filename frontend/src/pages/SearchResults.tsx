import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Typography, Tabs, Card, Row, Col, Spin } from 'antd';
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

interface Movie {
    id: number;
    title: string;
    posterUrl: string;
    rating: number;
}

interface Category {
    id: number;
    name: string;
    image: string;
}

const SearchResults: React.FC = () => {
    const location = useLocation();
    const [activeTab, setActiveTab] = useState('movies');
    const [loading, setLoading] = useState(false);
    const [movies, setMovies] = useState<Movie[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);

    const searchQuery = new URLSearchParams(location.search).get('q') || '';

    useEffect(() => {
        if (searchQuery) {
            handleSearch();
        }
    }, [searchQuery, activeTab]);

    const handleSearch = async () => {
        if (!searchQuery.trim()) return;

        setLoading(true);
        try {
            if (activeTab === 'movies') {
                const response = await api.get('/movies/search', {
                    params: { query: searchQuery }
                });
                setMovies(response.data);
            } else {
                const response = await api.get('/categories/search', {
                    params: { query: searchQuery }
                });
                setCategories(response.data);
            }
        } catch (error) {
            console.error('Error searching:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <SearchContainer>
            <Title level={2}>Search Results for "{searchQuery}"</Title>
            
            <Tabs activeKey={activeTab} onChange={setActiveTab}>
                <TabPane tab="Movies" key="movies">
                    <ResultsGrid gutter={[16, 16]}>
                        {loading ? (
                            <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                                <Spin size="large" />
                            </Col>
                        ) : movies.length > 0 ? (
                            movies.map(movie => (
                                <Col xs={24} sm={12} md={8} lg={6} key={movie.id}>
                                    <ResultCard
                                        hoverable
                                        cover={<img alt={movie.title} src={movie.posterUrl} />}
                                    >
                                        <Card.Meta
                                            title={movie.title}
                                            description={`Rating: ${movie.rating}/10`}
                                        />
                                    </ResultCard>
                                </Col>
                            ))
                        ) : (
                            <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                                <Title level={4}>No movies found</Title>
                            </Col>
                        )}
                    </ResultsGrid>
                </TabPane>
                
                <TabPane tab="Categories" key="categories">
                    <ResultsGrid gutter={[16, 16]}>
                        {loading ? (
                            <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                                <Spin size="large" />
                            </Col>
                        ) : categories.length > 0 ? (
                            categories.map(category => (
                                <Col xs={24} sm={12} md={8} key={category.id}>
                                    <ResultCard
                                        hoverable
                                        cover={<img alt={category.name} src={category.image} />}
                                    >
                                        <Card.Meta title={category.name} />
                                    </ResultCard>
                                </Col>
                            ))
                        ) : (
                            <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                                <Title level={4}>No categories found</Title>
                            </Col>
                        )}
                    </ResultsGrid>
                </TabPane>
            </Tabs>
        </SearchContainer>
    );
};

export default SearchResults; 