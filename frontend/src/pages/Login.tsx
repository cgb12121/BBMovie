// import React, { useState, useEffect } from 'react';
// import { useNavigate, useLocation } from 'react-router-dom';
// import { Form, Input, Button, message, Typography, Card, Space, Modal } from 'antd';
// import { 
//     UserOutlined, 
//     LockOutlined, 
//     MailOutlined, 
//     GoogleOutlined, 
//     FacebookOutlined, 
//     CloseOutlined, 
//     CheckCircleTwoTone, 
//     CloseCircleTwoTone 
// } from '@ant-design/icons';
// import styled, { keyframes } from 'styled-components';
// import Particles from '@tsparticles/react';

// import Alert from 'antd/es/alert/Alert';
// import api from '../services/api';

// const { Title, Text } = Typography;

// const popIn = keyframes`
//     0% {
//         transform: scale(0.3);
//         opacity: 0;
//     }
//     80% {
//         transform: scale(1.05);
//         opacity: 1;
//     }
//     100% {
//         transform: scale(1);
//     }
// `;

// const IconWrapper = styled.div`
//     animation: ${popIn} 0.5s ease-out;
//     font-size: 50px;
//     display: flex;
//     align-items: center;
//     justify-content: center;
// `;

// const fadeIn = keyframes`
//     from {
//         opacity: 0;
//         transform: translateY(20px);
//     }
//     to {
//         opacity: 1;
//         transform: translateY(0);
//     }
// `;

// const bounce = keyframes`
//     0%, 20%, 50%, 80%, 100% {
//         transform: translateY(0);
//     }
//     40% {
//         transform: translateY(-10px);
//     }
//     60% {
//         transform: translateY(-5px);
//     }
// `;

// const progressAnimation = keyframes`
//     0% {
//         width: 100%;
//         background-color: green;
//     }
//     50% {
//         background-color: orange;
//     }
//     100% {
//         width: 0%;
//         background-color: red;
//     }
// `;

// const LoginPageContainer = styled.div`
//     min-height: 100vh;
//     display: flex;
//     justify-content: center;
//     align-items: center;
//     background: url('https://images.unsplash.com/photo-1489599849927-2ee91cede3cf?q=80&w=2070&auto=format&fit=crop') no-repeat center center fixed;
//     background-size: cover;
//     padding: 1rem;
//     position: relative;

//     &:before {
//         content: '';
//         position: absolute;
//         top: 0;
//         left: 0;
//         right: 0;
//         bottom: 0;
//         background: rgba(0, 0, 0, 0.6);
//         z-index: 1;
//     }

//     > * {
//         position: relative;
//         z-index: 2;
//     }
// `;

// const StyledCard = styled(Card)`
//     max-width: 450px;
//     width: 100%;
//     background: rgba(255, 255, 255, 0.1);
//     backdrop-filter: blur(10px);
//     border: 1px solid rgba(255, 255, 255, 0.2);
//     border-radius: 12px;
//     box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
//     animation: ${fadeIn} 0.8s ease-out;
//     padding: 1rem;
// `;

// const StyledTitle = styled(Title)`
//     text-align: center;
//     color: #ffffff !important;
//     font-family: 'Poppins', sans-serif;
//     animation: ${bounce} 1s ease;
// `;

// const StyledInput = styled(Input)`
//     border-radius: 8px;
//     padding: 10px 12px;
//     background: rgb(240, 240, 240);
//     border: 1px solid rgba(0, 0, 0, 0.2);
//     color: #000000;
//     caret-color: black;
//     transition: all 0.3s ease;

//     &:hover,
//     &:focus {
//         border-color: rgb(0, 0, 0);
//         background: rgba(255, 255, 255, 0.3);
//     }

//     &::placeholder {
//         color: #444444;
//     }

//     .ant-input {
//         background-color: transparent;
//         color: #000000;
//     }

//     svg {
//         color: black !important;
//     }

//     &:-webkit-autofill {
//         box-shadow: 0 0 0px 1000px rgba(255, 255, 255, 0.3) inset !important;
//         -webkit-text-fill-color: #000000 !important;
//     }
// `;

// const StyledInputPassword = styled(Input.Password)`
//     border-radius: 8px;
//     padding: 10px 12px;
//     background: rgb(240, 240, 240);
//     border: 1px solid rgba(0, 0, 0, 0.2);
//     color: #000000;
//     caret-color: black;
//     transition: all 0.3s ease;

//     &:hover,
//     &:focus {
//         border-color: rgb(0, 0, 0);
//         background: rgba(255, 255, 255, 0.3);
//     }

//     &::placeholder {
//         color: #444444;
//     }

//     .ant-input {
//         background-color: transparent;
//         color: #000000;
//     }

//     svg {
//         color: black !important;
//     }

//     &:-webkit-autofill {
//         box-shadow: 0 0 0px 1000px rgba(255, 255, 255, 0.3) inset !important;
//         -webkit-text-fill-color: #000000 !important;
//     }
// `;

// const StyledButton = styled(Button)`
//     border-radius: 8px;
//     padding: 10px;
//     font-weight: 600;
//     transition: all 0.3s ease;
//     background: #1890ff;
//     border: none;

//     &:hover {
//         background: #40a9ff;
//         transform: translateY(-2px);
//         box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
//     }
// `;

// const LinksContainer = styled(Space)`
//     a {
//         color: #40a9ff;
//         transition: color 0.3s ease;

//         &:hover {
//             color: #69b1ff;
//         }
//     }
// `;

// const StyledModal = styled(Modal)`
//     .ant-modal-content {
//         background: rgba(255, 255, 255, 0.1);
//         backdrop-filter: blur(10px);
//         border: 1px solid rgba(255, 255, 255, 0.2);
//         border-radius: 12px;
//         box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
//         color:rgba(255, 255, 255, 0.1);
//         text-align: center;
//     }

//     .ant-modal-body {
//         padding: 16px;
//         display: flex;
//         flex-direction: column;
//         align-items: center;
//         gap: 12px;
//     }

//     .ant-modal-close-x {
//         color: #d9d9d9;
//         transition: color 0.3s ease;

//         &:hover {
//             color: #ffffff;
//         }
//     }
// `;

// const ProgressBar = styled.div<{ status: string }>`
//     width: 100%;
//     height: 4px;
//     background: rgba(255, 255, 255, 0.2);
//     border-radius: 2px;
//     overflow: hidden;
//     position: relative;

//     &::after {
//         content: '';
//         position: absolute;
//         left: 0;
//         top: 0;
//         height: 100%;
//         width: 100%;
//         background: ${({ status }) => (status === 'success' ? '#52c41a' : '#ff4d4f')};
//         animation: ${progressAnimation} 5s linear forwards;
//     }
// `;

// interface LoginFormData {
//     email: string;
//     password: string;
// }

// const Login: React.FC = () => {
//     const [form] = Form.useForm();
//     const navigate = useNavigate();
//     const location = useLocation();
//     const [loading, setLoading] = useState(false);
//     const [error, setError] = useState<string | null>(null);
//     const [isEmail, setIsEmail] = useState(false);
//     const [notificationVisible, setNotificationVisible] = useState(false);

//     const query = new URLSearchParams(location.search);
//     const status = query.get('status');
//     const messageFromQuery = query.get('message');

//     useEffect(() => {
//         if (status) {
//             setNotificationVisible(true);
//             const timer = setTimeout(() => {
//                 setNotificationVisible(false);
//                 navigate('/login', { replace: true });
//             }, 5000);

//             return () => clearTimeout(timer);
//         }
//     }, [status, navigate]);

//     const onFinish = async (values: LoginFormData) => {
//         try {
//             setLoading(true);
//             setError(null);
            
//             const response = await api.post('/api/auth/login', {
//                 email: values.email,
//                 password: values.password,
//             }, {
//                 headers: {
//                   'Content-Type': 'application/json'
//                 }
//             });

//             localStorage.setItem('user', JSON.stringify(response.data));
//             message.success('Login successful!');
//             navigate('/');
//         } catch (error: any) {
//             console.log(error)
//         } finally {
//             setLoading(false);
//         }
//     };

//     const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
//         const value = e.target.value;
//         setIsEmail(value.includes('@'));
//     };

//     return (
//         <LoginPageContainer>
//             <Particles
//                 id="tsparticles"
//                 options={{
//                     background: { color: { value: "transparent" } },
//                     particles: {
//                         number: {
//                             value: 50,
//                             density: {
//                                 enable: true
//                             },
//                         },
//                         color: { value: "#ffffff" },
//                         opacity: { value: 0.5 },
//                         size: { value: 3 },
//                         move: { enable: true, speed: 2 },
//                     },
//                 }}
//                 style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }}
//             />
//             <StyledCard>
//                 <Space direction="vertical" size="large" style={{ width: '100%' }}>
//                     <div style={{ textAlign: 'center' }}>
//                         <h1 style={{ color: '#e50914', fontSize: '2rem', marginBottom: 0, fontFamily: 'Poppins, sans-serif' }}>
//                             BBMovie
//                         </h1>
//                     </div>
//                     <StyledTitle level={2}>Login</StyledTitle>

//                     {error && (
//                         <Alert
//                             message="Login Failed"
//                             description={error}
//                             type="error"
//                             showIcon
//                             style={{ marginBottom: 24 }}
//                         />
//                     )}

//                     <Form
//                         form={form}
//                         name="login"
//                         onFinish={onFinish}
//                         layout="vertical"
//                         size="large"
//                     >
//                         <Form.Item
//                             name="email"
//                             rules={[
//                                 { required: true, message: 'Please input your email!' },
//                                 { type: 'email', message: 'Invalid email format!' }
//                             ]}
//                         >
//                             <StyledInput
//                                 prefix={isEmail ? <MailOutlined /> : <UserOutlined />}
//                                 placeholder="Email"
//                                 autoComplete="email"
//                                 onChange={handleInputChange}
//                             />
//                         </Form.Item>

//                         <Form.Item
//                             name="password"
//                             rules={[
//                                 { required: true, message: 'Please input your password!' },
//                             ]}
//                         >
//                             <StyledInputPassword
//                                 prefix={<LockOutlined />}
//                                 placeholder="Password"
//                                 autoComplete="current-password"
//                             />
//                         </Form.Item>

//                         <Form.Item>
//                             <StyledButton type="primary" htmlType="submit" loading={loading} block>
//                                 Login
//                             </StyledButton>
//                         </Form.Item>

//                         <div style={{ display: 'flex', gap: '16px' }}>
//                             <Form.Item style={{ flex: 1, marginBottom: 0 }}>
//                                 <Button
//                                     type="default"
//                                     block
//                                     icon={<GoogleOutlined style={{ fontSize: 18 }} />}
//                                     style={{
//                                         borderRadius: 8,
//                                         background: '#ffffff',
//                                         color: '#3c4043',
//                                         border: '1px solid #dadce0',
//                                         fontWeight: 500,
//                                         boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
//                                         display: 'flex',
//                                         alignItems: 'center',
//                                         justifyContent: 'center',
//                                         gap: 8,
//                                     }}
//                                     onClick={() => message.info('Google login coming soon!')}
//                                     onMouseEnter={(e) => (e.currentTarget.style.background = '#f7f8f8')}
//                                     onMouseLeave={(e) => (e.currentTarget.style.background = '#ffffff')}
//                                 >
//                                     Google
//                                 </Button>
//                             </Form.Item>

//                             <Form.Item style={{ flex: 1, marginBottom: 0 }}>
//                                 <Button
//                                     type="default"
//                                     block
//                                     icon={<FacebookOutlined style={{ fontSize: 18 }} />}
//                                     style={{
//                                         borderRadius: 8,
//                                         background: '#1877f2',
//                                         color: '#ffffff',
//                                         border: '1px solid #1877f2',
//                                         fontWeight: 500,
//                                         boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
//                                         display: 'flex',
//                                         alignItems: 'center',
//                                         justifyContent: 'center',
//                                         gap: 8,
//                                     }}
//                                     onClick={() => message.info('Facebook login coming soon!')}
//                                     onMouseEnter={(e) => (e.currentTarget.style.background = '#166fe5')}
//                                     onMouseLeave={(e) => (e.currentTarget.style.background = '#1877f2')}
//                                 >
//                                     Facebook
//                                 </Button>
//                             </Form.Item>
//                         </div>

//                         <Form.Item style={{ textAlign: 'center', marginTop: 10 }}>
//                             <LinksContainer direction="vertical" size="small">
//                                 <Text style={{ color: '#d9d9d9' }}>
//                                     Don't have an account?{' '}
//                                     <a href="/register">Register</a>
//                                 </Text>
//                                 <Text style={{ color: '#d9d9d9' }}>
//                                     Forgot your password?{' '}
//                                     <a href="/forgot-password">Reset Password</a>
//                                 </Text>
//                             </LinksContainer>
//                         </Form.Item>
//                     </Form>
//                 </Space>
//             </StyledCard>

//             <StyledModal
//                 open={notificationVisible && !!status}
//                 onCancel={() => setNotificationVisible(false)}
//                 footer={null}
//                 width={350}
//                 centered
//                 closeIcon={<CloseOutlined />}
//             >
//                 <Space
//                     direction="vertical"
//                     size="middle"
//                     style={{
//                         width: '100%',
//                         alignItems: 'center',
//                         textAlign: 'center',
//                     }}
//                 >
//                     <IconWrapper>
//                         {status === 'success' ? (
//                             <CheckCircleTwoTone twoToneColor="#52c41a" />
//                         ) : (
//                             <CloseCircleTwoTone twoToneColor="#ff4d4f" />
//                         )}
//                     </IconWrapper>

//                     <Text style={{ color: '#ffffff', fontSize: 18, fontWeight: 600 }}>
//                         {status === 'success' ? 'Success' : 'Error'}
//                     </Text>

//                     <Text style={{ color: '#d9d9d9', fontSize: 14 }}>
//                         {messageFromQuery ?? 'Unknown error'}
//                     </Text>
//                 </Space>
                
//                 {notificationVisible && !!status && (
//                     <div style={{ position: 'absolute', bottom: 0, left: 0, width: '100%' }}>
//                         <ProgressBar status={status ?? ''} />
//                     </div>
//                 )}
//             </StyledModal>
//         </LoginPageContainer>
//     );
// };

// export default Login;

"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { Link, useNavigate, useLocation } from "react-router-dom"
import { Form, Alert, Space, Modal, message } from "antd"
import {
  LockOutlined,
  MailOutlined,
  GoogleOutlined,
  FacebookOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
} from "@ant-design/icons"
import { motion, AnimatePresence } from "framer-motion"
import api from "../services/api"
import AuthLayout from "../styles/AuthLayout"
import {
  StyledCard,
  StyledInput,
  StyledPassword,
  PrimaryButton,
  GoogleButton,
  FacebookButton,
  LinkText,
  OrDivider,
} from "../styles/AuthStyles"
import { IconWrapper, ModalMessage, ModalTitle, ProgressBar, StyledModal } from "../styles/LoginStyles"


interface LoginFormData {
  email: string
  password: string
}

const Login: React.FC = () => {
  const [form] = Form.useForm<LoginFormData>()
  const navigate = useNavigate()
  const location = useLocation()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notificationVisible, setNotificationVisible] = useState(false)

  const query = new URLSearchParams(location.search)
  const status = query.get("status")
  const messageFromQuery = query.get("message")

  useEffect(() => {
    if (status) {
      setNotificationVisible(true)
      const timer = setTimeout(() => {
        setNotificationVisible(false)
        navigate("/login", { replace: true })
      }, 5000)

      return () => clearTimeout(timer)
    }
  }, [status, navigate])

  const onFinish = async (values: LoginFormData) => {
    try {
      setLoading(true)
      setError(null)

      const response = await api.post("/api/auth/login", {
        email: values.email,
        password: values.password,
      })

      localStorage.setItem("user", JSON.stringify(response.data))
      message.success("Login successful!")
      navigate("/")
    } catch (error: any) {
      console.error("Login error:", error)
      setError(error.response?.data?.message ?? "Login failed. Please check your credentials.")
    } finally {
      setLoading(false)
    }
  }

  const handleSocialLogin = (provider: string) => {
    message.info(`${provider} login coming soon!`)
  }

  const formVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.5 },
    },
    exit: {
      opacity: 0,
      y: -20,
      transition: { duration: 0.3 },
    },
  }

  return (
    <AuthLayout title="Welcome Back" subtitle="Sign in to continue to BBMovie">
      <StyledCard>
        <AnimatePresence mode="wait">
          <motion.div key="login-form" variants={formVariants} initial="hidden" animate="visible" exit="exit">
            {error && (
              <Alert
                message="Login Failed"
                description={error}
                type="error"
                showIcon
                style={{ marginBottom: 24 }}
                closable
                onClose={() => setError(null)}
              />
            )}

            <Form
              form={form}
              name="login"
              onFinish={onFinish}
              layout="vertical"
              size="large"
            >
              <Form.Item
                name="email"
                rules={[
                  { required: true, message: "Please enter your email" },
                  { type: "email", message: "Please enter a valid email" },
                ]}
              >
                <StyledInput prefix={<MailOutlined />} placeholder="Email" autoComplete="email" />
              </Form.Item>

              <Form.Item name="password" rules={[{ required: true, message: "Please enter your password" }]}>
                <StyledPassword prefix={<LockOutlined />} placeholder="Password" autoComplete="current-password" />
              </Form.Item>

              <Form.Item>
                <PrimaryButton
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  block
                  className={loading ? "loading" : ""}
                >
                  Sign In
                </PrimaryButton>
              </Form.Item>

              <OrDivider>
                <span>or continue with</span>
              </OrDivider>

              <Space direction="horizontal" style={{ width: "100%", justifyContent: "space-between" }}>
                <GoogleButton
                  style={{ width: "48%" }}
                  onClick={() => handleSocialLogin("Google")}
                  icon={<GoogleOutlined />}
                >
                  Google
                </GoogleButton>

                <FacebookButton
                  style={{ width: "48%" }}
                  onClick={() => handleSocialLogin("Facebook")}
                  icon={<FacebookOutlined />}
                >
                  Facebook
                </FacebookButton>
              </Space>
            </Form>

            <LinkText>
              <p>
                Forgot your password? <Link to="/forgot-password">Reset it</Link>
              </p>
              <p style={{ marginTop: "8px" }}>
                Don't have an account? <Link to="/register">Sign up</Link>
              </p>
            </LinkText>
          </motion.div>
        </AnimatePresence>
      </StyledCard>

      {/* Notification Modal */}
      <StyledModal
        open={notificationVisible && !!status}
        onCancel={() => setNotificationVisible(false)}
        footer={null}
        width={400}
        centered
        closable
      >
        <IconWrapper>
          {status === "success" ? (
            <CheckCircleFilled style={{ color: "#52c41a" }} />
          ) : (
            <CloseCircleFilled style={{ color: "#ff4d4f" }} />
          )}
        </IconWrapper>

        <ModalTitle>{status === "success" ? "Success" : "Error"}</ModalTitle>

        <ModalMessage>
          {messageFromQuery ?? (status === "success" ? "Operation completed successfully." : "An error occurred.")}
        </ModalMessage>

        <PrimaryButton type="primary" block onClick={() => setNotificationVisible(false)}>
          {status === "success" ? "Continue" : "Try Again"}
        </PrimaryButton>

        {notificationVisible && <ProgressBar status={status ?? ""} />}
      </StyledModal>
    </AuthLayout>
  )
}

export default Login;
