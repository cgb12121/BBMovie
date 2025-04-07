import React from 'react';
import { Typography, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';

const { Title, Text } = Typography;

const NotFoundContainer = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    text-align: center;
    padding: 2rem;
`;

const NotFound: React.FC = () => {
    const navigate = useNavigate();

    return (
        <NotFoundContainer>
            <Title level={1}>404</Title>
            <Title level={2}>Page Not Found</Title>
            <Text type="secondary" style={{ fontSize: '1.2rem', marginBottom: '2rem' }}>
                The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.
            </Text>
            <Button type="primary" size="large" onClick={() => navigate('/')}>
                Go to Homepage
            </Button>
        </NotFoundContainer>
    );
};

export default NotFound; 