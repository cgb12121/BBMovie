import React from 'react';
import { useSelector } from 'react-redux';
import { RootState } from '../redux/store';

const Profile: React.FC = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  const role = useSelector((state: RootState) => state.auth.auth?.role);

  if (!user) {
    return <div>Please log in to view your profile.</div>;
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Welcome, {user.firstName} {user.lastName}</h1>
      <p>Email: {user.email}</p>
      {role && <p>Role: {role}</p>}
      <img
        src={user.profilePictureUrl || '/default-avatar.png'}
        alt="Profile"
        style={{ width: 150, height: 150, borderRadius: '50%', objectFit: 'cover' }}
      />
    </div>
  );
};

export default Profile;
