"use client"

import type React from "react"
import { useEffect } from "react"
import styled, { keyframes } from "styled-components"
import { Typography } from "antd"
import { motion } from "framer-motion"
import { Link } from "react-router-dom"

const { Title, Text } = Typography

const fadeIn = keyframes`
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
`

const Container = styled.div`
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.7)),
    url('https://images.unsplash.com/photo-1489599849927-2ee91cede3cf?q=80&w=2070&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  padding: 1rem;
  position: relative;
  animation: ${fadeIn} 1s ease-out;
`

const Overlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1;
`

const Content = styled.div`
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 450px;
`

const Logo = styled.div`
  text-align: center;
  margin-bottom: 1.5rem;
`

const LogoText = styled(Link)`
  color: #e50914;
  font-size: 2.5rem;
  margin: 0;
  font-weight: 700;
  font-family: 'Poppins', sans-serif;
  letter-spacing: -0.5px;
`

const StyledTitle = styled(Title)`
  text-align: center;
  color: #ffffff !important;
  margin-bottom: 0.5rem !important;
`

const StyledSubtitle = styled(Text)`
  display: block;
  text-align: center;
  color: #d9d9d9 !important;
  margin-bottom: 2rem;
  font-size: 1rem;
`

interface AuthLayoutProps {
  children: React.ReactNode
}

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      duration: 0.6,
      when: "beforeChildren",
      staggerChildren: 0.2,
    },
  },
}

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: { duration: 0.5 },
  },
}

const AuthLayout: React.FC<AuthLayoutProps> = ({ children }) => {
  useEffect(() => {
    document.body.style.overflow = "hidden"
    return () => {
      document.body.style.overflow = "auto"
    }
  }, [])

  return (
    <Container>
      <Overlay />
      <Content>
        <motion.div variants={containerVariants} initial="hidden" animate="visible">
          <motion.div variants={itemVariants}>
            <Logo>
              <LogoText to="/">BBMovie</LogoText>
            </Logo>
          </motion.div>
          <motion.div variants={itemVariants}>{children}</motion.div>
        </motion.div>
      </Content>
    </Container>
  )
}

export default AuthLayout
