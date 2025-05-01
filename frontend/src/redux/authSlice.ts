import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserResponse {
  username: string
  email: string
  firstName: string
  lastName: string
  profilePictureUrl: string | null
}

interface AuthResponse {
  accessToken: string
  email: string
  role: string
}

interface AuthState {
  user: UserResponse | null
  auth: AuthResponse | null
}

const initialState: AuthState = {
  user: null,
  auth: null
}

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setCredentials: (
      state,
      action: PayloadAction<{ user: UserResponse; auth: AuthResponse }>
    ) => {
      state.user = action.payload.user
      state.auth = action.payload.auth
    },
    logout: (state) => {
      state.user = null
      state.auth = null

      localStorage.removeItem('user');
      localStorage.removeItem('auth');
      localStorage.removeItem('accessToken');
    }
  }
})

export const { setCredentials, logout } = authSlice.actions
export default authSlice.reducer
