import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Modal } from 'antd';
import { Smartphone, Monitor, Tv, Tablet, MapPin, Clock, Loader2 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';
import authService from '../services/authService';
import { useUserAgent } from '../hooks/useUserAgent';

interface LoggedInDeviceResponse {
  deviceName: string;
  ipAddress: string;
  current: boolean;
}

interface DeviceRevokeEntry {
  deviceName: string;
  ip: string;
}

const DeviceManagement: React.FC = () => {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<LoggedInDeviceResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const { userAgent, loading: userAgentLoading } = useUserAgent();

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
      console.log(error);
      message.error('Failed to fetch devices');
    } finally {
      setLoading(false);
    }
  };

  const handleRevokeDevice = async (device: LoggedInDeviceResponse) => {
    Modal.confirm({
      title: 'Sign Out Device',
      content: `Are you sure you want to sign out from "${device.deviceName}"?`,
      okText: 'Sign Out',
      okType: 'danger',
      onOk: async () => {
        try {
          setLoading(true);
          const response = await authService.revokeDevices([{ 
            deviceName: device.deviceName, 
            ip: device.ipAddress 
          }]);
          if (response.success) {
            message.success('Device signed out successfully');
            fetchDevices();
          }
        } catch (error: any) {
          console.log(error);
          message.error('Failed to sign out device');
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const handleRevokeAllDevices = () => {
    const otherDevices = devices.filter(d => !d.current);
    Modal.confirm({
      title: 'Sign Out All Devices',
      content: `Are you sure you want to sign out from all ${otherDevices.length} other devices?`,
      okText: 'Sign Out All',
      okType: 'danger',
      onOk: async () => {
        try {
          setLoading(true);
          const response = await authService.revokeDevices(
            otherDevices.map(d => ({ deviceName: d.deviceName, ip: d.ipAddress }))
          );
          if (response.success) {
            message.success('All other devices signed out successfully');
            fetchDevices();
          }
        } catch (error: any) {
          console.log(error);
          message.error('Failed to sign out devices');
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const getDeviceIcon = (deviceName: string) => {
    const name = deviceName.toLowerCase();
    if (name.includes('mobile') || name.includes('iphone') || name.includes('android')) {
      return <Smartphone className="h-6 w-6 text-gray-400" />;
    }
    if (name.includes('tablet') || name.includes('ipad')) {
      return <Tablet className="h-6 w-6 text-gray-400" />;
    }
    if (name.includes('tv')) {
      return <Tv className="h-6 w-6 text-gray-400" />;
    }
    return <Monitor className="h-6 w-6 text-gray-400" />;
  };

  const currentDevice = devices.find(d => d.current);
  const otherDevices = devices.filter(d => !d.current);

  if (loading || userAgentLoading) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <Loader2 className="h-12 w-12 text-red-600 animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header */}
        <div className="space-y-2">
          <h1 className="text-white text-3xl md:text-4xl font-bold">Device Management</h1>
          <p className="text-gray-400">Manage devices that have access to your account</p>
        </div>

        {/* Info Card */}
        <Card className="bg-blue-900/20 border-blue-800">
          <CardContent className="p-4">
            <p className="text-blue-200 text-sm">
              🔒 For your security, you can sign out of any device remotely. If you see a device you don't recognize, 
              change your password immediately.
            </p>
          </CardContent>
        </Card>

        {/* Current Device */}
        {currentDevice && (
          <Card className="bg-gray-900 border-gray-800">
            <CardHeader>
              <CardTitle className="text-white">Current Device</CardTitle>
              <CardDescription>This is the device you're currently using</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="bg-gray-800 border border-gray-700 rounded-lg p-4">
                <div className="flex items-start gap-4">
                  <div className="bg-green-900/20 border border-green-800 rounded-lg p-3">
                    {getDeviceIcon(currentDevice.deviceName)}
                  </div>
                  <div className="flex-1 space-y-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-white">{currentDevice.deviceName}</h3>
                          <Badge className="bg-green-600 text-white text-xs">Active Now</Badge>
                        </div>
                        <div className="flex items-center gap-4 mt-2 text-sm text-gray-400">
                          <div className="flex items-center gap-1">
                            <MapPin className="h-4 w-4" />
                            <span>{currentDevice.ipAddress}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Other Devices */}
        {otherDevices.length > 0 && (
          <Card className="bg-gray-900 border-gray-800">
            <CardHeader>
              <CardTitle className="text-white">Other Devices</CardTitle>
              <CardDescription>Devices that have accessed your account recently</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {otherDevices.map((device, index) => (
                <div key={device.deviceName + device.ipAddress}>
                  {index > 0 && <Separator className="bg-gray-800 my-4" />}
                  <div className="flex items-start gap-4">
                    <div className="bg-gray-800 border border-gray-700 rounded-lg p-3">
                      {getDeviceIcon(device.deviceName)}
                    </div>
                    <div className="flex-1 space-y-2">
                      <div className="flex items-start justify-between">
                        <div>
                          <h3 className="text-white">{device.deviceName}</h3>
                          <div className="flex items-center gap-4 mt-1 text-sm text-gray-400">
                            <div className="flex items-center gap-1">
                              <MapPin className="h-4 w-4" />
                              <span>{device.ipAddress}</span>
                            </div>
                          </div>
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-500 hover:text-red-400 hover:bg-red-900/20"
                          onClick={() => handleRevokeDevice(device)}
                        >
                          Sign Out
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        )}

        {otherDevices.length === 0 && !currentDevice && (
          <Card className="bg-gray-900 border-gray-800">
            <CardContent className="p-8 text-center">
              <p className="text-gray-400">No devices found</p>
            </CardContent>
          </Card>
        )}

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3">
          <Button
            variant="outline"
            className="flex-1 border-gray-700 text-white hover:bg-gray-800"
            onClick={() => navigate('/settings')}
          >
            Back to Settings
          </Button>
          {otherDevices.length > 0 && (
            <Button
              variant="destructive"
              className="flex-1 bg-red-600 hover:bg-red-700 text-white"
              onClick={handleRevokeAllDevices}
            >
              Sign Out All Other Devices
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default DeviceManagement; 