"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { 
  Link, 
  useNavigate, 
  useLocation 
} from "react-router-dom"
import { 
  Form, 
  Alert, 
  Space, 
  message 
} from "antd"
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
import { 
  IconWrapper,
  ModalMessage, 
  ModalTitle, 
  ProgressBar, 
  StyledModal 
} from "../styles/LoginStyles"

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
                  style={{ width: "100%" }}
                  onClick={() => handleSocialLogin("Google")}
                  icon={<GoogleOutlined />}
                >
                  Google
                </GoogleButton>

                <FacebookButton
                  style={{ width: "100%" }}
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
