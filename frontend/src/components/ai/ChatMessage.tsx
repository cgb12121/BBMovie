import React from 'react';
import { Avatar, Typography, Card } from 'antd';
import { UserOutlined, RobotOutlined } from '@ant-design/icons';
import styled from 'styled-components';

const { Text } = Typography;

interface ChatMessageProps {
     type: 'user' | 'assistant' | 'system' | 'error';
     content: string;
     thinking?: string;
}

const MessageContainer = styled.div<{ type: string }>`
    display: flex;
    justify-content: ${props => props.type === 'user' ? 'flex-end' : 'flex-start'};
    margin-bottom: 16px;
    padding: 0 8px;
`;

const MessageContent = styled(Card) <{ type: string }>`
    max-width: 80%;
    background-color: ${props => props.type === 'user' ? '#1890ff' : '#f0f2f5'};
    
    .ant-card-body {
        padding: 8px 12px;
    }

    p {
        margin: 0;
        color: ${props => props.type === 'user' ? '#fff' : 'rgba(0, 0, 0, 0.85)'};
        white-space: pre-wrap;
    }
`;

const ThinkingBlock = styled.div`
    font-size: 12px;
    color: #8c8c8c;
    margin-bottom: 4px;
    font-style: italic;
    border-left: 2px solid #d9d9d9;
    padding-left: 8px;
`;

const ChatMessage: React.FC<ChatMessageProps> = ({ type, content, thinking }) => {
     if (type === 'system') {
          return (
               <div style={{ textAlign: 'center', margin: '8px 0', color: '#8c8c8c', fontSize: '12px' }}>
                    {content}
               </div>
          );
     }

     return (
          <MessageContainer type={type}>
               {type !== 'user' && (
                    <Avatar icon={<RobotOutlined />} style={{ marginRight: 8, backgroundColor: '#52c41a' }} />
               )}
               <MessageContent type={type} bordered={false} size="small">
                    {thinking && (
                         <ThinkingBlock>
                              Thinking: {thinking}
                         </ThinkingBlock>
                    )}
                    <Text>{content}</Text>
               </MessageContent>
               {type === 'user' && (
                    <Avatar icon={<UserOutlined />} style={{ marginLeft: 8, backgroundColor: '#1890ff' }} />
               )}
          </MessageContainer>
     );
};

export default ChatMessage;
