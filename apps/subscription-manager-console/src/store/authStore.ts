import { create } from 'zustand';
import { authAPI, type LoginResponse } from '../lib/api';

export type UserRole = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'TENANT_USER';

interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  tenantId?: string;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  
  login: async (email, password) => {
    set({ isLoading: true, error: null });
    
    try {
      const response: LoginResponse = await authAPI.login({ email, password });
      
      // Store token in localStorage
      localStorage.setItem('auth_token', response.token);
      
      // Store token expiry time
      localStorage.setItem('auth_token_expiry', response.expiresAt);
      
      // Create user object from response
      const user: User = {
        id: response.userId,
        email: response.email,
        name: response.email.split('@')[0], // Extract name from email for now
        role: response.role as UserRole,
        tenantId: response.tenantId || undefined,
      };
      
      // Store user in localStorage for persistence
      localStorage.setItem('auth_user', JSON.stringify(user));
      
      // Set up automatic logout when token expires
      const expiryTime = new Date(response.expiresAt).getTime() - Date.now();
      if (expiryTime > 0) {
        setTimeout(() => {
          useAuthStore.getState().logout();
          window.location.href = '/';
        }, expiryTime);
      }
      
      set({ 
        user, 
        isAuthenticated: true, 
        isLoading: false,
        error: null,
      });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Login failed. Please check your credentials.';
      set({ 
        isLoading: false, 
        error: errorMessage,
        isAuthenticated: false,
        user: null,
      });
      throw error;
    }
  },
  
  logout: () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('auth_token_expiry');
    set({ user: null, isAuthenticated: false, error: null });
  },
  
  clearError: () => {
    set({ error: null });
  },
}));

// Helper function to check if token is expired
const isTokenExpired = (expiresAt: string): boolean => {
  try {
    const expiryDate = new Date(expiresAt);
    return expiryDate.getTime() < Date.now();
  } catch {
    return true;
  }
};

// Initialize auth state from localStorage on app load
const storedToken = localStorage.getItem('auth_token');
const storedUser = localStorage.getItem('auth_user');
const storedExpiry = localStorage.getItem('auth_token_expiry');

if (storedToken && storedUser && storedExpiry) {
  try {
    // Check if token is expired
    if (isTokenExpired(storedExpiry)) {
      // Token expired, clear everything
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
      localStorage.removeItem('auth_token_expiry');
      console.warn('Session expired. Please login again.');
    } else {
      // Token still valid, restore session
      const user = JSON.parse(storedUser);
      useAuthStore.setState({ user, isAuthenticated: true });
      
      // Set up automatic logout when token expires
      const expiryTime = new Date(storedExpiry).getTime() - Date.now();
      if (expiryTime > 0) {
        setTimeout(() => {
          useAuthStore.getState().logout();
          window.location.href = '/';
        }, expiryTime);
      }
    }
  } catch (error) {
    // Invalid stored data, clear it
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('auth_token_expiry');
  }
}
