import React, { useEffect, useState } from 'react';
import { Modal, Input, Form, Typography, message } from 'antd';
import mfaService from '../../services/mfaService';
import api from '../../services/api';

const { Text } = Typography;

interface Props {
  children: React.ReactNode;
}

const MfaProvider: React.FC<Props> = ({ children }) => {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm<{ code: string }>();

  useEffect(() => {
    const unsubscribe = mfaService.onPrompt(() => {
      setOpen(true);
      // fire setup to send OTP to email
      setupMfa().catch(() => {/* ignore */});
    });
    return unsubscribe;
  }, []);

  const setupMfa = async () => {
    setLoading(true);
    await api.post('/api/mfa/setup').then(() => {
      message.info('A verification code was sent to your email');
    }).catch(() => {
      // if already enabled or setup returned error, still allow user to enter code
    }).finally(() => setLoading(false));
  };

  const handleOk = async () => {
    try {
      const { code } = await form.validateFields();
      setLoading(true);
      await api.post('/api/mfa/verify', { code });
      message.success('MFA verified');
      setOpen(false);
      form.resetFields();
      mfaService.complete(true);
    } catch (e: any) {
      const msg = e?.response?.data?.message || 'Invalid code or expired';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setOpen(false);
    form.resetFields();
    mfaService.complete(false);
  };

  return (
    <>
      {children}
      <Modal
        title="Multi-factor authentication"
        open={open}
        onOk={handleOk}
        confirmLoading={loading}
        onCancel={handleCancel}
        okText="Verify"
        cancelButtonProps={{ disabled: loading }}
        maskClosable={false}
        closable={!loading}
      >
        <Text>Please enter the 6-digit code sent to your email.</Text>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item name="code" label="Verification code" rules={[{ required: true, message: 'Code is required' }, { len: 6, message: '6 digits' }]}>
            <Input inputMode="numeric" maxLength={6} placeholder="123456" disabled={loading} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default MfaProvider;


