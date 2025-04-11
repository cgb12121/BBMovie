import React, { useState, useEffect } from 'react';
import { Typography, Card, Row, Col, Spin } from 'antd';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import api from '../services/api';

const { Title } = Typography;

const CategoriesContainer = styled.div`
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
`;

const CategoryCard = styled(Card)`
    height: 200px;
    cursor: pointer;
    transition: transform 0.2s;
    background-size: cover;
    background-position: center;
    position: relative;
    overflow: hidden;
    
    &:hover {
        transform: translateY(-5px);
    }
    
    &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
    }
    
    .ant-card-meta-title {
        color: white;
        position: relative;
        z-index: 1;
    }
`;

const CategoriesGrid = styled(Row)`
    margin-top: 2rem;
`;

interface Category {
    id: number;
    name: string;
    image: string;
}

const Categories: React.FC = () => {
    const [loading, setLoading] = useState(true);
    const [categories, setCategories] = useState<Category[]>([]);
    const navigate = useNavigate();

    useEffect(() => {
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        try {
            setLoading(true);
            const response = await api.get('/categories');
            setCategories(response.data);
        } catch (error) {
            console.error('Error fetching categories:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCategoryClick = (categoryId: number) => {
        navigate(`/categories/${categoryId}`);
    };

    return (
        <CategoriesContainer>
            <Title level={2}>Movie Categories</Title>
            
            <CategoriesGrid gutter={[16, 16]}>
                {loading ? (
                    <Col span={24} style={{ textAlign: 'center', padding: '2rem' }}>
                        <Spin size="large" />
                    </Col>
                ) : (
                    categories.map(category => (
                        <Col xs={24} sm={12} md={8} key={category.id}>
                            <CategoryCard
                                hoverable
                                onClick={() => handleCategoryClick(category.id)}
                                style={{ backgroundImage: `url(${category.image})` }}
                            >
                                <Card.Meta title={category.name} />
                            </CategoryCard>
                        </Col>
                    ))
                )}
            </CategoriesGrid>
        </CategoriesContainer>
    );
};

export default Categories; 