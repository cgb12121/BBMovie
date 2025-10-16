import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, Globe, Shield, Video, Volume2 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Switch } from '../components/ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Slider } from '../components/ui/slider';
import { Button } from '../components/ui/button';
import { Separator } from '../components/ui/separator';
import { Label } from '../components/ui/label';

const Settings: React.FC = () => {
  const navigate = useNavigate();
  const [settings, setSettings] = useState({
    autoplay: true,
    notifications: true,
    darkMode: true,
    autoDownload: false,
    dataUsage: 'medium',
    language: 'en',
    subtitles: true,
    volume: [70],
  });

  return (
    <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header */}
        <div className="space-y-2">
          <h1 className="text-white text-3xl md:text-4xl font-bold">Settings</h1>
          <p className="text-gray-400">Customize your viewing experience</p>
        </div>

        {/* Playback Settings */}
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader>
            <div className="flex items-center gap-3">
              <Video className="h-5 w-5 text-red-600" />
              <CardTitle className="text-white">Playback</CardTitle>
            </div>
            <CardDescription>Control how videos play</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="text-white">Autoplay next episode</p>
                <p className="text-sm text-gray-400">Automatically start the next episode</p>
              </div>
              <Switch
                checked={settings.autoplay}
                onCheckedChange={(checked) => setSettings({ ...settings, autoplay: checked })}
              />
            </div>

            <Separator className="bg-gray-800" />

            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="text-white">Subtitles</p>
                <p className="text-sm text-gray-400">Show subtitles by default</p>
              </div>
              <Switch
                checked={settings.subtitles}
                onCheckedChange={(checked) => setSettings({ ...settings, subtitles: checked })}
              />
            </div>

            <Separator className="bg-gray-800" />

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Volume2 className="h-5 w-5 text-gray-400" />
                  <p className="text-white">Default Volume</p>
                </div>
                <span className="text-gray-400">{settings.volume[0]}%</span>
              </div>
              <Slider
                value={settings.volume}
                onValueChange={(value) => setSettings({ ...settings, volume: value })}
                max={100}
                step={1}
                className="cursor-pointer"
              />
            </div>
          </CardContent>
        </Card>

        {/* Notifications */}
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader>
            <div className="flex items-center gap-3">
              <Bell className="h-5 w-5 text-red-600" />
              <CardTitle className="text-white">Notifications</CardTitle>
            </div>
            <CardDescription>Manage your notification preferences</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="text-white">Push notifications</p>
                <p className="text-sm text-gray-400">Receive updates about new content</p>
              </div>
              <Switch
                checked={settings.notifications}
                onCheckedChange={(checked) => setSettings({ ...settings, notifications: checked })}
              />
            </div>

            <Separator className="bg-gray-800" />

            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="text-white">Email notifications</p>
                <p className="text-sm text-gray-400">Get emails about your account</p>
              </div>
              <Switch defaultChecked />
            </div>
          </CardContent>
        </Card>

        {/* Download & Data */}
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader>
            <CardTitle className="text-white">Download & Data Usage</CardTitle>
            <CardDescription>Manage storage and data settings</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="text-white">Auto-download over Wi-Fi</p>
                <p className="text-sm text-gray-400">Download new episodes automatically</p>
              </div>
              <Switch
                checked={settings.autoDownload}
                onCheckedChange={(checked) => setSettings({ ...settings, autoDownload: checked })}
              />
            </div>

            <Separator className="bg-gray-800" />

            <div className="space-y-2">
              <Label className="text-white">Video Quality</Label>
              <Select value={settings.dataUsage} onValueChange={(value) => setSettings({ ...settings, dataUsage: value })}>
                <SelectTrigger className="bg-gray-800 border-gray-700 text-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="low">Low - Save data</SelectItem>
                  <SelectItem value="medium">Medium - Balanced</SelectItem>
                  <SelectItem value="high">High - Best quality</SelectItem>
                  <SelectItem value="auto">Auto</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Language & Region */}
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader>
            <div className="flex items-center gap-3">
              <Globe className="h-5 w-5 text-red-600" />
              <CardTitle className="text-white">Language & Region</CardTitle>
            </div>
            <CardDescription>Set your language preferences</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label className="text-white">Display Language</Label>
              <Select value={settings.language} onValueChange={(value) => setSettings({ ...settings, language: value })}>
                <SelectTrigger className="bg-gray-800 border-gray-700 text-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="en">English</SelectItem>
                  <SelectItem value="es">Español</SelectItem>
                  <SelectItem value="fr">Français</SelectItem>
                  <SelectItem value="de">Deutsch</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Security */}
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader>
            <div className="flex items-center gap-3">
              <Shield className="h-5 w-5 text-red-600" />
              <CardTitle className="text-white">Security & Privacy</CardTitle>
            </div>
            <CardDescription>Manage your account security</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button
              variant="outline"
              className="w-full border-gray-700 text-white hover:bg-gray-800"
              onClick={() => navigate('/device-management')}
            >
              Manage Devices
            </Button>
            <Button
              variant="outline"
              className="w-full border-gray-700 text-white hover:bg-gray-800"
              onClick={() => navigate('/password-reset')}
            >
              Change Password
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Settings; 