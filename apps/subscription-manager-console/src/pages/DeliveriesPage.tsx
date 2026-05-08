import { useState, useEffect } from 'react';
import { Package, Search, Calendar, CheckCircle, Clock, XCircle, AlertCircle } from 'lucide-react';
import { deliveriesAPI, subscriptionsAPI, type Delivery as APIDelivery } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';
import { useTenantStore } from '../store/tenantStore';

interface Delivery {
  id: string;
  subscriptionId: string;
  customerName: string;
  planName: string;
  status: 'PENDING' | 'DELIVERED' | 'FAILED' | 'SCHEDULED';
  scheduledDate: string;
  deliveredDate?: string;
  productName: string;
}

export default function DeliveriesPage() {
  const { selectedTenant } = useTenantStore();
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PENDING' | 'DELIVERED' | 'FAILED' | 'SCHEDULED'>('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchDeliveries();
  }, [selectedTenant, currentPage]);

  const fetchDeliveries = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await deliveriesAPI.getAll(currentPage, 20);
      
      // Map API deliveries to UI format
      const mappedDeliveries: Delivery[] = response.content.map((d: APIDelivery) => ({
        id: d.id,
        subscriptionId: d.subscriptionId,
        customerName: 'Customer', // Placeholder - would need subscription lookup
        planName: 'Plan', // Placeholder - would need subscription lookup
        status: d.status as 'PENDING' | 'DELIVERED' | 'FAILED' | 'SCHEDULED',
        scheduledDate: d.scheduledFor,
        deliveredDate: d.deliveredAt,
        productName: 'Product', // Placeholder - would need product info
      }));
      
      setDeliveries(mappedDeliveries);
      setTotalPages(response.totalPages);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load deliveries');
      console.error('Error fetching deliveries:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const filteredDeliveries = deliveries.filter((delivery) => {
    const matchesSearch = 
      delivery.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      delivery.productName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      delivery.id.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || delivery.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const stats = {
    total: deliveries.length,
    delivered: deliveries.filter(d => d.status === 'DELIVERED').length,
    pending: deliveries.filter(d => d.status === 'PENDING').length,
    scheduled: deliveries.filter(d => d.status === 'SCHEDULED').length,
    failed: deliveries.filter(d => d.status === 'FAILED').length,
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DELIVERED': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'SCHEDULED': return 'bg-blue-100 text-blue-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'DELIVERED': return CheckCircle;
      case 'PENDING': return AlertCircle;
      case 'SCHEDULED': return Clock;
      case 'FAILED': return XCircle;
      default: return Package;
    }
  };

  return (
    <div className="p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            {selectedTenant ? `${selectedTenant.name} - Deliveries` : 'Deliveries'}
          </h1>
          <p className="text-gray-600 mt-1">
            {selectedTenant ? `Track deliveries for ${selectedTenant.name}` : 'Track subscription deliveries and fulfillment'}
          </p>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm text-red-800">{error}</p>
          </div>
          <button
            onClick={fetchDeliveries}
            className="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition"
          >
            Retry
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Package className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Total</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <CheckCircle className="w-5 h-5 text-green-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Delivered</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.delivered}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-yellow-100 rounded-lg flex items-center justify-center">
              <AlertCircle className="w-5 h-5 text-yellow-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Pending</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.pending}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Clock className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Scheduled</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.scheduled}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
              <XCircle className="w-5 h-5 text-red-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Failed</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.failed}</p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search deliveries..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            />
          </div>
          <div className="flex gap-2 flex-wrap">
            {['ALL', 'DELIVERED', 'PENDING', 'SCHEDULED', 'FAILED'].map((status) => (
              <button
                key={status}
                onClick={() => setStatusFilter(status as any)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                  statusFilter === status
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {status.charAt(0) + status.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase">Delivery ID</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase">Customer</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase">Product</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase">Status</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase">Scheduled</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase">Delivered</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredDeliveries.map((delivery) => {
                const StatusIcon = getStatusIcon(delivery.status);
                return (
                  <tr key={delivery.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4">
                      <span className="font-mono text-sm text-gray-900">{delivery.id}</span>
                    </td>
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-medium text-gray-900">{delivery.customerName}</p>
                        <p className="text-sm text-gray-500">{delivery.planName}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-sm text-gray-900">{delivery.productName}</span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(delivery.status)}`}>
                        <StatusIcon className="w-3 h-3" />
                        {delivery.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2 text-sm text-gray-600">
                        <Calendar className="w-4 h-4 text-gray-400" />
                        {new Date(delivery.scheduledDate).toLocaleDateString()}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {delivery.deliveredDate ? (
                        <div className="flex items-center gap-2 text-sm text-gray-600">
                          <Calendar className="w-4 h-4 text-gray-400" />
                          {new Date(delivery.deliveredDate).toLocaleDateString()}
                        </div>
                      ) : (
                        <span className="text-sm text-gray-400">-</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {filteredDeliveries.length === 0 && (
          <div className="text-center py-12">
            <Package className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500">No deliveries found</p>
            <p className="text-sm text-gray-400 mt-1">Try adjusting your search or filters</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <p className="text-sm text-gray-600">
            Page {currentPage + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
              disabled={currentPage === 0}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <button
              onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
              disabled={currentPage >= totalPages - 1}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isLoading && <LoadingOverlay message="Loading deliveries..." />}
    </div>
  );
}
