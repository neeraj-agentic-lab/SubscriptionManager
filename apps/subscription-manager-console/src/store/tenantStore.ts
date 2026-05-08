import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { tenantsAPI, type Tenant as APITenant } from '../lib/api';

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: 'ACTIVE' | 'SUSPENDED';
  subscriptionCount: number;
  mrr: number;
  createdAt: string;
}

interface TenantState {
  selectedTenant: Tenant | null;
  tenants: Tenant[];
  recentTenants: Tenant[];
  isLoading: boolean;
  setSelectedTenant: (tenant: Tenant | null) => void;
  clearTenantContext: () => void;
  loadTenants: () => Promise<void>;
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
  selectedTenant: null,
  tenants: [],
  recentTenants: [],
  isLoading: false,

  loadTenants: async () => {
    try {
      set({ isLoading: true });
      const response = await tenantsAPI.getAll(0, 100); // Get first 100 tenants
      const tenants: Tenant[] = response.content.map((t: APITenant) => ({
        id: t.id,
        name: t.name,
        slug: t.slug,
        status: t.status as 'ACTIVE' | 'SUSPENDED',
        subscriptionCount: 0, // Placeholder - would need separate API call
        mrr: 0, // Placeholder - would need separate API call
        createdAt: t.createdAt,
      }));
      set({ tenants, isLoading: false });
    } catch (error) {
      console.error('Failed to load tenants:', error);
      set({ isLoading: false });
    }
  },
  
  setSelectedTenant: (tenant) => {
    if (tenant) {
      set((state) => {
        // Add to recent tenants, keeping only last 5 unique tenants
        const updatedRecent = [
          tenant,
          ...state.recentTenants.filter((t) => t.id !== tenant.id),
        ].slice(0, 5);
        
        return {
          selectedTenant: tenant,
          recentTenants: updatedRecent,
        };
      });
    } else {
      set({ selectedTenant: tenant });
    }
  },
  
  clearTenantContext: () => {
    set({ selectedTenant: null });
  },
}),
    {
      name: 'tenant-store',
    }
  )
);
