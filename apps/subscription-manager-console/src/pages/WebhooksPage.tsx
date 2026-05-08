import { useState, useEffect } from 'react';
import { Webhook, Search, Plus, MoreVertical, CheckCircle, XCircle, Clock, AlertCircle, Edit, Trash2 } from 'lucide-react';
import { webhooksAPI, type Webhook as APIWebhook } from '../lib/api';
import LoadingOverlay from '../components/common/LoadingOverlay';
import { useTenantStore } from '../store/tenantStore';

interface WebhookEndpoint {
  id: string;
  url: string;
  events: string[];
  status: 'ACTIVE' | 'INACTIVE';
  successRate: number;
  lastDelivery?: string;
  createdAt: string;
}

export default function WebhooksPage() {
  const { selectedTenant } = useTenantStore();
  const [webhooks, setWebhooks] = useState<WebhookEndpoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedWebhook, setSelectedWebhook] = useState<WebhookEndpoint | null>(null);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  useEffect(() => {
    fetchWebhooks();
  }, [selectedTenant]);

  const fetchWebhooks = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await webhooksAPI.getAll();
      
      // Map API webhooks to UI format
      const mappedWebhooks: WebhookEndpoint[] = data.map((w: APIWebhook) => ({
        id: w.id,
        url: w.url,
        events: w.events,
        status: w.status as 'ACTIVE' | 'INACTIVE',
        successRate: 0, // Placeholder - would need webhook delivery stats
        lastDelivery: undefined, // Placeholder - would need webhook delivery stats
        createdAt: w.createdAt,
      }));
      
      setWebhooks(mappedWebhooks);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load webhooks');
      console.error('Error fetching webhooks:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteWebhook = async () => {
    if (!selectedWebhook) return;
    try {
      await webhooksAPI.delete(selectedWebhook.id);
      setShowDeleteModal(false);
      setSelectedWebhook(null);
      fetchWebhooks();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete webhook');
    }
  };

  const filteredWebhooks = webhooks.filter((webhook) => {
    const matchesSearch = 
      webhook.url.toLowerCase().includes(searchQuery.toLowerCase()) ||
      webhook.events.some(e => e.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesStatus = statusFilter === 'ALL' || webhook.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const stats = {
    total: webhooks.length,
    active: webhooks.filter(w => w.status === 'ACTIVE').length,
    inactive: webhooks.filter(w => w.status === 'INACTIVE').length,
    avgSuccessRate: webhooks.length > 0 ? webhooks.reduce((sum, w) => sum + w.successRate, 0) / webhooks.length : 0,
  };

  return (
    <div className="p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            {selectedTenant ? `${selectedTenant.name} - Webhooks` : 'Webhooks'}
          </h1>
          <p className="text-gray-600 mt-1">
            {selectedTenant ? `Manage webhooks for ${selectedTenant.name}` : 'Manage webhook endpoints and event subscriptions'}
          </p>
        </div>
        <button 
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition font-medium"
        >
          <Plus className="w-5 h-5" />
          Add Webhook
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
            onClick={fetchWebhooks}
            className="px-3 py-1 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition"
          >
            Retry
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Webhook className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Total Webhooks</h3>
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
            <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
              <XCircle className="w-5 h-5 text-red-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Inactive</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.inactive}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <CheckCircle className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Avg Success Rate</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats.avgSuccessRate.toFixed(1)}%</p>
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
              placeholder="Search webhooks by URL or event..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                statusFilter === 'ALL' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setStatusFilter('ACTIVE')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                statusFilter === 'ACTIVE' ? 'bg-green-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Active
            </button>
            <button
              onClick={() => setStatusFilter('INACTIVE')}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                statusFilter === 'INACTIVE' ? 'bg-red-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Inactive
            </button>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        {filteredWebhooks.map((webhook) => (
          <div key={webhook.id} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-start gap-4 flex-1">
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
                  <Webhook className="w-6 h-6 text-blue-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="font-semibold text-gray-900 truncate">{webhook.url}</h3>
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      webhook.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {webhook.status}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2 mb-3">
                    {webhook.events.map((event) => (
                      <span key={event} className="px-2 py-1 bg-gray-100 text-gray-700 text-xs rounded">
                        {event}
                      </span>
                    ))}
                  </div>
                  <div className="flex items-center gap-6 text-sm text-gray-600">
                    <div className="flex items-center gap-2">
                      <CheckCircle className="w-4 h-4 text-green-500" />
                      <span>Success Rate: {webhook.successRate}%</span>
                    </div>
                    {webhook.lastDelivery && (
                      <div className="flex items-center gap-2">
                        <Clock className="w-4 h-4 text-gray-400" />
                        <span>Last: {new Date(webhook.lastDelivery).toLocaleString()}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <div className="relative">
                <button 
                  onClick={() => setOpenMenuId(openMenuId === webhook.id ? null : webhook.id)}
                  className="p-2 hover:bg-gray-100 rounded-lg transition"
                >
                  <MoreVertical className="w-5 h-5 text-gray-600" />
                </button>
                
                {openMenuId === webhook.id && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                    <button
                      onClick={() => {
                        setSelectedWebhook(webhook);
                        setShowEditModal(true);
                        setOpenMenuId(null);
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
                    >
                      <Edit className="w-4 h-4" />
                      Edit Webhook
                    </button>
                    <button
                      onClick={() => {
                        setSelectedWebhook(webhook);
                        setShowDeleteModal(true);
                        setOpenMenuId(null);
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
                    >
                      <Trash2 className="w-4 h-4" />
                      Delete Webhook
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {filteredWebhooks.length === 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center">
          <Webhook className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">No webhooks found</p>
          <p className="text-sm text-gray-400 mt-1">Try adjusting your search or filters</p>
        </div>
      )}

      {/* Delete Modal */}
      {showDeleteModal && selectedWebhook && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                <AlertCircle className="w-6 h-6 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Delete Webhook</h3>
                <p className="text-sm text-gray-600">This action cannot be undone</p>
              </div>
            </div>
            
            <p className="text-gray-700 mb-6">
              Are you sure you want to delete webhook <strong>{selectedWebhook.url}</strong>?
            </p>

            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setShowDeleteModal(false);
                  setSelectedWebhook(null);
                }}
                className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteWebhook}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
              >
                Delete Webhook
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create/Edit Modal Placeholders */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">Add Webhook</h3>
            <p className="text-gray-600 mb-4">Create webhook modal - API integrated</p>
            <button
              onClick={() => setShowCreateModal(false)}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {showEditModal && selectedWebhook && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">Edit Webhook</h3>
            <p className="text-gray-600 mb-4">Edit webhook modal - API integrated</p>
            <button
              onClick={() => {
                setShowEditModal(false);
                setSelectedWebhook(null);
              }}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isLoading && <LoadingOverlay message="Loading webhooks..." />}
    </div>
  );
}
