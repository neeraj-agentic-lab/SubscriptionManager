import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronDown, Bell, User, LogOut, Settings, Search, Clock } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { useTenantStore } from '../../store/tenantStore';
import TenantSearchModal from '../TenantSearchModal';

export default function Header() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { selectedTenant, tenants, recentTenants, setSelectedTenant, clearTenantContext, loadTenants } = useTenantStore();
  const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const isSuperAdmin = user?.role === 'SUPER_ADMIN';

  // Load tenants when component mounts (for super admin)
  useEffect(() => {
    if (isSuperAdmin && tenants.length === 0) {
      loadTenants();
    }
  }, [isSuperAdmin, tenants.length, loadTenants]);

  const handleTenantSelect = (tenantId: string) => {
    console.log('handleTenantSelect called with:', tenantId);
    if (tenantId === '') {
      console.log('Navigating to dashboard and clearing tenant context');
      navigate('/'); // Navigate first to unmount current page
      setTimeout(() => {
        console.log('Clearing tenant context');
        clearTenantContext();
      }, 0); // Then clear tenant context
    } else if (tenantId === 'search') {
      setIsSearchModalOpen(true);
    } else {
      const tenant = tenants.find(t => t.id === tenantId);
      if (tenant) {
        console.log('Navigating to dashboard and setting tenant:', tenant.name);
        navigate('/'); // Navigate first to unmount current page
        setTimeout(() => {
          console.log('Setting tenant:', tenant.name);
          setSelectedTenant(tenant);
        }, 0); // Then set tenant
      }
    }
    setIsDropdownOpen(false);
  };

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="px-6 py-4">
        <div className="flex items-center justify-between">
          {/* Logo and Tenant Selector */}
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <div>
                <h1 className="text-lg font-semibold text-gray-900">SubscriptionManager</h1>
                <p className="text-xs text-gray-500">Console</p>
              </div>
            </div>

            {/* Tenant Selector (Super Admin only) */}
            {isSuperAdmin && (
              <div className="relative">
                <button
                  onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                  className="flex items-center gap-2 bg-gray-50 border border-gray-300 rounded-lg px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 min-w-[240px]"
                >
                  <span className="flex-1 text-left truncate">
                    {selectedTenant ? `🏢 ${selectedTenant.name}` : '🌐 All Tenants (Platform View)'}
                  </span>
                  <ChevronDown className="w-4 h-4 text-gray-500 flex-shrink-0" />
                </button>

                {/* Custom Dropdown */}
                {isDropdownOpen && (
                  <>
                    <div
                      className="fixed inset-0 z-10"
                      onClick={() => setIsDropdownOpen(false)}
                    ></div>
                    <div className="absolute top-full left-0 mt-2 w-80 bg-white rounded-lg shadow-xl border border-gray-200 py-2 z-20">
                      {/* Platform View Option */}
                      <button
                        onClick={() => handleTenantSelect('')}
                        className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition text-left ${
                          !selectedTenant ? 'bg-blue-50' : ''
                        }`}
                      >
                        <span className="text-lg">🌐</span>
                        <div className="flex-1">
                          <p className="text-sm font-medium text-gray-900">All Tenants</p>
                          <p className="text-xs text-gray-500">Platform View</p>
                        </div>
                      </button>

                      {/* Recent Tenants Section */}
                      {recentTenants.length > 0 && (
                        <>
                          <div className="px-4 py-2 border-t border-gray-200 mt-2">
                            <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase">
                              <Clock className="w-3 h-3" />
                              Recent
                            </div>
                          </div>
                          {recentTenants.map((tenant) => (
                            <button
                              key={tenant.id}
                              onClick={() => handleTenantSelect(tenant.id)}
                              className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition text-left ${
                                selectedTenant?.id === tenant.id ? 'bg-blue-50' : ''
                              }`}
                            >
                              <span className="text-lg">🏢</span>
                              <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-gray-900 truncate">{tenant.name}</p>
                                <p className="text-xs text-gray-500">
                                  {tenant.subscriptionCount} subs · ${tenant.mrr.toLocaleString()}/mo
                                </p>
                              </div>
                            </button>
                          ))}
                        </>
                      )}

                      {/* Search Option */}
                      <div className="border-t border-gray-200 mt-2">
                        <button
                          onClick={() => handleTenantSelect('search')}
                          className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition text-left text-blue-600 font-medium"
                        >
                          <Search className="w-4 h-4" />
                          <span className="text-sm">Search all tenants...</span>
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          {/* Right Side - Notifications and User Menu */}
          <div className="flex items-center gap-4">
            {/* Notifications */}
            <button className="relative p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>

            {/* User Menu */}
            <div className="flex items-center gap-3 pl-4 border-l border-gray-200">
              <div className="text-right">
                <p className="text-sm font-medium text-gray-900">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-gray-500">{user?.role.replace('_', ' ')}</p>
              </div>
              <div className="relative group">
                <button className="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-100 transition">
                  <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center">
                    <User className="w-5 h-5 text-white" />
                  </div>
                  <ChevronDown className="w-4 h-4 text-gray-500" />
                </button>
                
                {/* Dropdown Menu */}
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all">
                  <a href="#" className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                    <User className="w-4 h-4" />
                    Profile
                  </a>
                  <a href="#" className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                    <Settings className="w-4 h-4" />
                    Settings
                  </a>
                  <hr className="my-1 border-gray-200" />
                  <button
                    onClick={logout}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50 w-full text-left"
                  >
                    <LogOut className="w-4 h-4" />
                    Sign out
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tenant Context Banner */}
      {isSuperAdmin && selectedTenant && (
        <div className="bg-yellow-50 border-t border-yellow-200 px-6 py-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm">
              <svg className="w-4 h-4 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span className="text-yellow-900">
                Viewing as: <strong>{selectedTenant.name}</strong>
              </span>
              <span className="px-2 py-0.5 bg-yellow-200 text-yellow-800 text-xs font-medium rounded">
                Tenant Context
              </span>
            </div>
            <button
              onClick={() => {
                navigate('/');
                setTimeout(() => clearTenantContext(), 0);
              }}
              className="text-sm text-yellow-900 hover:text-yellow-950 font-medium flex items-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              Back to Platform View
            </button>
          </div>
        </div>
      )}

      {/* Tenant Search Modal */}
      <TenantSearchModal
        isOpen={isSearchModalOpen}
        onClose={() => setIsSearchModalOpen(false)}
        tenants={tenants}
        onSelectTenant={(tenant) => {
          setSelectedTenant(tenant);
          setIsSearchModalOpen(false);
        }}
      />
    </header>
  );
}
