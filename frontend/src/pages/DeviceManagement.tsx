import React, { useEffect, useState } from 'react';
import { Table, Button, message, Modal, Space } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import authService from '../services/authService';

interface Device {
  deviceName: string;
  deviceIpAddress: string;
  deviceOs: string;
  browser: string;
  browserVersion: string;
}

const DeviceManagement: React.FC = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedDevices, setSelectedDevices] = useState<string[]>([]);

  useEffect(() => {
    fetchDevices();
  }, []);

  const fetchDevices = async () => {
    try {
      setLoading(true);
      const response = await authService.getAllLoggedInDevices();
      if (response.success) {
        setDevices(response.data);
      }
    } catch (error: any) {
      message.error('Failed to fetch devices');
    } finally {
      setLoading(false);
    }
  };

  const handleRevokeDevices = async (p0: string[]) => {
    try {
      setLoading(true);
      const response = await authService.revokeDevices(selectedDevices);
      if (response.success) {
        message.success('Selected devices have been logged out');
        fetchDevices(); // Refresh the list
        setSelectedDevices([]); // Clear selection
      }
    } catch (error: any) {
      message.error('Failed to revoke devices');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Device Name',
      dataIndex: 'deviceName',
      key: 'deviceName',
    },
    {
      title: 'IP Address',
      dataIndex: 'deviceIpAddress',
      key: 'deviceIpAddress',
    },
    {
      title: 'Operating System',
      dataIndex: 'deviceOs',
      key: 'deviceOs',
    },
    {
      title: 'Browser',
      dataIndex: 'browser',
      key: 'browser',
      render: (text: string, record: Device) => `${record.browser} ${record.browserVersion}`,
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: Device) => (
        <Button
          type="text"
          danger
          icon={<LogoutOutlined />}
          onClick={() => handleRevokeDevices([record.deviceName])}
        >
          Revoke
        </Button>
      ),
    },
  ];

  return (
    <div className="device-management-container">
      <div className="device-management-header">
        <h2>Active Sessions</h2>
        {selectedDevices.length > 0 && (
          <Button
            type="primary"
            danger
            onClick={() => {
              Modal.confirm({
                title: 'Revoke Selected Devices',
                content: 'Are you sure you want to log out from the selected devices?',
                onOk: handleRevokeDevices,
              });
            }}
          >
            Revoke Selected ({selectedDevices.length})
          </Button>
        )}
      </div>

      <Table
        columns={columns}
        dataSource={devices}
        loading={loading}
        rowKey="deviceName"
        rowSelection={{
          type: 'checkbox',
          onChange: (selectedRowKeys) => setSelectedDevices(selectedRowKeys as string[]),
        }}
      />
    </div>
  );
};

export default DeviceManagement; 