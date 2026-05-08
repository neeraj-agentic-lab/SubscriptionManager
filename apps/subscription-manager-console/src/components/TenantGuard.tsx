import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTenantStore } from '../store/tenantStore';

interface TenantGuardProps {
  children: React.ReactNode;
}

/**
 * Guard component that redirects to dashboard if no tenant is selected.
 * Use this to wrap tenant-scoped pages to prevent API errors when tenant context is cleared.
 */
export default function TenantGuard({ children }: TenantGuardProps) {
  const { selectedTenant } = useTenantStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!selectedTenant) {
      // Redirect to dashboard if no tenant is selected
      navigate('/');
    }
  }, [selectedTenant, navigate]);

  // Don't render children if no tenant is selected
  if (!selectedTenant) {
    return null;
  }

  return <>{children}</>;
}
