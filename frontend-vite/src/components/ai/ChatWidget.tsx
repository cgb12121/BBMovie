import React, { useState } from 'react';
import { Button } from 'antd';
import { MessageOutlined, CloseOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import { motion, AnimatePresence } from 'framer-motion';
import ChatWindow from './ChatWindow';

const WidgetContainer = styled.div`
    position: fixed;
    bottom: 24px;
    right: 24px;
    z-index: 1000;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 16px;
`;

const ChatWindowWrapper = styled(motion.div)`
    width: 380px;
    height: 600px;
    background-color: #fff;
    border-radius: 12px;
    box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.05);
    overflow: hidden;
    border: 1px solid #f0f0f0;
`;

const ChatWidget: React.FC = () => {
     const [isOpen, setIsOpen] = useState(false);

     return (
          <WidgetContainer>
               <AnimatePresence>
                    {isOpen && (
                         <ChatWindowWrapper
                              initial={{ opacity: 0, y: 20, scale: 0.95 }}
                              animate={{ opacity: 1, y: 0, scale: 1 }}
                              exit={{ opacity: 0, y: 20, scale: 0.95 }}
                              transition={{ duration: 0.2 }}
                         >
                              <ChatWindow />
                         </ChatWindowWrapper>
                    )}
               </AnimatePresence>

               <Button
                    type="primary"
                    shape="circle"
                    size="large"
                    icon={isOpen ? <CloseOutlined /> : <MessageOutlined />}
                    onClick={() => setIsOpen(!isOpen)}
                    style={{
                         width: 56,
                         height: 56,
                         boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                         display: 'flex',
                         alignItems: 'center',
                         justifyContent: 'center',
                         fontSize: 24
                    }}
               />
          </WidgetContainer>
     );
};

export default ChatWidget;
