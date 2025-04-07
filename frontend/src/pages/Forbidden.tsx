import React from 'react';
import { Typography, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { LockOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const ForbiddenContainer = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    text-align: center;
    padding: 2rem;
`;

const IconWrapper = styled.div`
    font-size: 4rem;
    margin-bottom: 1rem;
    color: #ff4d4f;
`;

const Forbidden: React.FC = () => {
    const navigate = useNavigate();

    return (
        <ForbiddenContainer>
            <IconWrapper>
                <LockOutlined />
            </IconWrapper>
            <Title level={1}>403</Title>
            <Title level={2}>Access Forbidden</Title>
            <Text type="secondary" style={{ fontSize: '1.2rem', marginBottom: '2rem' }}>
                You don't have permission to access this page. Please contact the administrator if you believe this is an error.
            </Text>
            <Button type="primary" size="large" onClick={() => navigate('/')}>
                Go to Homepage
            </Button>
        </ForbiddenContainer>
    );
};

export default Forbidden; 