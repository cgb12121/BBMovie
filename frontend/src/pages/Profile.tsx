import React from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { RootState } from '../redux/store';
import { User, Mail, CreditCard, Edit } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '../components/ui/avatar';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';
import { DEFAULT_PROFILE_PICTURE } from '../assets/DefaultProfilePicture';

const Profile: React.FC = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  const role = useSelector((state: RootState) => state.auth.auth?.role);
  const navigate = useNavigate();

  if (!user) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center px-4">
        <Card className="bg-gray-900 border-gray-800 max-w-md">
          <CardContent className="pt-6">
            <p className="text-white text-center">Please log in to view your profile.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Profile Header */}
        <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
          <Avatar className="h-24 w-24">
            <AvatarImage src={user.profilePictureUrl || DEFAULT_PROFILE_PICTURE} alt={user.firstName} />
            <AvatarFallback className="bg-red-600 text-white text-2xl">
              <User className="h-12 w-12" />
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 space-y-2">
            <h1 className="text-white text-3xl font-bold">{user.firstName} {user.lastName}</h1>
            <p className="text-gray-400">{user.email}</p>
            {role && (
              <Badge className="bg-red-600 text-white">{role}</Badge>
            )}
          </div>
          <Button variant="secondary" className="gap-2">
            <Edit className="h-4 w-4" />
            Edit Profile
          </Button>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="account" className="w-full">
          <TabsList className="bg-gray-900 border-gray-800">
            <TabsTrigger value="account" className="data-[state=active]:bg-gray-800">
              Account
            </TabsTrigger>
            <TabsTrigger value="billing" className="data-[state=active]:bg-gray-800">
              Billing
            </TabsTrigger>
          </TabsList>

          <TabsContent value="account" className="space-y-6 mt-6">
            <Card className="bg-gray-900 border-gray-800">
              <CardHeader>
                <CardTitle className="text-white">Account Information</CardTitle>
                <CardDescription>Manage your account details</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-sm text-gray-400">Full Name</label>
                    <div className="flex items-center gap-2 text-white p-3 bg-gray-800 rounded-md">
                      <User className="h-4 w-4 text-gray-400" />
                      <span>{user.firstName} {user.lastName}</span>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm text-gray-400">Email Address</label>
                    <div className="flex items-center gap-2 text-white p-3 bg-gray-800 rounded-md">
                      <Mail className="h-4 w-4 text-gray-400" />
                      <span>{user.email}</span>
                    </div>
                  </div>
                </div>

                <Separator className="bg-gray-800" />

                <div className="space-y-2">
                  <label className="text-sm text-gray-400">Account Status</label>
                  <div className="flex items-center justify-between p-4 bg-gray-800 rounded-md">
                    <div className="space-y-1">
                      <p className="text-white">Active Account</p>
                      <p className="text-sm text-gray-400">Your account is in good standing</p>
                    </div>
                    <Badge className="bg-green-600 text-white">Active</Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-gray-900 border-gray-800">
              <CardHeader>
                <CardTitle className="text-white">Preferences</CardTitle>
                <CardDescription>Customize your viewing experience</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-800 rounded-md">
                  <div className="space-y-1">
                    <p className="text-white">Autoplay next episode</p>
                    <p className="text-sm text-gray-400">Automatically play the next episode</p>
                  </div>
                  <Button variant="outline" className="border-gray-700 text-white">
                    Enabled
                  </Button>
                </div>
                <div className="flex items-center justify-between p-4 bg-gray-800 rounded-md">
                  <div className="space-y-1">
                    <p className="text-white">Download quality</p>
                    <p className="text-sm text-gray-400">Standard quality</p>
                  </div>
                  <Button variant="outline" className="border-gray-700 text-white">
                    Change
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="billing" className="space-y-6 mt-6">
            <Card className="bg-gray-900 border-gray-800">
              <CardHeader>
                <CardTitle className="text-white">Billing Information</CardTitle>
                <CardDescription>Manage your payment methods and billing history</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-2">
                  <label className="text-sm text-gray-400">Current Plan</label>
                  <div className="flex items-center justify-between p-4 bg-gray-800 rounded-md">
                    <div className="space-y-1">
                      <p className="text-white">Free Plan</p>
                      <p className="text-sm text-gray-400">
                        Upgrade to unlock premium features
                      </p>
                    </div>
                    <Button 
                      variant="outline" 
                      className="border-gray-700 text-white hover:bg-gray-800"
                      onClick={() => navigate('/subscriptions')}
                    >
                      Upgrade
                    </Button>
                  </div>
                </div>

                <Separator className="bg-gray-800" />

                <div className="space-y-2">
                  <label className="text-sm text-gray-400">Payment Method</label>
                  <div className="flex items-center justify-between p-4 bg-gray-800 rounded-md">
                    <div className="flex items-center gap-3">
                      <CreditCard className="h-8 w-8 text-gray-400" />
                      <div className="space-y-1">
                        <p className="text-white">No payment method on file</p>
                        <p className="text-sm text-gray-400">Add a payment method to subscribe</p>
                      </div>
                    </div>
                    <Button variant="outline" className="border-gray-700 text-white hover:bg-gray-800">
                      Add
                    </Button>
                  </div>
                </div>

                <Separator className="bg-gray-800" />

                <div className="space-y-4">
                  <label className="text-sm text-gray-400">Billing History</label>
                  <div className="text-center py-8 text-gray-400">
                    No billing history available
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default Profile;
