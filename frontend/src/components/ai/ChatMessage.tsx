import React from 'react';
import { Avatar, Typography, Card, Tag, Space, Tooltip } from 'antd';
import { UserOutlined, RobotOutlined, FileImageOutlined, FilePdfOutlined, AudioOutlined, FileTextOutlined, FileOutlined } from '@ant-design/icons';
import styled from 'styled-components';

const { Text } = Typography;

interface FileAttachment {
     id: string;
     fileName: string;
     fileType: string;
     fileSize: number;
     url?: string;
}

interface ChatMessageProps {
     type: 'user' | 'assistant' | 'system' | 'error';
     content: string;
     thinking?: string;
     fileAttachments?: FileAttachment[];
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

const getIconForFileType = (fileType: string) => {
     if (fileType.startsWith('image/')) {
          return <FileImageOutlined />;
     } else if (fileType.startsWith('audio/')) {
          return <AudioOutlined />;
     } else if (fileType === 'application/pdf') {
          return <FilePdfOutlined />;
     } else if (fileType.startsWith('text/')) {
          return <FileTextOutlined />;
     } else {
          return <FileOutlined />;
     }
};

const formatFileSize = (bytes: number): string => {
     if (bytes === 0) return '0 Bytes';
     const k = 1024;
     const sizes = ['Bytes', 'KB', 'MB', 'GB'];
     const i = Math.floor(Math.log(bytes) / Math.log(k));
     return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const ChatMessage: React.FC<ChatMessageProps> = ({ type, content, thinking, fileAttachments }) => {
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
                    {fileAttachments && fileAttachments.length > 0 && (
                         <div style={{ marginBottom: '8px' }}>
                              <Space wrap size={[4, 4]}>
                                   {fileAttachments.map((attachment) => (
                                        <Tooltip title={`${attachment.fileName} (${formatFileSize(attachment.fileSize)})`} key={attachment.id}>
                                             <Tag
                                                  icon={getIconForFileType(attachment.fileType)}
                                                  style={{ marginBottom: 0, cursor: 'pointer' }}
                                             >
                                                  {attachment.fileName.length > 20
                                                       ? attachment.fileName.substring(0, 17) + '...'
                                                       : attachment.fileName}
                                             </Tag>
                                        </Tooltip>
                                   ))}
                              </Space>
                         </div>
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
