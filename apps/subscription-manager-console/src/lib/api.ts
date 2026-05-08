import axios from 'axios';

// API base URL - update this based on your environment
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// Create axios instance with default config
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include auth token and tenant context
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Add tenant context header if a tenant is selected
    const tenantStoreData = localStorage.getItem('tenant-store');
    if (tenantStoreData) {
      try {
        const parsed = JSON.parse(tenantStoreData);
        const selectedTenant = parsed.state?.selectedTenant;
        if (selectedTenant?.id) {
          config.headers['X-Tenant-Id'] = selectedTenant.id;
        }
      } catch (e) {
        console.error('Failed to parse tenant store data:', e);
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - clear auth and redirect to login
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

// Auth API
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  email: string;
  tenantId: string | null;
  role: string;
  expiresAt: string;
}

export interface LoginError {
  error: string;
  message: string;
}

export const authAPI = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },
};

// Tenant API
export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  subscriptionCount?: number;
  customerCount?: number;
}

export interface CreateTenantRequest {
  name: string;
  slug: string;
  status?: string;
}

export interface UpdateTenantRequest {
  name: string;
  slug: string;
  status?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const tenantsAPI = {
  getAll: async (page = 0, size = 20): Promise<PaginatedResponse<Tenant>> => {
    const response = await apiClient.get<PaginatedResponse<Tenant>>('/admin/tenants', {
      params: { page, size }
    });
    return response.data;
  },

  getById: async (tenantId: string): Promise<Tenant> => {
    const response = await apiClient.get<Tenant>(`/admin/tenants/${tenantId}`);
    return response.data;
  },

  create: async (data: CreateTenantRequest): Promise<Tenant> => {
    const response = await apiClient.post<Tenant>('/admin/tenants', data);
    return response.data;
  },

  update: async (tenantId: string, data: UpdateTenantRequest): Promise<Tenant> => {
    const response = await apiClient.put<Tenant>(`/admin/tenants/${tenantId}`, data);
    return response.data;
  },

  delete: async (tenantId: string): Promise<void> => {
    await apiClient.delete(`/admin/tenants/${tenantId}`);
  },
};

// User API
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  status: string;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  role?: string;
  status?: string;
}

export interface UserListResponse {
  users: User[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

export const usersAPI = {
  getAll: async (page = 0, size = 20, status?: string, role?: string): Promise<UserListResponse> => {
    const params: any = { page, size };
    if (status) params.status = status;
    if (role) params.role = role;
    
    const response = await apiClient.get<UserListResponse>('/admin/users', { params });
    return response.data;
  },

  getById: async (userId: string): Promise<User> => {
    const response = await apiClient.get<User>(`/admin/users/${userId}`);
    return response.data;
  },

  create: async (data: CreateUserRequest): Promise<User> => {
    const response = await apiClient.post<User>('/admin/users', data);
    return response.data;
  },

  update: async (userId: string, data: UpdateUserRequest): Promise<User> => {
    const response = await apiClient.patch<User>(`/admin/users/${userId}`, data);
    return response.data;
  },

  suspend: async (userId: string): Promise<User> => {
    const response = await apiClient.post<User>(`/admin/users/${userId}/suspend`);
    return response.data;
  },

  activate: async (userId: string): Promise<User> => {
    const response = await apiClient.post<User>(`/admin/users/${userId}/activate`);
    return response.data;
  },

  delete: async (userId: string): Promise<void> => {
    await apiClient.delete(`/admin/users/${userId}`);
  },
};

// User-Tenant API
export interface UserTenant {
  id: string;
  userId: string;
  tenantId: string;
  role: string;
  assignedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface AssignUserToTenantRequest {
  userId: string;
  tenantId: string;
  role: string;
}

export const userTenantsAPI = {
  assign: async (data: AssignUserToTenantRequest): Promise<UserTenant> => {
    const response = await apiClient.post<UserTenant>('/admin/user-tenants', data);
    return response.data;
  },

  getUserTenants: async (userId: string): Promise<UserTenant[]> => {
    const response = await apiClient.get<UserTenant[]>(`/admin/user-tenants/user/${userId}`);
    return response.data;
  },

  getTenantUsers: async (tenantId: string): Promise<UserTenant[]> => {
    const response = await apiClient.get<UserTenant[]>(`/admin/user-tenants/tenant/${tenantId}`);
    return response.data;
  },

  remove: async (userId: string, tenantId: string): Promise<void> => {
    await apiClient.delete(`/admin/user-tenants/user/${userId}/tenant/${tenantId}`);
  },
};

// Customers API
export interface Customer {
  id: string;
  tenantId: string;
  email: string;
  firstName?: string;
  lastName?: string;
  externalCustomerId?: string;
  status: string;
  customerType: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  email: string;
  name?: string;
  externalCustomerRef?: string;
}

export const customersAPI = {
  getAll: async (): Promise<Customer[]> => {
    const response = await apiClient.get<Customer[]>('/admin/customers');
    return response.data;
  },

  getById: async (customerId: string): Promise<Customer> => {
    const response = await apiClient.get<Customer>(`/admin/customers/${customerId}`);
    return response.data;
  },

  create: async (data: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<Customer>('/admin/customers', data);
    return response.data;
  },

  update: async (customerId: string, data: Partial<Customer>): Promise<Customer> => {
    const response = await apiClient.put<Customer>(`/admin/customers/${customerId}`, data);
    return response.data;
  },

  delete: async (customerId: string): Promise<void> => {
    await apiClient.delete(`/admin/customers/${customerId}`);
  },
};

// Plans API
export interface Plan {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  amount: number;
  currency: string;
  billingInterval: string;
  intervalCount: number;
  trialPeriodDays?: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  name: string;
  description?: string;
  amount: number;
  currency: string;
  billingInterval: string;
  intervalCount?: number;
  trialPeriodDays?: number;
}

export const plansAPI = {
  getAll: async (page = 0, size = 20): Promise<{ content: Plan[]; totalPages: number; totalElements: number }> => {
    const response = await apiClient.get('/admin/plans', { params: { page, size } });
    return response.data;
  },

  getById: async (planId: string): Promise<Plan> => {
    const response = await apiClient.get<Plan>(`/admin/plans/${planId}`);
    return response.data;
  },

  create: async (data: CreatePlanRequest): Promise<Plan> => {
    const response = await apiClient.post<Plan>('/admin/plans', data);
    return response.data;
  },

  update: async (planId: string, data: Partial<CreatePlanRequest>): Promise<Plan> => {
    const response = await apiClient.put<Plan>(`/admin/plans/${planId}`, data);
    return response.data;
  },

  delete: async (planId: string): Promise<void> => {
    await apiClient.delete(`/admin/plans/${planId}`);
  },
};

// Subscriptions API
export interface Subscription {
  id: string;
  tenantId: string;
  customerId: string;
  planId: string;
  status: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  canceledAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubscriptionRequest {
  customerId: string;
  planId: string;
  startDate?: string;
}

export const subscriptionsAPI = {
  getAll: async (page = 0, size = 20, tenantId?: string): Promise<{ content: Subscription[]; totalPages: number; totalElements: number }> => {
    const params: any = { page, size };
    if (tenantId) {
      params.tenantId = tenantId;
    }
    const response = await apiClient.get('/admin/subscriptions', { params });
    return response.data;
  },

  getById: async (subscriptionId: string): Promise<Subscription> => {
    const response = await apiClient.get<Subscription>(`/admin/subscriptions/${subscriptionId}`);
    return response.data;
  },

  create: async (data: CreateSubscriptionRequest): Promise<Subscription> => {
    const response = await apiClient.post<Subscription>('/admin/subscriptions', data);
    return response.data;
  },

  cancel: async (subscriptionId: string): Promise<Subscription> => {
    const response = await apiClient.post<Subscription>(`/admin/subscriptions/${subscriptionId}/cancel`);
    return response.data;
  },

  reactivate: async (subscriptionId: string): Promise<Subscription> => {
    const response = await apiClient.post<Subscription>(`/admin/subscriptions/${subscriptionId}/reactivate`);
    return response.data;
  },
};

// Deliveries API
export interface Delivery {
  id: string;
  subscriptionId: string;
  scheduledFor: string;
  deliveredAt?: string;
  status: string;
  createdAt: string;
}

export const deliveriesAPI = {
  getAll: async (page = 0, size = 20): Promise<{ content: Delivery[]; totalPages: number; totalElements: number }> => {
    const response = await apiClient.get('/admin/deliveries', { params: { page, size } });
    return response.data;
  },

  getById: async (deliveryId: string): Promise<Delivery> => {
    const response = await apiClient.get<Delivery>(`/admin/deliveries/${deliveryId}`);
    return response.data;
  },
};

// Webhooks API
export interface Webhook {
  id: string;
  tenantId: string;
  url: string;
  events: string[];
  status: string;
  createdAt: string;
}

export interface CreateWebhookRequest {
  url: string;
  events: string[];
}

export const webhooksAPI = {
  getAll: async (): Promise<Webhook[]> => {
    const response = await apiClient.get<{ data: { webhooks: Webhook[] } }>('/admin/webhooks');
    return response.data.data.webhooks;
  },

  getById: async (webhookId: string): Promise<Webhook> => {
    const response = await apiClient.get<Webhook>(`/admin/webhooks/${webhookId}`);
    return response.data;
  },

  create: async (data: CreateWebhookRequest): Promise<Webhook> => {
    const response = await apiClient.post<Webhook>('/admin/webhooks', data);
    return response.data;
  },

  update: async (webhookId: string, data: Partial<CreateWebhookRequest>): Promise<Webhook> => {
    const response = await apiClient.put<Webhook>(`/admin/webhooks/${webhookId}`, data);
    return response.data;
  },

  delete: async (webhookId: string): Promise<void> => {
    await apiClient.delete(`/admin/webhooks/${webhookId}`);
  },
};

// API Clients API
export interface APIClient {
  id: string;
  tenantId: string;
  name: string;
  apiKey: string;
  status: string;
  createdAt: string;
}

export interface CreateAPIClientRequest {
  name: string;
}

export const apiClientsAPI = {
  getAll: async (): Promise<APIClient[]> => {
    const response = await apiClient.get<{ content: APIClient[] }>('/admin/api-clients/tenant');
    return response.data.content;
  },

  create: async (data: CreateAPIClientRequest): Promise<APIClient> => {
    const response = await apiClient.post<APIClient>('/admin/api-clients', data);
    return response.data;
  },

  delete: async (clientId: string): Promise<void> => {
    await apiClient.delete(`/admin/api-clients/${clientId}`);
  },
};
