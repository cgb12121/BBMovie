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
import { ButtonsContainer, FormContainer } from "../styles/RegisterStyles"

const { Step } = Steps

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
  const [submitting, setSubmitting] = useState(false)

  const [formData, setFormData] = useState<RegistrationData>({
    email: "",
    password: "",
    confirmPassword: "",
    username: "",
    firstName: "",
    lastName: "",
  })

  useEffect(() => {
    const saved = localStorage.getItem("registrationData")
    if (saved) {
      const parsed = JSON.parse(saved)
      setFormData(parsed)
      form.setFieldsValue(parsed)
    }
  }, [form])

  useEffect(() => {
    localStorage.setItem("registrationData", JSON.stringify(formData))
  }, [formData])

  const updateFormData = (values: Partial<RegistrationData>) => {
    setFormData((prev) => ({ ...prev, ...values }))
  }

  const handleNext = async () => {
    try {
      const values = await form.validateFields()
      updateFormData(values)
      setCurrentStep((prev) => prev + 1)
    } catch (err) {
      console.error("Validation failed:", err)
    }
  }

  const handleBack = () => setCurrentStep((prev) => prev - 1)

  const handleSubmit = async () => {
    if (submitting) return
    try {
      setSubmitting(true)
      const values = await form.validateFields()
      updateFormData(values)
      setLoading(true)

      await api.post("/api/auth/register", {
        email: formData.email,
        username: formData.username,
        password: formData.password,
        confirmPassword: formData.confirmPassword,
        firstName: formData.firstName,
        lastName: formData.lastName,
      })

      localStorage.removeItem("registrationData")
      message.success("Registration successful! Please check your email.")
      navigate("/login?status=success&message=Registration successful! Please check your email.")
    } catch (error: any) {
      message.error(error.response?.data?.message ?? "Registration failed. Try again.")
    } finally {
      setSubmitting(false)
      setLoading(false)
    }
  }

  const formVariants = {
    hidden: { opacity: 0, x: 50 },
    visible: { opacity: 1, x: 0, transition: { duration: 0.4 } },
    exit: { opacity: 0, x: -50, transition: { duration: 0.3 } },
  }

  const steps = [
    {
      title: "Account",
      content: (
        <Form.Item
          name="email"
          rules={[
            { required: true, message: "Please enter your email" },
            { type: "email", message: "Enter a valid email" },
          ]}
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
              { min: 8, message: "Minimum 8 characters" },
            ]}
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
                  return Promise.reject(new Error("Passwords do not match"))
                },
              }),
            ]}
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
    <AuthLayout>
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
                  style={{ width: "48%" }}
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

        <Space style={{ width: "100%", marginTop: 24, justifyContent: "center" }}>
          <Link to="/login" style={{ color: "rgba(255, 255, 255, 0.7)" }}>
            Already have an account? Sign in
          </Link>
        </Space>
      </StyledCard>
    </AuthLayout>
  )
}

export default Registration
