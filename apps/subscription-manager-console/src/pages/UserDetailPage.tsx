import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { User, ArrowLeft, Calendar, CheckCircle, XCircle, Edit, Mail, Shield, Loader2, AlertCircle, Building } from 'lucide-react';
import { usersAPI, userTenantsAPI, tenantsAPI, type User as UserType, type Tenant } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';

export default function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const [user, setUser] = useState<UserType | null>(null);
  const [userTenants, setUserTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    if (userId) {
      fetchUserDetails();
    }
  }, [userId]);

  const fetchUserDetails = async () => {
    if (!userId) return;

    try {
      setIsLoading(true);
      setError(null);
      
      // Fetch user details
      const userData = await usersAPI.getById(userId);
      setUser(userData);

      // Fetch user's tenant assignments
      try {
        const tenantAssignments = await userTenantsAPI.getUserTenants(userId);
        const tenantDetails = await Promise.all(
          tenantAssignments.map(async (assignment) => {
            const tenant = await tenantsAPI.getById(assignment.tenantId);
            return tenant;
          })
        );
        setUserTenants(tenantDetails);
      } catch (err) {
        console.log('No tenant assignments for user');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load user details');
      console.error('Error fetching user:', err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingOverlay message="Loading user details..." />;
  }

  if (error || !user) {
    return (
      <div className="p-8">
        <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm text-red-800">{error || 'User not found'}</p>
          </div>
          <button
            onClick={() => navigate('/users')}
            className="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition"
          >
            Back to Users
          </button>
        </div>
      </div>
    );
  }

  const getRoleBadgeColor = (role: string) => {
    switch (role) {
      case 'SUPER_ADMIN':
        return 'bg-purple-100 text-purple-800';
      case 'TENANT_ADMIN':
        return 'bg-blue-100 text-blue-800';
      case 'TENANT_USER':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'SUPER_ADMIN':
        return 'Super Admin';
      case 'TENANT_ADMIN':
        return 'Tenant Admin';
      case 'TENANT_USER':
        return 'Tenant User';
      default:
        return role;
    }
  };

  return (
    <div className="p-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/users')}
            className="p-2 hover:bg-gray-100 rounded-lg transition"
          >
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{user.firstName} {user.lastName}</h1>
            <p className="text-gray-600 mt-1">User Details</p>
          </div>
        </div>
        <button
          onClick={() => setShowEditModal(true)}
          className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition font-medium"
        >
          <Edit className="w-5 h-5" />
          Edit User
        </button>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <User className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Status</h3>
          </div>
          <span
            className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
              user.status === 'ACTIVE'
                ? 'bg-green-100 text-green-800'
                : 'bg-red-100 text-red-800'
            }`}
          >
            {user.status === 'ACTIVE' ? (
              <CheckCircle className="w-4 h-4 mr-1" />
            ) : (
              <XCircle className="w-4 h-4 mr-1" />
            )}
            {user.status}
          </span>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <Shield className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Role</h3>
          </div>
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getRoleBadgeColor(user.role)}`}>
            {getRoleLabel(user.role)}
          </span>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <Building className="w-5 h-5 text-green-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Tenants</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{userTenants.length}</p>
          <p className="text-xs text-gray-400 mt-1">
            {user.role === 'SUPER_ADMIN' ? 'Global Access' : 'Assigned tenants'}
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
              <Calendar className="w-5 h-5 text-orange-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Last Login</h3>
          </div>
          <p className="text-sm font-medium text-gray-900">
            {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never'}
          </p>
        </div>
      </div>

      {/* User Information */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-6">User Information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="text-sm font-medium text-gray-600">User ID</label>
            <p className="mt-1 text-gray-900 font-mono text-sm">{user.id}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Email</label>
            <div className="mt-1 flex items-center gap-2 text-gray-900">
              <Mail className="w-4 h-4 text-gray-400" />
              {user.email}
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">First Name</label>
            <p className="mt-1 text-gray-900">{user.firstName}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Last Name</label>
            <p className="mt-1 text-gray-900">{user.lastName}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Role</label>
            <p className="mt-1">
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getRoleBadgeColor(user.role)}`}>
                {getRoleLabel(user.role)}
              </span>
            </p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Status</label>
            <p className="mt-1">
              <span
                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  user.status === 'ACTIVE'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                }`}
              >
                {user.status}
              </span>
            </p>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Created At</label>
            <div className="mt-1 flex items-center gap-2 text-gray-900">
              <Calendar className="w-4 h-4 text-gray-400" />
              {new Date(user.createdAt).toLocaleString()}
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-600">Updated At</label>
            <div className="mt-1 flex items-center gap-2 text-gray-900">
              <Calendar className="w-4 h-4 text-gray-400" />
              {new Date(user.updatedAt).toLocaleString()}
            </div>
          </div>
        </div>
      </div>

      {/* Tenant Assignments */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Tenant Assignments</h2>
        {user.role === 'SUPER_ADMIN' ? (
          <div className="text-center py-8">
            <Shield className="w-12 h-12 text-purple-300 mx-auto mb-3" />
            <p className="text-gray-500">Super Admin - Global Access</p>
            <p className="text-sm text-gray-400 mt-1">This user has access to all tenants</p>
          </div>
        ) : userTenants.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {userTenants.map((tenant) => (
              <div
                key={tenant.id}
                className="border border-gray-200 rounded-lg p-4 hover:border-blue-300 hover:bg-blue-50 transition cursor-pointer"
                onClick={() => navigate(`/tenants/${tenant.id}`)}
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                    <Building className="w-5 h-5 text-blue-600" />
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">{tenant.name}</p>
                    <p className="text-xs text-gray-500">{tenant.slug}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8">
            <Building className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500">No tenant assignments</p>
            <p className="text-sm text-gray-400 mt-1">This user is not assigned to any tenants</p>
          </div>
        )}
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

      {/* Edit Modal - Reuse from UsersPage */}
      {showEditModal && (
        <EditUserModal
          user={user}
          onClose={() => setShowEditModal(false)}
          onSuccess={() => {
            setShowEditModal(false);
            fetchUserDetails();
          }}
        />
      )}
    </div>
  );
}

// Edit User Modal Component (copied from UsersPage for consistency)
interface EditUserModalProps {
  user: UserType;
  onClose: () => void;
  onSuccess: () => void;
}

function EditUserModal({ user, onClose, onSuccess }: EditUserModalProps) {
  const [firstName, setFirstName] = useState(user.firstName);
  const [lastName, setLastName] = useState(user.lastName);
  const [role, setRole] = useState(user.role);
  const [status, setStatus] = useState(user.status);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    try {
      setIsSaving(true);
      await usersAPI.update(user.id, { firstName, lastName, role, status });
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update user');
      console.error('Error updating user:', err);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-semibold text-gray-900">Edit User</h3>
          <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded transition">
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
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">First Name *</label>
              <input
                type="text"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Last Name *</label>
              <input
                type="text"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Role *</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            >
              <option value="SUPER_ADMIN">Super Admin</option>
              <option value="TENANT_ADMIN">Tenant Admin</option>
              <option value="TENANT_USER">Tenant User</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status *</label>
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
