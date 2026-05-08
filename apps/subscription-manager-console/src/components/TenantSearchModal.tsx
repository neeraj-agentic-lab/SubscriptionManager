import { useState, useEffect } from 'react';
import { Search, X, Building } from 'lucide-react';
import type { Tenant } from '../store/tenantStore';

interface TenantSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  tenants: Tenant[];
  onSelectTenant: (tenant: Tenant) => void;
}

export default function TenantSearchModal({ isOpen, onClose, tenants, onSelectTenant }: TenantSearchModalProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredTenants, setFilteredTenants] = useState<Tenant[]>(tenants);

  useEffect(() => {
    if (searchQuery.trim() === '') {
      setFilteredTenants(tenants);
    } else {
      const query = searchQuery.toLowerCase();
      const filtered = tenants.filter(
        (tenant) =>
          tenant.name.toLowerCase().includes(query) ||
          tenant.slug.toLowerCase().includes(query)
      );
      setFilteredTenants(filtered);
    }
  }, [searchQuery, tenants]);

  const handleSelectTenant = (tenant: Tenant) => {
    onSelectTenant(tenant);
    onClose();
    setSearchQuery('');
  };

  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 z-[100] transition-opacity"
        onClick={onClose}
      ></div>

      {/* Modal */}
      <div className="fixed inset-0 z-[101] flex items-center justify-center px-4 pointer-events-none">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[600px] flex flex-col pointer-events-auto">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <h2 className="text-xl font-semibold text-gray-900">Search Tenants</h2>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-lg transition"
            >
              <X className="w-5 h-5 text-gray-500" />
            </button>
          </div>

          {/* Search Input */}
          <div className="p-6 border-b border-gray-200">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by tenant name or slug..."
                className="w-full pl-12 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                autoFocus
              />
            </div>
          </div>

          {/* Results */}
          <div className="flex-1 overflow-y-auto p-4">
            {filteredTenants.length === 0 ? (
              <div className="text-center py-12">
                <Building className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500">No tenants found</p>
                <p className="text-sm text-gray-400 mt-1">Try a different search term</p>
              </div>
            ) : (
              <div className="space-y-2">
                {filteredTenants.map((tenant) => (
                  <button
                    key={tenant.id}
                    onClick={() => handleSelectTenant(tenant)}
                    className="w-full flex items-center justify-between p-4 hover:bg-gray-50 rounded-lg transition text-left group"
                  >
                    <div className="flex items-center gap-3 flex-1">
                      <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition">
                        <Building className="w-5 h-5 text-blue-600" />
                      </div>
                      <div className="flex-1">
                        <h3 className="font-medium text-gray-900">{tenant.name}</h3>
                        <p className="text-sm text-gray-500">{tenant.slug}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-medium text-gray-900">
                        {tenant.subscriptionCount} subs
                      </p>
                      <p className="text-xs text-gray-500">
                        ${tenant.mrr.toLocaleString()}/mo
                      </p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-gray-200 bg-gray-50 rounded-b-xl">
            <p className="text-sm text-gray-600 text-center">
              Showing {filteredTenants.length} of {tenants.length} tenants
            </p>
          </div>
        </div>
      </div>
    </>
  );
}
