import { Modal } from "antd"
import styled from "styled-components"


export const StyledModal = styled(Modal)`
     .ant-modal-content {
          background: rgba(20, 20, 20, 0.9);
          backdrop-filter: blur(10px);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 12px;
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
     }
     
     .ant-modal-body {
          padding: 32px;
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
     }
     
     .ant-modal-close {
          color: rgba(255, 255, 255, 0.7);
          
          &:hover {
               color: #ffffff;
          }
     }
`

export const IconWrapper = styled.div`
     font-size: 64px;
     margin-bottom: 24px;
`

export const ModalTitle = styled.h3`
     color: #ffffff;
     font-size: 24px;
     margin-bottom: 12px;
`

export const ModalMessage = styled.p`
     color: rgba(255, 255, 255, 0.7);
     font-size: 16px;
     margin-bottom: 24px;
`

export const ProgressBar = styled.div<{ status: string }>`
     width: 100%;
     height: 4px;
     background: rgba(255, 255, 255, 0.1);
     border-radius: 2px;
     overflow: hidden;
     position: relative;
     
     &::after {
          content: '';
          position: absolute;
          left: 0;
          top: 0;
          height: 100%;
          width: 100%;
          background: ${({ status }) => (status === "success" ? "#52c41a" : "#ff4d4f")};
          animation: progress 5s linear forwards;
     }
     
     @keyframes progress {
          0% { width: 100%; }
          100% { width: 0%; }
     }
`