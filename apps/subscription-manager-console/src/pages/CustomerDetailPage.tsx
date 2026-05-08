import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Mail, Calendar, User, CreditCard, Package, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import { customersAPI, subscriptionsAPI } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';

export default function CustomerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [customer, setCustomer] = useState<any>(null);
  const [subscriptions, setSubscriptions] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      fetchCustomerDetails();
    }
  }, [id]);

  const fetchCustomerDetails = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Fetch customer
      const customerData = await customersAPI.getById(id!);
      setCustomer(customerData);

      // Fetch customer's subscriptions
      try {
        const subsResponse = await subscriptionsAPI.getAll(0, 100);
        const customerSubs = subsResponse.content.filter((sub: any) => sub.customerId === id);
        setSubscriptions(customerSubs);
      } catch (err) {
        console.error('Error fetching subscriptions:', err);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load customer details');
      console.error('Error fetching customer:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    return status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
  };

  const getSubscriptionStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  if (isLoading) {
    return <LoadingOverlay message="Loading customer details..." />;
  }

  if (error) {
    return (
      <div className="p-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-red-900 mb-2">Error Loading Customer</h2>
          <p className="text-red-700">{error}</p>
          <button
            onClick={() => navigate('/customers')}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
          >
            Back to Customers
          </button>
        </div>
      </div>
    );
  }

  if (!customer) {
    return (
      <div className="p-8">
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
          <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Customer Not Found</h2>
          <button
            onClick={() => navigate('/customers')}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Back to Customers
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
            onClick={() => navigate('/customers')}
            className="p-2 hover:bg-gray-100 rounded-lg transition"
          >
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              {`${customer.firstName || ''} ${customer.lastName || ''}`.trim() || customer.email}
            </h1>
            <p className="text-gray-600 mt-1 font-mono text-sm">{customer.id}</p>
          </div>
        </div>
        <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(customer.status)}`}>
          {customer.status === 'ACTIVE' ? <CheckCircle className="w-4 h-4 mr-1" /> : <XCircle className="w-4 h-4 mr-1" />}
          {customer.status}
        </span>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Customer Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Contact Information */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <User className="w-5 h-5 text-blue-600" />
              Contact Information
            </h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Email:</span>
                <div className="flex items-center gap-2">
                  <Mail className="w-4 h-4 text-gray-400" />
                  <span className="font-medium text-gray-900">{customer.email}</span>
                </div>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">First Name:</span>
                <span className="font-medium text-gray-900">{customer.firstName || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Last Name:</span>
                <span className="font-medium text-gray-900">{customer.lastName || 'N/A'}</span>
              </div>
              {customer.externalCustomerId && (
                <div className="flex justify-between">
                  <span className="text-gray-600">External ID:</span>
                  <span className="font-mono text-sm text-gray-900">{customer.externalCustomerId}</span>
                </div>
              )}
            </div>
          </div>

          {/* Subscriptions */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Package className="w-5 h-5 text-purple-600" />
              Subscriptions ({subscriptions.length})
            </h2>
            {subscriptions.length > 0 ? (
              <div className="space-y-3">
                {subscriptions.map((sub) => (
                  <div key={sub.id} className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition">
                    <div className="flex items-center justify-between mb-2">
                      <button
                        onClick={() => navigate(`/subscriptions/${sub.id}`)}
                        className="font-mono text-sm text-blue-600 hover:text-blue-800 hover:underline"
                      >
                        {sub.id}
                      </button>
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${getSubscriptionStatusColor(sub.status)}`}>
                        {sub.status}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600 space-y-1">
                      <div className="flex justify-between">
                        <span>Plan ID:</span>
                        <span className="font-mono text-xs">{sub.planId}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Started:</span>
                        <span>{new Date(sub.currentPeriodStart).toLocaleDateString()}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500 text-center py-4">No subscriptions found</p>
            )}
          </div>
        </div>

        {/* Right Column - Stats & Timeline */}
        <div className="space-y-6">
          {/* Quick Stats */}
          <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl border border-blue-200 p-6">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Quick Stats</h3>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-gray-600 text-sm">Active Subscriptions:</span>
                <span className="text-2xl font-bold text-blue-600">
                  {subscriptions.filter(s => s.status === 'ACTIVE').length}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600 text-sm">Total Subscriptions:</span>
                <span className="text-xl font-semibold text-gray-900">{subscriptions.length}</span>
              </div>
            </div>
          </div>

          {/* Timeline */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Calendar className="w-5 h-5 text-orange-600" />
              Timeline
            </h2>
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <div className="w-2 h-2 bg-blue-600 rounded-full mt-2"></div>
                <div>
                  <p className="text-sm font-medium text-gray-900">Created</p>
                  <p className="text-xs text-gray-600">
                    {new Date(customer.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-2 h-2 bg-green-600 rounded-full mt-2"></div>
                <div>
                  <p className="text-sm font-medium text-gray-900">Last Updated</p>
                  <p className="text-xs text-gray-600">
                    {new Date(customer.updatedAt).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Customer Type */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Customer Details</h3>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Type:</span>
                <span className="font-medium text-gray-900">{customer.customerType || 'N/A'}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Tenant ID:</span>
                <span className="font-mono text-xs text-gray-900">{customer.tenantId}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
