import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

interface UserResponse {
  username: string
  email: string
  firstName: string
  lastName: string
  profilePictureUrl: string | undefined;
}

interface AuthResponse {
  accessToken: string
  email: string
  role: string
}

interface UserAgentResponse {
  deviceName: string
  deviceOs: string
  deviceIpAddress: string
  browser: string
  browserVersion: string
}

interface AuthState {
  user: UserResponse | null
  auth: AuthResponse | null
  userAgent: UserAgentResponse | null
}

const initialState: AuthState = {
  user: null,
  auth: null,
  userAgent: null
}

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setCredentials: (
      state,
      action: PayloadAction<{ user: UserResponse; auth: AuthResponse; userAgent: UserAgentResponse }>
    ) => {
      state.user = action.payload.user
      state.auth = action.payload.auth
      state.userAgent = action.payload.userAgent
    },
    logout: (state) => {
      state.user = null
      state.auth = null
      state.userAgent = null
      localStorage.removeItem('user');
      localStorage.removeItem('auth');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('userAgent');
    }
  }
})

export const { setCredentials, logout } = authSlice.actions
export default authSlice.reducer
