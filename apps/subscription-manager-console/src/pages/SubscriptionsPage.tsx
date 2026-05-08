import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Repeat, Search, Plus, MoreVertical, Calendar, DollarSign, User, CheckCircle, Clock, XCircle, AlertCircle, Ban, RotateCcw } from 'lucide-react';
import { subscriptionsAPI, customersAPI, plansAPI, type Subscription as APISubscription } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';
import { useTenantStore } from '../store/tenantStore';

interface Subscription {
  id: string;
  customerName: string;
  planName: string;
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'EXPIRED';
  price: number;
  billingCycle: 'MONTHLY' | 'YEARLY';
  startDate: string;
  nextBillingDate?: string;
  endDate?: string;
}

export default function SubscriptionsPage() {
  const navigate = useNavigate();
  const { selectedTenant } = useTenantStore();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'EXPIRED'>('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedSubscription, setSelectedSubscription] = useState<Subscription | null>(null);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  useEffect(() => {
    fetchSubscriptions();
  }, [selectedTenant, currentPage]);

  const fetchSubscriptions = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await subscriptionsAPI.getAll(currentPage, 20);
      
      // Fetch customer and plan names for each subscription
      const mappedSubscriptions = await Promise.all(
        response.content.map(async (s: APISubscription) => {
          let customerName = 'Unknown Customer';
          let planName = 'Unknown Plan';
          let price = 0;
          let billingCycle: 'MONTHLY' | 'YEARLY' = 'MONTHLY';
          
          try {
            const customer = await customersAPI.getById(s.customerId);
            customerName = `${customer.firstName || ''} ${customer.lastName || ''}`.trim() || customer.email;
          } catch (err) {
            console.error('Error fetching customer:', err);
          }
          
          try {
            const plan = await plansAPI.getById(s.planId);
            planName = plan.name;
            price = plan.amount / 100;
            billingCycle = plan.billingInterval.toUpperCase() as 'MONTHLY' | 'YEARLY';
          } catch (err) {
            console.error('Error fetching plan:', err);
          }
          
          return {
            id: s.id,
            customerName,
            planName,
            status: s.status as 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'EXPIRED',
            price,
            billingCycle,
            startDate: s.currentPeriodStart,
            nextBillingDate: s.currentPeriodEnd,
            endDate: s.canceledAt,
          };
        })
      );
      
      setSubscriptions(mappedSubscriptions);
      setTotalPages(response.totalPages);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load subscriptions');
      console.error('Error fetching subscriptions:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancelSubscription = async (subscriptionId: string) => {
    try {
      await subscriptionsAPI.cancel(subscriptionId);
      fetchSubscriptions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to cancel subscription');
    }
  };

  const handleReactivateSubscription = async (subscriptionId: string) => {
    try {
      await subscriptionsAPI.reactivate(subscriptionId);
      fetchSubscriptions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reactivate subscription');
    }
  };

  const filteredSubscriptions = subscriptions.filter((sub) => {
    const matchesSearch = 
      sub.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      sub.planName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      sub.id.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || sub.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const stats = {
    total: subscriptions.length,
    active: subscriptions.filter(s => s.status === 'ACTIVE').length,
    paused: subscriptions.filter(s => s.status === 'PAUSED').length,
    cancelled: subscriptions.filter(s => s.status === 'CANCELLED').length,
    mrr: subscriptions
      .filter(s => s.status === 'ACTIVE')
      .reduce((sum, s) => sum + (s.billingCycle === 'MONTHLY' ? s.price : s.price / 12), 0),
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      case 'EXPIRED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="p-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            {selectedTenant ? `${selectedTenant.name} - Subscriptions` : 'Subscriptions'}
          </h1>
          <p className="text-gray-600 mt-1">
            {selectedTenant ? `Manage subscriptions for ${selectedTenant.name}` : 'Manage customer subscriptions'}
          </p>
        </div>
        <button 
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition font-medium"
        >
          <Plus className="w-5 h-5" />
          New Subscription
        </button>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
          <div className="flex-1">
            <p className="text-sm text-red-800">{error}</p>
          </div>
          <button
            onClick={fetchSubscriptions}
            className="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Repeat className="w-5 h-5 text-blue-600" />
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
            <h3 className="text-sm font-medium text-gray-600">Active</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.active}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-yellow-100 rounded-lg flex items-center justify-center">
              <Clock className="w-5 h-5 text-yellow-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Paused</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.paused}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
              <XCircle className="w-5 h-5 text-red-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Cancelled</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.cancelled}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <DollarSign className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">MRR</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">${Math.round(stats.mrr).toLocaleString()}</p>
        </div>
      </div>

      {/* Filters and Search */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by customer, plan, or ID..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            />
          </div>

          {/* Status Filter */}
          <div className="flex gap-2 flex-wrap">
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                statusFilter === 'ALL'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setStatusFilter('ACTIVE')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                statusFilter === 'ACTIVE'
                  ? 'bg-green-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Active
            </button>
            <button
              onClick={() => setStatusFilter('PAUSED')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                statusFilter === 'PAUSED'
                  ? 'bg-yellow-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Paused
            </button>
            <button
              onClick={() => setStatusFilter('CANCELLED')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                statusFilter === 'CANCELLED'
                  ? 'bg-red-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Cancelled
            </button>
          </div>
        </div>
      </div>

      {/* Subscriptions Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Subscription ID
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Customer
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Plan
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Status
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Price
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Start Date
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Next Billing
                </th>
                <th className="text-right px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredSubscriptions.map((subscription) => (
                <tr key={subscription.id} className="hover:bg-gray-50 transition">
                  <td className="px-6 py-4">
                    <button
                      onClick={() => navigate(`/subscriptions/${subscription.id}`)}
                      className="font-mono text-sm text-blue-600 hover:text-blue-800 hover:underline transition"
                    >
                      {subscription.id}
                    </button>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <User className="w-4 h-4 text-gray-400" />
                      <span className="font-medium text-gray-900">{subscription.customerName}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="text-sm text-gray-900">{subscription.planName}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(subscription.status)}`}>
                      {subscription.status}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1">
                      <DollarSign className="w-4 h-4 text-gray-400" />
                      <span className="font-semibold text-gray-900">${subscription.price}</span>
                      <span className="text-sm text-gray-500">/{subscription.billingCycle === 'MONTHLY' ? 'mo' : 'yr'}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                      <Calendar className="w-4 h-4 text-gray-400" />
                      {new Date(subscription.startDate).toLocaleDateString()}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    {subscription.nextBillingDate ? (
                      <div className="flex items-center gap-2 text-sm text-gray-600">
                        <Calendar className="w-4 h-4 text-gray-400" />
                        {new Date(subscription.nextBillingDate).toLocaleDateString()}
                      </div>
                    ) : (
                      <span className="text-sm text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="relative">
                      <button 
                        onClick={() => setOpenMenuId(openMenuId === subscription.id ? null : subscription.id)}
                        className="p-2 hover:bg-gray-100 rounded-lg transition"
                      >
                        <MoreVertical className="w-5 h-5 text-gray-600" />
                      </button>
                      
                      {openMenuId === subscription.id && (
                        <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                          {subscription.status === 'ACTIVE' && (
                            <button
                              onClick={() => {
                                handleCancelSubscription(subscription.id);
                                setOpenMenuId(null);
                              }}
                              className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
                            >
                              <Ban className="w-4 h-4" />
                              Cancel Subscription
                            </button>
                          )}
                          {subscription.status === 'CANCELLED' && (
                            <button
                              onClick={() => {
                                handleReactivateSubscription(subscription.id);
                                setOpenMenuId(null);
                              }}
                              className="w-full px-4 py-2 text-left text-sm text-green-600 hover:bg-green-50 flex items-center gap-2"
                            >
                              <RotateCcw className="w-4 h-4" />
                              Reactivate Subscription
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredSubscriptions.length === 0 && (
          <div className="text-center py-12">
            <Repeat className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500">No subscriptions found</p>
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

      {/* Create Modal Placeholder */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">Create Subscription</h3>
            <p className="text-gray-600 mb-4">Create subscription modal - API integrated</p>
            <button
              onClick={() => setShowCreateModal(false)}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isLoading && <LoadingOverlay message="Loading subscriptions..." />}
    </div>
  );
}
