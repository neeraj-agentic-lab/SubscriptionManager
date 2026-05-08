import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, DollarSign, User, Package, CreditCard, Clock, AlertCircle, CheckCircle, XCircle, Ban } from 'lucide-react';
import { subscriptionsAPI, customersAPI, plansAPI } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';

export default function SubscriptionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [subscription, setSubscription] = useState<any>(null);
  const [customer, setCustomer] = useState<any>(null);
  const [plan, setPlan] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      fetchSubscriptionDetails();
    }
  }, [id]);

  const fetchSubscriptionDetails = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Fetch subscription
      const subData = await subscriptionsAPI.getById(id!);
      setSubscription(subData);

      // Fetch customer
      try {
        const customerData = await customersAPI.getById(subData.customerId);
        setCustomer(customerData);
      } catch (err) {
        console.error('Error fetching customer:', err);
      }

      // Fetch plan
      try {
        const planData = await plansAPI.getById(subData.planId);
        setPlan(planData);
      } catch (err) {
        console.error('Error fetching plan:', err);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load subscription details');
      console.error('Error fetching subscription:', err);
    } finally {
      setIsLoading(false);
    }
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

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE': return <CheckCircle className="w-5 h-5 text-green-600" />;
      case 'PAUSED': return <Clock className="w-5 h-5 text-yellow-600" />;
      case 'CANCELLED': return <XCircle className="w-5 h-5 text-red-600" />;
      case 'EXPIRED': return <Ban className="w-5 h-5 text-gray-600" />;
      default: return <AlertCircle className="w-5 h-5 text-gray-600" />;
    }
  };

  if (isLoading) {
    return <LoadingOverlay message="Loading subscription details..." />;
  }

  if (error) {
    return (
      <div className="p-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-red-900 mb-2">Error Loading Subscription</h2>
          <p className="text-red-700">{error}</p>
          <button
            onClick={() => navigate('/subscriptions')}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
          >
            Back to Subscriptions
          </button>
        </div>
      </div>
    );
  }

  if (!subscription) {
    return (
      <div className="p-8">
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
          <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Subscription Not Found</h2>
          <button
            onClick={() => navigate('/subscriptions')}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Back to Subscriptions
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
            onClick={() => navigate('/subscriptions')}
            className="p-2 hover:bg-gray-100 rounded-lg transition"
          >
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Subscription Details</h1>
            <p className="text-gray-600 mt-1 font-mono text-sm">{subscription.id}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {getStatusIcon(subscription.status)}
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(subscription.status)}`}>
            {subscription.status}
          </span>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Main Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Customer Information */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <User className="w-5 h-5 text-blue-600" />
              Customer Information
            </h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Name:</span>
                <span className="font-medium text-gray-900">
                  {customer ? `${customer.firstName || ''} ${customer.lastName || ''}`.trim() || customer.email : 'Loading...'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Email:</span>
                <span className="font-medium text-gray-900">{customer?.email || 'Loading...'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Customer ID:</span>
                <span className="font-mono text-sm text-gray-900">{subscription.customerId}</span>
              </div>
            </div>
          </div>

          {/* Plan Information */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Package className="w-5 h-5 text-purple-600" />
              Plan Information
            </h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Plan Name:</span>
                <span className="font-medium text-gray-900">{plan?.name || 'Loading...'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Price:</span>
                <span className="font-semibold text-gray-900">
                  ${plan ? (plan.amount / 100).toFixed(2) : '0.00'} / {plan?.billingInterval || 'month'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Plan ID:</span>
                <span className="font-mono text-sm text-gray-900">{subscription.planId}</span>
              </div>
            </div>
          </div>

          {/* Billing Information */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <CreditCard className="w-5 h-5 text-green-600" />
              Billing Information
            </h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600">Current Period Start:</span>
                <span className="font-medium text-gray-900">
                  {new Date(subscription.currentPeriodStart).toLocaleDateString()}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Current Period End:</span>
                <span className="font-medium text-gray-900">
                  {new Date(subscription.currentPeriodEnd).toLocaleDateString()}
                </span>
              </div>
              {subscription.canceledAt && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Canceled At:</span>
                  <span className="font-medium text-red-600">
                    {new Date(subscription.canceledAt).toLocaleDateString()}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column - Timeline & Actions */}
        <div className="space-y-6">
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
                    {new Date(subscription.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-2 h-2 bg-green-600 rounded-full mt-2"></div>
                <div>
                  <p className="text-sm font-medium text-gray-900">Last Updated</p>
                  <p className="text-xs text-gray-600">
                    {new Date(subscription.updatedAt).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Stats */}
          <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl border border-blue-200 p-6">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Quick Stats</h3>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Status:</span>
                <span className="font-semibold text-gray-900">{subscription.status}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Tenant ID:</span>
                <span className="font-mono text-xs text-gray-900">{subscription.tenantId}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
