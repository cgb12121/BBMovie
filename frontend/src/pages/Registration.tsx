// import React, { useState, useEffect } from 'react';
// import { useNavigate } from 'react-router-dom';
// import { Form, Input, Button, Typography, Steps, Space, message, Card } from 'antd';
// import { UserOutlined, MailOutlined, LockOutlined } from '@ant-design/icons';
// import styled, { keyframes } from 'styled-components';
// import Particles from '@tsparticles/react';
// import api from '../services/api';

// const { Title, Text } = Typography;
// const { Step } = Steps;

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

// const pulse = keyframes`
//     0% {
//         box-shadow: 0 0 0 0 rgba(240, 240, 240, 0.7);
//     }
//     70% {
//         box-shadow: 0 0 0 10px rgba(24, 144, 255, 0);
//     }
//     100% {
//         box-shadow: 0 0 0 0 rgba(24, 144, 255, 0);
//     }
// `;

// const RegisterPageContainer = styled.div`
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

// const StyledText = styled(Text)`
//     display: block;
//     text-align: center;
//     color: #d9d9d9 !important;
//     margin-bottom: 24px;
// `;

// const StyledSteps = styled(Steps)`
//     background: rgba(255, 255, 255, 0.05);
//     padding: 8px 16px;
//     border-radius: 8px;
//     margin-bottom: 24px;
//     display: flex;
//     justify-content: center;
//     align-items: center;

//     .ant-steps-item {
//         flex: none !important;
//     }

//     .ant-steps-item-title {
//         color: #ffffff !important;
//         font-size: 16px !important;
//         font-weight: 500 !important;
//         display: inline !important;
//     }

//     .ant-steps-item-active .ant-steps-item-title {
//         color: #40a9ff !important;
//     }

//     .ant-steps-item-icon {
//         background: rgba(240, 240, 240, 1) !important;
//         border-color: rgba(255, 255, 255, 0.4) !important;
//         color: #ffffff !important;
//         font-size: 14px !important;
//         width: 24px !important;
//         height: 24px !important;
//         line-height: 24px !important;
//         margin-right: 8px !important;
//     }

//     .ant-steps-item-active .ant-steps-item-icon {
//         background: #40a9ff !important;
//         border-color: #40a9ff !important;
//         animation: ${pulse} 2s infinite;
//     }

//     .ant-steps-item-finish .ant-steps-item-icon {
//         background: #52c41a !important;
//         border-color: #52c41a !important;
//     }

//     /* Hide titles for non-active steps */
//     .ant-steps-item:not(.ant-steps-item-active) .ant-steps-item-title {
//         display: none !important;
//     }
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

// const StyledPassword = styled(Input.Password)`
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
//     padding: 10px 12px; 
//     font-weight: 600;
//     transition: all 0.3s ease;
//     background: #1890ff;
//     border: none;
//     height: 40px; 
//     line-height: 1;

//     &:hover {
//         background: #40a9ff;
//         transform: translateY(-2px);
//         box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
//     }
// `;

// const StyledBackButton = styled(Button)`
//     border-radius: 8px;
//     padding: 10px 12px;
//     font-weight: 600;
//     transition: all 0.3s ease;
//     background: transparent;
//     border: 1px solid rgba(255, 255, 255, 0.3);
//     color: #d9d9d9;
//     height: 40px;
//     line-height: 1;

//     &:hover {
//         background: rgba(255, 255, 255, 0.1);
//         color: #ffffff;
//         transform: translateY(-2px);
//         box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
//     }
// `;

// const Registration: React.FC = () => {
//     const [form] = Form.useForm();
//     const navigate = useNavigate();
//     const [step, setStep] = useState(0);
//     const [formData, setFormData] = useState({
//         email: '',
//         password: '',
//         confirmPassword: '',
//         username: '',
//         firstName: '',
//         lastName: ''
//     });
//     const [loading, setLoading] = useState(false);

//     useEffect(() => {
//         const saved = localStorage.getItem('registrationData');
//         if (saved) setFormData(JSON.parse(saved));
//     }, []);

//     useEffect(() => {
//         localStorage.setItem('registrationData', JSON.stringify(formData));
//     }, [formData]);

//     const handleNext = () => {
//         form.validateFields().then(() => {
//             setStep(prev => prev + 1);
//         });
//     };

//     const handleFinish = async () => {
//         try {
//             setLoading(true);
//             const { username, email, password, firstName, lastName } = formData;
//             await api.post('/api/auth/register', {
//                 email,
//                 username,
//                 password,
//                 firstName,
//                 lastName
//             });
//             message.success('Registration successful!');
//             localStorage.removeItem('registrationData');
//             navigate('/login?status=success&message=Registration successful!');
//         } catch (err: any) {
//             const errorMessage = err?.response?.data?.message || 'Registration failed.';
//             navigate(`/login?status=error&message=${encodeURIComponent(errorMessage)}`);
//         } finally {
//             setLoading(false);
//         }
//     };

//     const steps = [
//         {
//             title: 'Email',
//             content: (
//                 <Form.Item
//                     name="email"
//                     rules={[
//                         { required: true, message: 'Email is required' },
//                         { type: 'email', message: 'Invalid email' }
//                     ]}
//                     initialValue={formData.email}
//                 >
//                     <StyledInput
//                         prefix={<MailOutlined />}
//                         placeholder="Email"
//                         value={formData.email}
//                         onChange={e => setFormData({ ...formData, email: e.target.value })}
//                     />
//                 </Form.Item>
//             )
//         },
//         {
//             title: 'Password',
//             content: (
//                 <>
//                     <Form.Item
//                         name="password"
//                         rules={[
//                             { required: true, message: 'Password is required' },
//                             { min: 8, message: 'At least 8 characters' }
//                         ]}
//                         initialValue={formData.password}
//                     >
//                         <StyledPassword
//                             prefix={<LockOutlined />}
//                             placeholder="Password"
//                             onChange={e => setFormData({ ...formData, password: e.target.value })}
//                         />
//                     </Form.Item>
//                     <Form.Item
//                         name="confirmPassword"
//                         dependencies={['password']}
//                         rules={[
//                             { required: true, message: 'Please confirm your password' },
//                             ({ getFieldValue }) => ({
//                                 validator(_, value) {
//                                     if (!value || value === getFieldValue('password')) {
//                                         return Promise.resolve();
//                                     }
//                                     return Promise.reject(new Error('Passwords do not match!'));
//                                 }
//                             })
//                         ]}
//                         initialValue={formData.confirmPassword}
//                     >
//                         <StyledPassword
//                             prefix={<LockOutlined />}
//                             placeholder="Confirm Password"
//                             onChange={e => setFormData({ ...formData, confirmPassword: e.target.value })}
//                         />
//                     </Form.Item>
//                 </>
//             )
//         },
//         {
//             title: 'Personal Info',
//             content: (
//                 <>
//                     <Form.Item name="username" rules={[{ required: true }]} initialValue={formData.username}>
//                         <StyledInput
//                             prefix={<UserOutlined />}
//                             placeholder="Username"
//                             onChange={e => setFormData({ ...formData, username: e.target.value })}
//                         />
//                     </Form.Item>
//                     <Form.Item name="firstName" rules={[{ required: true }]} initialValue={formData.firstName}>
//                         <StyledInput placeholder="First Name" onChange={e => setFormData({ ...formData, firstName: e.target.value })} />
//                     </Form.Item>
//                     <Form.Item name="lastName" rules={[{ required: true }]} initialValue={formData.lastName}>
//                         <StyledInput placeholder="Last Name" onChange={e => setFormData({ ...formData, lastName: e.target.value })} />
//                     </Form.Item>
//                 </>
//             )
//         }
//     ];

//     return (
//         <RegisterPageContainer>
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
//                     <StyledTitle level={2}>Register</StyledTitle>
//                     <StyledText>Create your BBMovie account</StyledText>

//                     <StyledSteps current={step}>
//                         {steps.map((s, index) => (
//                             <Step
//                                 key={s.title}
//                                 title={s.title}
//                                 className={index === step ? 'ant-steps-item-active' : ''}
//                             />
//                         ))}
//                     </StyledSteps>

//                     <Form form={form} layout="vertical" onFinish={handleFinish}>
//                         {steps[step].content}

//                         <Form.Item>
//                             <Space style={{ width: '100%' }} direction="vertical">
//                                 {step < steps.length - 1 ? (
//                                     <StyledButton type="primary" onClick={handleNext} block>
//                                         Next
//                                     </StyledButton>
//                                 ) : (
//                                     <StyledButton type="primary" htmlType="submit" loading={loading} block>
//                                         Register
//                                     </StyledButton>
//                                 )}
//                                 {step > 0 && (
//                                     <StyledBackButton onClick={() => setStep(step - 1)} block>
//                                         Back
//                                     </StyledBackButton>
//                                 )}
//                             </Space>
//                         </Form.Item>
//                     </Form>
//                 </Space>
//             </StyledCard>
//         </RegisterPageContainer>
//     );
// };

// export default Registration;


"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { Link, useNavigate } from "react-router-dom"
import { Form, Steps, Space, message } from "antd"
import {
  UserOutlined,
  MailOutlined,
  LockOutlined,
  IdcardOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
} from "@ant-design/icons"
import { motion, AnimatePresence } from "framer-motion"
import styled from "styled-components"
import api from "../services/api"
import AuthLayout from "../styles/AuthLayout"
import {
  StyledCard,
  StyledForm,
  StyledInput,
  StyledPassword,
  PrimaryButton,
  SecondaryButton,
  StyledSteps,
} from "../styles/AuthStyles"

const { Step } = Steps

const FormContainer = styled.div`
  min-height: 320px;
  display: flex;
  flex-direction: column;
`

const ButtonsContainer = styled.div`
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 24px;
`

interface RegistrationData {
  email: string
  password: string
  confirmPassword: string
  username: string
  firstName: string
  lastName: string
}

const Registration: React.FC = () => {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [currentStep, setCurrentStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState<RegistrationData>({
    email: "",
    password: "",
    confirmPassword: "",
    username: "",
    firstName: "",
    lastName: "",
  })

  // Load saved form data from localStorage
  useEffect(() => {
    const savedData = localStorage.getItem("registrationData")
    if (savedData) {
      const parsedData = JSON.parse(savedData)
      setFormData(parsedData)
      form.setFieldsValue(parsedData)
    }
  }, [form])

  // Save form data to localStorage when it changes
  useEffect(() => {
    localStorage.setItem("registrationData", JSON.stringify(formData))
  }, [formData])

  const updateFormData = (values: Partial<RegistrationData>) => {
    setFormData((prev) => ({ ...prev, ...values }))
  }

  const handleNext = async () => {
    try {
      // Validate current step fields
      const values = await form.validateFields()
      updateFormData(values)
      setCurrentStep((prev) => prev + 1)
    } catch (error) {
      // Form validation failed
      console.error("Validation failed:", error)
    }
  }

  const handleBack = () => {
    setCurrentStep((prev) => prev - 1)
  }

  const handleSubmit = async () => {
    try {
      // Validate final step fields
      const values = await form.validateFields()
      updateFormData(values)

      setLoading(true)

      // Submit registration
      await api.post("/api/auth/register", {
        email: formData.email,
        username: formData.username,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
      })

      // Clear saved form data
      localStorage.removeItem("registrationData")

      // Show success message and redirect
      message.success("Registration successful! Please check your email to verify your account.")
      navigate("/login?status=success&message=Registration successful! Please check your email to verify your account.")
    } catch (error: any) {
      console.error("Registration error:", error)
      message.error(error.response?.data?.message || "Registration failed. Please try again.")
    } finally {
      setLoading(false)
    }
  }

  // Animation variants
  const formVariants = {
    hidden: { opacity: 0, x: 50 },
    visible: {
      opacity: 1,
      x: 0,
      transition: { duration: 0.4 },
    },
    exit: {
      opacity: 0,
      x: -50,
      transition: { duration: 0.3 },
    },
  }

  // Step content
  const steps = [
    {
      title: "Account",
      content: (
        <Form.Item
          name="email"
          rules={[
            { required: true, message: "Please enter your email" },
            { type: "email", message: "Please enter a valid email" },
          ]}
          initialValue={formData.email}
        >
          <StyledInput
            prefix={<MailOutlined />}
            placeholder="Email"
            onChange={(e) => updateFormData({ email: e.target.value })}
          />
        </Form.Item>
      ),
    },
    {
      title: "Security",
      content: (
        <>
          <Form.Item
            name="password"
            rules={[
              { required: true, message: "Please enter a password" },
              { min: 8, message: "Password must be at least 8 characters" },
            ]}
            initialValue={formData.password}
          >
            <StyledPassword
              prefix={<LockOutlined />}
              placeholder="Password"
              onChange={(e) => updateFormData({ password: e.target.value })}
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={["password"]}
            rules={[
              { required: true, message: "Please confirm your password" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("password") === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error("The two passwords do not match"))
                },
              }),
            ]}
            initialValue={formData.confirmPassword}
          >
            <StyledPassword
              prefix={<LockOutlined />}
              placeholder="Confirm Password"
              onChange={(e) => updateFormData({ confirmPassword: e.target.value })}
            />
          </Form.Item>
        </>
      ),
    },
    {
      title: "Profile",
      content: (
        <>
          <Form.Item
            name="username"
            rules={[{ required: true, message: "Please enter a username" }]}
            initialValue={formData.username}
          >
            <StyledInput
              prefix={<UserOutlined />}
              placeholder="Username"
              onChange={(e) => updateFormData({ username: e.target.value })}
            />
          </Form.Item>

          <Form.Item
            name="firstName"
            rules={[{ required: true, message: "Please enter your first name" }]}
            initialValue={formData.firstName}
          >
            <StyledInput
              prefix={<IdcardOutlined />}
              placeholder="First Name"
              onChange={(e) => updateFormData({ firstName: e.target.value })}
            />
          </Form.Item>

          <Form.Item
            name="lastName"
            rules={[{ required: true, message: "Please enter your last name" }]}
            initialValue={formData.lastName}
          >
            <StyledInput
              prefix={<IdcardOutlined />}
              placeholder="Last Name"
              onChange={(e) => updateFormData({ lastName: e.target.value })}
            />
          </Form.Item>
        </>
      ),
    },
  ]

  return (
    <AuthLayout title="Create Account" subtitle="Join BBMovie and start exploring">
      <StyledCard>
        <StyledSteps current={currentStep} size="small">
          {steps.map((step) => (
            <Step key={step.title} title={step.title} />
          ))}
        </StyledSteps>

        <StyledForm form={form} layout="vertical" size="large">
          <FormContainer>
            <AnimatePresence mode="wait">
              <motion.div
                key={`step-${currentStep}`}
                variants={formVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                style={{ flex: 1 }}
              >
                {steps[currentStep].content}
              </motion.div>
            </AnimatePresence>

            <ButtonsContainer>
              {currentStep > 0 && (
                <SecondaryButton
                  onClick={handleBack}
                  icon={<ArrowLeftOutlined />}
                  style={{ width: currentStep === steps.length - 1 ? "48%" : "100px" }}
                >
                  Back
                </SecondaryButton>
              )}

              {currentStep < steps.length - 1 ? (
                <PrimaryButton
                  type="primary"
                  onClick={handleNext}
                  style={{ marginLeft: "auto", width: currentStep === 0 ? "100%" : "48%" }}
                >
                  Next <ArrowRightOutlined />
                </PrimaryButton>
              ) : (
                <PrimaryButton
                  type="primary"
                  onClick={handleSubmit}
                  loading={loading}
                  icon={<CheckOutlined />}
                  style={{ width: "48%" }}
                >
                  Register
                </PrimaryButton>
              )}
            </ButtonsContainer>
          </FormContainer>
        </StyledForm>

        <Space direction="vertical" size="small" style={{ width: "100%", marginTop: "24px" }}>
          <Link to="/login" style={{ display: "block", textAlign: "center", color: "rgba(255, 255, 255, 0.7)" }}>
            Already have an account? Sign in
          </Link>
        </Space>
      </StyledCard>
    </AuthLayout>
  )
}

export default Registration;
