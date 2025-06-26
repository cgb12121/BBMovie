import React from 'react';
import { Tabs } from 'antd';
import DeviceManagement from './DeviceManagement';

const Settings: React.FC = () => {
  return (
    <div className="settings-container">
      <h1>Settings</h1>
      <Tabs
        defaultActiveKey="devices"
        items={[
          {
            key: 'devices',
            label: 'Device Management',
            children: <DeviceManagement />,
          },
        ]}
      />
    </div>
  );
};

export default Settings; 