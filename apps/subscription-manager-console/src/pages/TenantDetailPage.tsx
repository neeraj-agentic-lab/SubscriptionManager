import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Building, ArrowLeft, Calendar, CheckCircle, XCircle, Edit, Users, DollarSign, Loader2, AlertCircle, Eye } from 'lucide-react';
import { tenantsAPI, type Tenant } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';
import { useTenantStore } from '../store/tenantStore';

export default function TenantDetailPage() {
  const { tenantId } = useParams<{ tenantId: string }>();
  const navigate = useNavigate();
  const { setSelectedTenant } = useTenantStore();
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    if (tenantId) {
      fetchTenantDetails();
    }
  }, [tenantId]);

  const fetchTenantDetails = async () => {
    if (!tenantId) return;

    try {
      setIsLoading(true);
      setError(null);
      const data = await tenantsAPI.getById(tenantId);
      setTenant(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load tenant details');
      console.error('Error fetching tenant:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSwitchToTenantView = () => {
    if (tenant) {
      // Convert API Tenant to Store Tenant format
      const storeTenant = {
        id: tenant.id,
        name: tenant.name,
        slug: tenant.slug,
        status: tenant.status as 'ACTIVE' | 'SUSPENDED',
        subscriptionCount: 0,
        mrr: 0,
        createdAt: tenant.createdAt,
      };
      setSelectedTenant(storeTenant);
      // Navigate to a tenant-scoped page (e.g., customers)
      navigate('/customers');
    }
  };

  if (isLoading) {
    return <LoadingOverlay message="Loading tenant details..." />;
  }

  if (error || !tenant) {
    return (
      <div className="p-8">
        <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm text-red-800">{error || 'Tenant not found'}</p>
          </div>
          <button
            onClick={() => navigate('/tenants')}
            className="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition"
          >
            Back to Tenants
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/tenants')}
            className="p-2 hover:bg-gray-100 rounded-lg transition"
          >
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{tenant.name}</h1>
            <p className="text-gray-600 mt-1">Tenant Details</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleSwitchToTenantView}
            className="flex items-center gap-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition font-medium"
          >
            <Eye className="w-5 h-5" />
            Switch to Tenant View
          </button>
          <button
            onClick={() => setShowEditModal(true)}
            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition font-medium"
          >
            <Edit className="w-5 h-5" />
            Edit Tenant
          </button>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Building className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Status</h3>
          </div>
          <span
            className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
              tenant.status === 'ACTIVE'
                ? 'bg-green-100 text-green-800'
                : 'bg-red-100 text-red-800'
            }`}
          >
            {tenant.status === 'ACTIVE' ? (
              <CheckCircle className="w-4 h-4 mr-1" />
            ) : (
              <XCircle className="w-4 h-4 mr-1" />
            )}
            {tenant.status}
          </span>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <Users className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Subscriptions</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">-</p>
          <p className="text-xs text-gray-400 mt-1">API integration pending</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <DollarSign className="w-5 h-5 text-green-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">MRR</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">$0</p>
          <p className="text-xs text-gray-400 mt-1">API integration pending</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
              <Users className="w-5 h-5 text-orange-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Customers</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">-</p>
          <p className="text-xs text-gray-400 mt-1">API integration pending</p>
        </div>
      </div>

      {/* Tenant Information */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-6">Tenant Information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="text-sm font-medium text-gray-600">Tenant ID</label>
            <p className="mt-1 text-gray-900 font-mono text-sm">{tenant.id}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Tenant Name</label>
            <p className="mt-1 text-gray-900">{tenant.name}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Slug</label>
            <p className="mt-1 text-gray-900 font-mono">{tenant.slug}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Status</label>
            <p className="mt-1">
              <span
                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  tenant.status === 'ACTIVE'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                }`}
              >
                {tenant.status}
              </span>
            </p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Created At</label>
            <div className="mt-1 flex items-center gap-2 text-gray-900">
              <Calendar className="w-4 h-4 text-gray-400" />
              {new Date(tenant.createdAt).toLocaleString()}
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Updated At</label>
            <div className="mt-1 flex items-center gap-2 text-gray-900">
              <Calendar className="w-4 h-4 text-gray-400" />
              {new Date(tenant.updatedAt).toLocaleString()}
            </div>
          </div>
        </div>
      </div>

      {/* Activity Section - Placeholder */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Recent Activity</h2>
        <div className="text-center py-12">
          <Calendar className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">No recent activity</p>
          <p className="text-sm text-gray-400 mt-1">Activity tracking coming soon</p>
        </div>
      </div>

      {/* Edit Modal */}
      {showEditModal && (
        <EditTenantModal
          tenant={tenant}
          onClose={() => setShowEditModal(false)}
          onSuccess={() => {
            setShowEditModal(false);
            fetchTenantDetails();
          }}
        />
      )}
    </div>
  );
}

// Edit Tenant Modal Component
interface EditTenantModalProps {
  tenant: Tenant;
  onClose: () => void;
  onSuccess: () => void;
}

function EditTenantModal({ tenant, onClose, onSuccess }: EditTenantModalProps) {
  const [name, setName] = useState(tenant.name);
  const [slug, setSlug] = useState(tenant.slug);
  const [status, setStatus] = useState(tenant.status);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    try {
      setIsSaving(true);
      await tenantsAPI.update(tenant.id, { name, slug, status });
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update tenant');
      console.error('Error updating tenant:', err);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-semibold text-gray-900">Edit Tenant</h3>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded transition"
          >
            <XCircle className="w-5 h-5 text-gray-600" />
          </button>
        </div>

        {error && (
          <div className="mb-4 flex items-center gap-3 p-3 bg-red-50 border border-red-200 rounded-lg">
            <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tenant Name *
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              placeholder="Enter tenant name"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Slug *
            </label>
            <input
              type="text"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none font-mono"
              placeholder="tenant-slug"
            />
            <p className="text-xs text-gray-500 mt-1">
              Lowercase letters, numbers, and hyphens only
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status *
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            >
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
            </select>
          </div>

          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50 flex items-center gap-2"
            >
              {isSaving && <Loader2 className="w-4 h-4 animate-spin" />}
              {isSaving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
