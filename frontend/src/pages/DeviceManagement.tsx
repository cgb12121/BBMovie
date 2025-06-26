import React, { useEffect, useState } from 'react';
import { Table, Button, message, Modal, Space, Tag } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import authService from '../services/authService';
import { useUserAgent } from '../hooks/useUserAgent';

interface Device {
  deviceName: string;
  deviceOs: string;
  deviceIpAddress: string;
  browser: string;
  browserVersion: string;
}

interface DeviceRevokeEntry {
  deviceName: string;
  ip: string;
}

const DeviceManagement: React.FC = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedDevices, setSelectedDevices] = useState<DeviceRevokeEntry[]>([]);
  const { userAgent, loading: userAgentLoading } = useUserAgent();

  useEffect(() => {
    fetchDevices();
  }, []);

  const fetchDevices = async () => {
    try {
      setLoading(true);
      const response = await authService.getAllLoggedInDevices();
      if (response.success) {
        const transformedDevices = response.data.map((device: Device) => ({
          ...device,
          isCurrentDevice: userAgent ? device.deviceName === userAgent.deviceName : false
        }));
        setDevices(transformedDevices);
      }
    } catch (error: any) {
      console.log(error);
      message.error('Failed to fetch devices');
    } finally {
      setLoading(false);
    }
  };

  const handleRevokeDevices = async (devicesToRevoke: DeviceRevokeEntry[]) => {
    try {
      setLoading(true);
      const response = await authService.revokeDevices(devicesToRevoke.map(device => device.deviceName));
      if (response.success) {
        message.success(response.data);
        fetchDevices();
        setSelectedDevices([]);
      }
    } catch (error: any) {
      console.log(error);
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
      render: (text: string, record: Device) => (
        <Space>
          {text}
          {record.deviceName === userAgent?.deviceName && (
            <Tag color="blue">Current Device</Tag>
          )}
        </Space>
      ),
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
      render: (_: any, record: Device) => 
        record.browser ? `${record.browser} ${record.browserVersion}` : 'N/A',
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: Device) => (
        <Button
          type="text"
          danger
          icon={<LogoutOutlined />}
          onClick={() => handleRevokeDevices([{ 
            deviceName: record.deviceName, 
            ip: record.deviceIpAddress 
          }])}
          disabled={record.deviceName === userAgent?.deviceName}
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
                onOk: () => handleRevokeDevices(selectedDevices),
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
        loading={loading || userAgentLoading}
        rowKey="deviceName"
        rowSelection={{
          type: 'checkbox',
          onChange: (selectedRowKeys, selectedRows) => {
            setSelectedDevices(selectedRows.map(row => ({
              deviceName: row.deviceName,
              ip: row.deviceIpAddress
            })));
          },
          getCheckboxProps: (record: Device) => ({
            disabled: record.deviceName === userAgent?.deviceName,
          }),
        }}
      />
    </div>
  );
};

export default DeviceManagement; 