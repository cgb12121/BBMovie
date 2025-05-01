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
  Checkbox,
  Row,
  Col
} from "antd"
import { FcGoogle } from "react-icons/fc"
import { FaFacebookF, FaGithub } from "react-icons/fa"
import {
  LockOutlined,
  MailOutlined,
  CheckCircleFilled,
  CloseCircleFilled} from "@ant-design/icons"
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
  GithubButton,
  LinkText,
  OrDivider
} from "../styles/AuthStyles"
import {
  IconWrapper,
  ModalMessage,
  ModalTitle,
  ProgressBar,
  StyledModal
} from "../styles/LoginStyles"
import { useDispatch } from "react-redux"
import { setCredentials } from "../redux/authSlice"

interface LoginFormData {
  email: string
  password: string
  // remember?: boolean
}

const OAUTH_BASE_URL = "http://localhost:8080/oauth2/authorization"

const Login: React.FC = () => {
  const [form] = Form.useForm<LoginFormData>();
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const location = useLocation()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notificationVisible, setNotificationVisible] = useState(false)
  const [socialLoading, setSocialLoading] = useState<string | null>(null)

  const query = new URLSearchParams(location.search)
  const status = query.get("status")
  const messageFromQuery = query.get("message")

  // Redirect if already logged in
  useEffect(() => {
    const isLoggedIn = !!localStorage.getItem("user")
    if (isLoggedIn) navigate("/")
  }, [navigate])

  // Show success/error modal from URL query
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
    setLoading(true)
    setError(null)
  
    try {
      const { data } = await api.post("/api/auth/login", values)
      onLoginSuccess(data.data)
    } catch (err: any) {
      const msg = err?.response?.data?.message || "Login failed. Please check your credentials."
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const onLoginSuccess = (userData: any) => {
    const { userResponse, authResponse } = userData

    // Save to Redux
    dispatch(setCredentials({ user: userResponse, auth: authResponse }))

    // Save to localStorage (optional)
    localStorage.setItem("user", JSON.stringify(userResponse))
    localStorage.setItem("auth", JSON.stringify(authResponse))

    navigate("/")
  }

  const handleSocialLogin = (provider: string) => {
    try {
      setSocialLoading(provider)
      window.location.href = `${OAUTH_BASE_URL}/${provider.toLowerCase()}`
    } catch (err) {
      console.error(`${provider} login error:`, err)
      setError(`Failed to initiate ${provider} login. Please try again.`)
      setSocialLoading(null)
    }
  }

  const formVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5 } },
    exit: { opacity: 0, y: -20, transition: { duration: 0.3 } }
  }


  const googleIcon = FcGoogle({ size: 20 }) as JSX.Element;
  const facebookIcon = FaFacebookF({ size: 20 }) as JSX.Element;
  const githubIcon = FaGithub({ size: 20 }) as JSX.Element;

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
                closable
                style={{ marginBottom: 24 }}
                onClose={() => setError(null)}
              />
            )}

            <Form
              form={form}
              name="login"
              onFinish={onFinish}
              layout="vertical"
              size="large"
              // initialValues={{ remember: false }}
            >
              <Form.Item
                name="email"
                rules={[
                  { required: true, message: "Please enter your email" },
                  { type: "email", message: "Please enter a valid email" }
                ]}
              >
                <StyledInput prefix={<MailOutlined />} placeholder="Email" autoComplete="email" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[{ required: true, message: "Please enter your password" }]}
              >
                <StyledPassword prefix={<LockOutlined />} placeholder="Password" autoComplete="current-password" />
              </Form.Item>

              {/* 
              <Form.Item name="remember" valuePropName="checked">
                <Checkbox>Remember me</Checkbox>
              </Form.Item> 
              */}

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

              <Row gutter={16}>
                <Col span={8}>
                  <GoogleButton
                    onClick={() => handleSocialLogin("Google")}
                    icon={ googleIcon }
                    loading={socialLoading === "Google"}
                    disabled={!!socialLoading}
                    block
                  >
                    Google
                  </GoogleButton>
                </Col>
                <Col span={8}>
                  <FacebookButton
                    onClick={() => handleSocialLogin("Facebook")}
                    icon={ facebookIcon }
                    loading={socialLoading === "Facebook"}
                    disabled={!!socialLoading}
                    block
                  >
                    Facebook
                  </FacebookButton>
                </Col>
                <Col span={8}>
                  <GithubButton
                    onClick={() => handleSocialLogin("Github")}
                    icon={ githubIcon }
                    loading={socialLoading === "Github"}
                    disabled={!!socialLoading}
                    block
                  >
                    GitHub
                  </GithubButton>
                </Col>
              </Row>
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
          {messageFromQuery ?? (status === "success"
            ? "Operation completed successfully."
            : "An error occurred.")}
        </ModalMessage>

        <PrimaryButton type="primary" block onClick={() => setNotificationVisible(false)}>
          {status === "success" ? "Continue" : "Try Again"}
        </PrimaryButton>

        {notificationVisible && <ProgressBar status={status ?? ""} />}
      </StyledModal>
    </AuthLayout>
  )
}

export default Login
