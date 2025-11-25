import React, { useState, useEffect, useRef } from 'react';
import { Input, Button, Empty, Spin } from 'antd';
import { SendOutlined, DeleteOutlined } from '@ant-design/icons';
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
    gap: 8px;
    align-items: flex-end;
`;

interface Message {
     type: 'user' | 'assistant' | 'system' | 'error';
     content: string;
     thinking?: string;
}

const generateUUID = () => {
     return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
          var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
     });
};

const ChatWindow: React.FC = () => {
     const [messages, setMessages] = useState<Message[]>([]);
     const [inputValue, setInputValue] = useState('');
     const [isStreaming, setIsStreaming] = useState(false);
     const [sessionId, setSessionId] = useState('');
     const messagesEndRef = useRef<HTMLDivElement>(null);

     useEffect(() => {
          setSessionId(generateUUID());
          setMessages([{ type: 'system', content: 'Welcome! How can I help you today?' }]);
     }, []);

     useEffect(() => {
          messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
     }, [messages]);

     const handleSend = async () => {
          if (!inputValue.trim() || isStreaming) return;

          const userMessage = inputValue.trim();
          setInputValue('');
          setMessages(prev => [...prev, { type: 'user', content: userMessage }]);
          setIsStreaming(true);

          // Add placeholder for assistant message
          setMessages(prev => [...prev, { type: 'assistant', content: '', thinking: '' }]);

          await aiService.streamChat(
               sessionId,
               {
                    message: userMessage,
                    assistantType: 'user', // Default to user assistant
                    aiMode: 'normal' // Default mode
               },
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
     };

     const handleClear = () => {
          setMessages([{ type: 'system', content: 'Chat cleared.' }]);
          setSessionId(generateUUID());
     };

     const handleKeyDown = (e: React.KeyboardEvent) => {
          if (e.key === 'Enter' && !e.shiftKey) {
               e.preventDefault();
               handleSend();
          }
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
                              />
                         ))
                    )}
                    <div ref={messagesEndRef} />
               </MessageList>
               <InputArea>
                    <TextArea
                         value={inputValue}
                         onChange={e => setInputValue(e.target.value)}
                         onKeyDown={handleKeyDown}
                         placeholder="Type a message..."
                         autoSize={{ minRows: 1, maxRows: 4 }}
                         disabled={isStreaming}
                         style={{ resize: 'none' }}
                    />
                    <Button
                         type="primary"
                         icon={isStreaming ? <Spin size="small" /> : <SendOutlined />}
                         onClick={handleSend}
                         disabled={!inputValue.trim() || isStreaming}
                    />
               </InputArea>
          </WindowContainer>
     );
};

export default ChatWindow;
