import styled, { keyframes } from "styled-components"
import { Form, Input, Button, Card, Steps } from "antd"

export const pulse = keyframes`
  0% {
    box-shadow: 0 0 0 0 rgba(24, 144, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(24, 144, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(24, 144, 255, 0);
  }
`

export const shimmer = keyframes`
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
`

export const StyledCard = styled(Card)`
  background: rgba(20, 20, 20, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
`

export const StyledForm = styled(Form)`
  .ant-form-item-label > label {
    color: #d9d9d9;
  }
`

export const StyledInput = styled(Input)`
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #ffffff;
  height: 45px;
  padding: 10px 15px;
  
  &:hover, &:focus {
    border-color: #1890ff;
    background: rgba(255, 255, 255, 0.15);
  }
  
  &::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }
  
  .ant-input {
    background: transparent;
    color: #ffffff;
  }
  
  .ant-input-prefix {
    color: rgba(255, 255, 255, 0.7);
    margin-right: 10px;
  }
`

export const StyledPassword = styled(Input.Password)`
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #ffffff;
  height: 45px;
  padding: 10px 15px;
  
  &:hover, &:focus {
    border-color: #1890ff;
    background: rgba(255, 255, 255, 0.15);
  }
  
  &::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }
  
  .ant-input {
    background: transparent;
    color: #ffffff;
  }
  
  .ant-input-prefix {
    color: rgba(255, 255, 255, 0.7);
    margin-right: 10px;
  }
  
  .ant-input-suffix .anticon {
    color: rgba(255, 255, 255, 0.7);
  }
`

export const PrimaryButton = styled(Button)`
  height: 45px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  background: #e50914;
  border: none;
  box-shadow: 0 4px 12px rgba(229, 9, 20, 0.3);
  transition: all 0.3s ease;
  
  &:hover, &:focus {
    background: #f40612 !important;
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(229, 9, 20, 0.4);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  &.loading {
    opacity: 0.8;
  }
`

export const SecondaryButton = styled(Button)`
  height: 45px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  transition: all 0.3s ease;
  
  &:hover, &:focus {
    background: rgba(255, 255, 255, 0.1) !important;
    border-color: rgba(255, 255, 255, 0.5) !important;
    color: #ffffff !important;
    transform: translateY(-2px);
  }
  
  &:active {
    transform: translateY(0);
  }
`

export const SocialButton = styled(Button)`
  height: 45px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  
  .anticon {
    font-size: 18px;
  }
  
  &:hover {
    transform: translateY(-2px);
  }
  
  &:active {
    transform: translateY(0);
  }
`

export const GoogleButton = styled(SocialButton)`
  background: #ffffff;
  color: #3c4043;
  border: 1px solid #dadce0;

  &:hover,
  &:focus {
    background: #f7f8f8 !important;
    color: #3c4043 !important;
    border-color: #dadce0 !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

export const FacebookButton = styled(SocialButton)`
  background: #1877f2;
  color: #ffffff;
  border: 1px solid #1877f2;

  &:hover,
  &:focus {
    background: #166fe5 !important;
    color: #ffffff !important;
    border-color: #166fe5 !important;
    box-shadow: 0 4px 12px rgba(24, 119, 242, 0.3);
  }
`;

export const GithubButton = styled(SocialButton)`
  background: #24292e;
  color: #ffffff;
  border: 1px solid #24292e;

  &:hover,
  &:focus {
    background: #1b1f23 !important;
    color: #ffffff !important;
    border-color: #1b1f23 !important;
    box-shadow: 0 4px 12px rgba(36, 41, 46, 0.3);
  }
`;

export const DiscordButton = styled(SocialButton)`
  background: #5865f2;
  color: #ffffff;
  border: 1px solid #5865f2;

  &:hover,
  &:focus {
    background: #4752c4 !important;
    color: #ffffff !important;
    border-color: #4752c4 !important;
    box-shadow: 0 4px 12px rgba(88, 101, 242, 0.3);
  }
`;

export const XButton = styled(SocialButton)`
  background: #000000;
  color: #ffffff;
  border: 1px solid #000000;
`;

export const LinkText = styled.div`
  text-align: center;
  margin-top: 1.5rem;
  color: #d9d9d9;
  
  a {
    color: #1890ff;
    font-weight: 500;
    transition: all 0.3s ease;
    
    &:hover {
      color: #40a9ff;
      text-decoration: underline;
    }
  }
`

export const StyledSteps = styled(Steps)`
  margin-bottom: 2rem;
  
  .ant-steps-item-title {
    color: rgba(255, 255, 255, 0.7) !important;
  }
  
  .ant-steps-item-active .ant-steps-item-title {
    color: #ffffff !important;
  }
  
  .ant-steps-item-finish .ant-steps-item-icon {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
  }
  
  .ant-steps-item-active .ant-steps-item-icon {
    background-color: #1890ff !important;
    border-color: #1890ff !important;
    animation: ${pulse} 2s infinite;
  }
  
  .ant-steps-item-wait .ant-steps-item-icon {
    background-color: rgba(255, 255, 255, 0.2) !important;
    border-color: rgba(255, 255, 255, 0.3) !important;
  }
  
  .ant-steps-item-tail::after {
    background-color: rgba(255, 255, 255, 0.2) !important;
  }
  
  .ant-steps-item-finish .ant-steps-item-tail::after {
    background-color: #1890ff !important;
  }
`

export const OrDivider = styled.div`
  display: flex;
  align-items: center;
  margin: 1.5rem 0;
  
  &::before, &::after {
    content: "";
    flex: 1;
    height: 1px;
    background: rgba(255, 255, 255, 0.2);
  }
  
  span {
    padding: 0 1rem;
    color: rgba(255, 255, 255, 0.5);
    font-size: 14px;
  }
`
