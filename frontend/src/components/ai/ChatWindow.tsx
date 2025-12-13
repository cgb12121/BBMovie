import React, { useState, useEffect, useRef } from 'react';
import { Input, Button, Empty, Spin, Upload, Space, Tag, Alert } from 'antd';
import { SendOutlined, DeleteOutlined, UploadOutlined, FileImageOutlined, FilePdfOutlined, AudioOutlined, FileTextOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import ChatMessage from './ChatMessage';
import { aiService, ChatStreamChunk } from '../../services/aiService';

const { TextArea } = Input;

const WindowContainer = styled.div`
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #fff;
`;

const Header = styled.div`
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: #fafafa;
    border-radius: 8px 8px 0 0;
    font-weight: 600;
`;

const MessageList = styled.div`
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background-color: #fff;
`;

const InputArea = styled.div`
    padding: 12px;
    border-top: 1px solid #f0f0f0;
    background-color: #fff;
    border-radius: 0 0 8px 8px;
    display: flex;
    flex-direction: column;
    gap: 8px;
`;

const FileUploadContainer = styled.div`
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
`;

interface Message {
     type: 'user' | 'assistant' | 'system' | 'error';
     content: string;
     thinking?: string;
     fileAttachments?: Array<{
          id: string;
          fileName: string;
          fileType: string;
          fileSize: number;
          url?: string;
     }>;
}

interface ChatAttachment {
     uid: string;
     file: File;
     name: string;
     type: string;
     size: number;
}

const generateUUID = () => {
     return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
          var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
     });
};

const getIconForFileType = (fileType: string) => {
     if (fileType.startsWith('image/')) {
          return <FileImageOutlined />;
     } else if (fileType.startsWith('audio/') || fileType === 'video/mp4') {
          return <AudioOutlined />;
     } else if (fileType === 'application/pdf') {
          return <FilePdfOutlined />;
     } else {
          return <FileTextOutlined />;
     }
};

const ChatWindow: React.FC = () => {
     const [messages, setMessages] = useState<Message[]>([]);
     const [inputValue, setInputValue] = useState('');
     const [isStreaming, setIsStreaming] = useState(false);
     const [sessionId, setSessionId] = useState('');
     const [attachments, setAttachments] = useState<ChatAttachment[]>([]);
     const [uploadError, setUploadError] = useState<string | null>(null);
     const messagesEndRef = useRef<HTMLDivElement>(null);

     useEffect(() => {
          setSessionId(generateUUID());
          setMessages([{ type: 'system', content: 'Welcome! How can I help you today? (Supports file attachments)' }]);
     }, []);

     useEffect(() => {
          messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
     }, [messages]);

     const handleSend = async () => {
          if ((!inputValue.trim() && attachments.length === 0) || isStreaming) return;

          const userMessage = inputValue.trim();
          const attachmentObjects = attachments.map(att => ({
               id: att.uid,
               fileName: att.name,
               fileType: att.type,
               fileSize: att.size,
          }));

          setInputValue('');
          setAttachments([]);
          setUploadError(null);

          setMessages(prev => [...prev, {
               type: 'user',
               content: userMessage,
               fileAttachments: attachmentObjects
          }]);

          setIsStreaming(true);

          // Add placeholder for assistant message
          setMessages(prev => [...prev, { type: 'assistant', content: '', thinking: '' }]);

          try {
               const files = attachments.map(att => att.file);

               await aiService.streamChatWithAttachments(
                    sessionId,
                    userMessage,
                    files,
                    'user',
                    'normal',
                    (chunk: ChatStreamChunk) => {
                         setMessages(prev => {
                              const newMessages = [...prev];
                              const lastMessage = newMessages[newMessages.length - 1];

                              if (lastMessage.type === 'assistant') {
                                   if (chunk.type === 'assistant') {
                                        lastMessage.content += chunk.content || '';
                                        if (chunk.thinking) {
                                             lastMessage.thinking = (lastMessage.thinking || '') + chunk.thinking;
                                        }
                                   }
                              }
                              return newMessages;
                         });
                    },
                    (error) => {
                         console.error('Chat error:', error);
                         setMessages(prev => [...prev, { type: 'error', content: 'Sorry, something went wrong. Please try again.' }]);
                         setIsStreaming(false);
                    },
                    () => {
                         setIsStreaming(false);
                    }
               );
          } catch (error) {
               console.error('Error sending message with attachments:', error);
               setMessages(prev => [...prev, { type: 'error', content: 'Sorry, something went wrong while processing your message.' }]);
               setIsStreaming(false);
          }
     };

     const handleClear = () => {
          setMessages([{ type: 'system', content: 'Chat cleared.' }]);
          setSessionId(generateUUID());
          setAttachments([]);
          setUploadError(null);
     };

     const handleKeyDown = (e: React.KeyboardEvent) => {
          if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
               e.preventDefault();
               handleSend();
          }
     };

     const handleFileChange = ({ fileList }: any) => {
          // Convert antd Upload fileList to our ChatAttachment format
          const newAttachments = fileList.map((file: any) => ({
               uid: file.uid,
               file: file.originFileObj,
               name: file.name,
               type: file.type,
               size: file.size,
          }));
          setAttachments(newAttachments);
     };

     const beforeUpload = (file: File) => {
          // Check file type and size before allowing upload
          const allowedTypes = [
               'image/jpeg',
               'image/jpg',
               'image/png',
               'audio/mp3',
               'audio/wav',
               'audio/m4a',
               'application/pdf',
               'text/plain',
               'text/markdown',
               'application/json',
               'application/xml',
               'text/csv'
          ];

          const maxSize = 100 * 1024 * 1024; // 100MB

          if (!allowedTypes.includes(file.type)) {
               setUploadError('File type not supported. Please upload images, audio, PDF, or text files.');
               return false;
          }

          if (file.size > maxSize) {
               setUploadError('File size exceeds 100MB limit.');
               return false;
          }

          setUploadError(null);
          return true;
     };

     const removeAttachment = (uid: string) => {
          setAttachments(prev => prev.filter(att => att.uid !== uid));
     };

     return (
          <WindowContainer>
               <Header>
                    <span>AI Assistant</span>
                    <Button
                         type="text"
                         icon={<DeleteOutlined />}
                         onClick={handleClear}
                         title="Clear Chat"
                         size="small"
                    />
               </Header>
               <MessageList>
                    {messages.length === 0 ? (
                         <Empty description="No messages yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    ) : (
                         messages.map((msg, index) => (
                              <ChatMessage
                                   key={index}
                                   type={msg.type}
                                   content={msg.content}
                                   thinking={msg.thinking}
                                   fileAttachments={msg.fileAttachments}
                              />
                         ))
                    )}
                    <div ref={messagesEndRef} />
               </MessageList>
               <InputArea>
                    {uploadError && (
                         <Alert
                              message={uploadError}
                              type="error"
                              closable
                              onClose={() => setUploadError(null)}
                         />
                    )}

                    <FileUploadContainer>
                         {attachments.map((attachment) => (
                              <Tag
                                   key={attachment.uid}
                                   icon={getIconForFileType(attachment.type)}
                                   closable
                                   onClose={() => removeAttachment(attachment.uid)}
                                   style={{ marginBottom: '4px' }}
                              >
                                   {attachment.name}
                              </Tag>
                         ))}
                    </FileUploadContainer>

                    <Space style={{ width: '100%' }} direction="vertical">
                         <Upload
                              multiple
                              beforeUpload={beforeUpload}
                              onChange={handleFileChange}
                              fileList={attachments.map(att => ({
                                   uid: att.uid,
                                   name: att.name,
                                   status: 'done' as const,
                                   type: att.type,
                                   size: att.size,
                              }))}
                              showUploadList={false}
                              maxCount={5} // Limit to 5 files at a time
                         >
                              <Button icon={<UploadOutlined />} disabled={isStreaming}>
                                   Attach Files
                              </Button>
                         </Upload>

                         <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
                              <TextArea
                                   value={inputValue}
                                   onChange={e => setInputValue(e.target.value)}
                                   onKeyDown={handleKeyDown}
                                   placeholder="Type a message or attach files..."
                                   autoSize={{ minRows: 1, maxRows: 4 }}
                                   disabled={isStreaming}
                                   style={{ resize: 'none', flex: 1 }}
                              />
                              <Button
                                   type="primary"
                                   icon={isStreaming ? <Spin size="small" /> : <SendOutlined />}
                                   onClick={handleSend}
                                   disabled={(!inputValue.trim() && attachments.length === 0) || isStreaming}
                              >
                                   Send
                              </Button>
                         </div>
                    </Space>
               </InputArea>
          </WindowContainer>
     );
};

export default ChatWindow;
