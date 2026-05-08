import { 
  LayoutDashboard, 
  Building, 
  Users, 
  UserCircle, 
  FileText, 
  Repeat, 
  Package, 
  Webhook, 
  Key, 
  BarChart,
  Globe,
  Server
} from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { useTenantStore } from '../../store/tenantStore';
import { Link, useLocation } from 'react-router-dom';

interface NavItem {
  icon: React.ElementType;
  label: string;
  path: string;
  badge?: string | number;
}

export default function Sidebar() {
  const { user } = useAuthStore();
  const { selectedTenant, clearTenantContext } = useTenantStore();
  const location = useLocation();

  const isSuperAdmin = user?.role === 'SUPER_ADMIN';
  const inTenantContext = isSuperAdmin && selectedTenant;

  // Platform View Navigation (Super Admin without tenant selected)
  const platformNav: NavItem[] = [
    { icon: LayoutDashboard, label: 'Platform Dashboard', path: '/' },
    { icon: Building, label: 'Tenants', path: '/tenants', badge: 45 },
    { icon: Users, label: 'Users (All)', path: '/users', badge: 234 },
    { icon: Server, label: 'System', path: '/system' },
    { icon: BarChart, label: 'Analytics', path: '/' },
  ];

  // Tenant Context Navigation (Super Admin with tenant OR Tenant Admin)
  const tenantNav: NavItem[] = [
    { icon: LayoutDashboard, label: 'Dashboard', path: '/' },
    { icon: Users, label: 'Users', path: '/users', badge: 12 },
    { icon: UserCircle, label: 'Customers', path: '/customers', badge: 156 },
    { icon: FileText, label: 'Plans', path: '/plans', badge: 8 },
    { icon: Repeat, label: 'Subscriptions', path: '/subscriptions', badge: 89 },
    { icon: Package, label: 'Deliveries', path: '/deliveries', badge: 23 },
    { icon: Webhook, label: 'Webhooks', path: '/webhooks', badge: 5 },
    { icon: Key, label: 'API Clients', path: '/api-clients', badge: 3 },
    { icon: BarChart, label: 'Reports', path: '/reports' },
  ];

  // Tenant User Navigation (Read-only)
  const tenantUserNav: NavItem[] = [
    { icon: LayoutDashboard, label: 'Dashboard', path: '/' },
    { icon: UserCircle, label: 'Customers', path: '/customers' },
    { icon: Repeat, label: 'Subscriptions', path: '/subscriptions' },
    { icon: Package, label: 'Deliveries', path: '/deliveries' },
  ];

  // Determine which navigation to show
  let navigation: NavItem[] = [];
  if (user?.role === 'SUPER_ADMIN' && !selectedTenant) {
    navigation = platformNav;
  } else if (user?.role === 'SUPER_ADMIN' || user?.role === 'TENANT_ADMIN') {
    navigation = tenantNav;
  } else {
    navigation = tenantUserNav;
  }

  return (
    <aside className="w-64 bg-gray-900 text-gray-100 flex flex-col h-screen sticky top-0">
      <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
        {navigation.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.label}
              to={item.path}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition group ${
                isActive 
                  ? 'bg-blue-600 text-white' 
                  : 'text-gray-300 hover:bg-gray-800 hover:text-white'
              }`}
            >
              <item.icon className="w-5 h-5" />
              <span className="flex-1 font-medium">{item.label}</span>
              {item.badge !== undefined && (
                <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${
                  isActive 
                    ? 'bg-blue-700 text-white' 
                    : 'bg-gray-700 text-gray-300 group-hover:bg-gray-600'
                }`}>
                  {item.badge}
                </span>
              )}
            </Link>
          );
        })}

        {/* Platform View Link (for Super Admin in tenant context) */}
        {inTenantContext && (
          <>
            <div className="my-4 border-t border-gray-700"></div>
            <button
              onClick={clearTenantContext}
              className="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-400 hover:bg-gray-800 hover:text-white transition w-full"
            >
              <Globe className="w-5 h-5" />
              <span className="flex-1 font-medium text-left">Platform View</span>
            </button>
          </>
        )}
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-gray-800">
        <div className="px-4 py-3 bg-gray-800 rounded-lg">
          <p className="text-xs text-gray-400 mb-1">Current View</p>
          <p className="text-sm font-medium text-white">
            {isSuperAdmin && !selectedTenant && '🌐 Platform'}
            {isSuperAdmin && selectedTenant && `🏢 ${selectedTenant.name}`}
            {user?.role === 'TENANT_ADMIN' && '🏢 Tenant Admin'}
            {user?.role === 'TENANT_USER' && '👤 Tenant User'}
          </p>
        </div>
      </div>
    </aside>
  );
}
