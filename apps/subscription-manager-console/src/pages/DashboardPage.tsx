import { TrendingUp, TrendingDown, Users, Building, DollarSign, Repeat } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { useTenantStore } from '../store/tenantStore';

interface MetricCardProps {
  title: string;
  value: string | number;
  change: number;
  icon: React.ElementType;
  iconBg: string;
  iconColor: string;
}

function MetricCard({ title, value, change, icon: Icon, iconBg, iconColor }: MetricCardProps) {
  const isPositive = change >= 0;
  
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition">
      <div className="flex items-center justify-between mb-4">
        <div className={`w-12 h-12 ${iconBg} rounded-lg flex items-center justify-center`}>
          <Icon className={`w-6 h-6 ${iconColor}`} />
        </div>
        <div className={`flex items-center gap-1 text-sm font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
          {isPositive ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
          {Math.abs(change)}%
        </div>
      </div>
      <h3 className="text-gray-600 text-sm font-medium mb-1">{title}</h3>
      <p className="text-3xl font-bold text-gray-900">{value}</p>
    </div>
  );
}

function SuperAdminDashboard() {
  return (
    <div className="space-y-6">
      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          title="Total Tenants"
          value={45}
          change={12.5}
          icon={Building}
          iconBg="bg-blue-100"
          iconColor="text-blue-600"
        />
        <MetricCard
          title="Total Users"
          value={234}
          change={8.2}
          icon={Users}
          iconBg="bg-purple-100"
          iconColor="text-purple-600"
        />
        <MetricCard
          title="Platform Revenue"
          value="$125K"
          change={15.3}
          icon={DollarSign}
          iconBg="bg-green-100"
          iconColor="text-green-600"
        />
        <MetricCard
          title="Active Subscriptions"
          value="1,523"
          change={6.7}
          icon={Repeat}
          iconBg="bg-orange-100"
          iconColor="text-orange-600"
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Tenant Growth Chart */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Tenant Growth</h3>
          <div className="h-64 flex items-end justify-between gap-2">
            {[32, 38, 42, 45, 48, 52, 58, 62, 68, 72, 78, 85].map((height, i) => (
              <div key={i} className="flex-1 bg-blue-500 rounded-t hover:bg-blue-600 transition cursor-pointer" style={{ height: `${height}%` }}></div>
            ))}
          </div>
          <div className="flex justify-between mt-4 text-xs text-gray-500">
            <span>Jan</span>
            <span>Dec</span>
          </div>
        </div>

        {/* Revenue by Tenant */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Top Tenants by Revenue</h3>
          <div className="space-y-4">
            {[
              { name: 'Enterprise Co', revenue: 45600, color: 'bg-blue-500' },
              { name: 'Global Solutions', revenue: 24800, color: 'bg-purple-500' },
              { name: 'Acme Corp', revenue: 12450, color: 'bg-green-500' },
              { name: 'TechStart Inc', revenue: 8200, color: 'bg-orange-500' },
            ].map((tenant) => (
              <div key={tenant.name}>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm font-medium text-gray-700">{tenant.name}</span>
                  <span className="text-sm font-semibold text-gray-900">${tenant.revenue.toLocaleString()}/mo</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div className={`${tenant.color} h-2 rounded-full`} style={{ width: `${(tenant.revenue / 45600) * 100}%` }}></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
        <div className="space-y-4">
          {[
            { type: 'tenant', action: 'New tenant created', name: 'Startup Labs', time: '2 hours ago', color: 'bg-blue-100 text-blue-600' },
            { type: 'subscription', action: 'New subscription', name: 'Enterprise Co - Premium Plan', time: '4 hours ago', color: 'bg-green-100 text-green-600' },
            { type: 'user', action: 'New user registered', name: 'john@acme.com', time: '6 hours ago', color: 'bg-purple-100 text-purple-600' },
            { type: 'system', action: 'System update completed', name: 'v2.4.1', time: '1 day ago', color: 'bg-gray-100 text-gray-600' },
          ].map((activity, i) => (
            <div key={i} className="flex items-center gap-4 p-3 hover:bg-gray-50 rounded-lg transition">
              <div className={`w-10 h-10 ${activity.color} rounded-lg flex items-center justify-center font-semibold text-sm`}>
                {activity.type[0].toUpperCase()}
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">{activity.action}</p>
                <p className="text-xs text-gray-500">{activity.name}</p>
              </div>
              <span className="text-xs text-gray-400">{activity.time}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function TenantAdminDashboard() {
  const { selectedTenant } = useTenantStore();

  return (
    <div className="space-y-6">
      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          title="Total Customers"
          value={156}
          change={8.5}
          icon={Users}
          iconBg="bg-blue-100"
          iconColor="text-blue-600"
        />
        <MetricCard
          title="Active Subscriptions"
          value={89}
          change={12.3}
          icon={Repeat}
          iconBg="bg-purple-100"
          iconColor="text-purple-600"
        />
        <MetricCard
          title="Monthly Revenue (MRR)"
          value="$12,450"
          change={15.7}
          icon={DollarSign}
          iconBg="bg-green-100"
          iconColor="text-green-600"
        />
        <MetricCard
          title="Churn Rate"
          value="3.2%"
          change={-1.2}
          icon={TrendingDown}
          iconBg="bg-red-100"
          iconColor="text-red-600"
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Customer Growth */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Customer Growth</h3>
          <div className="h-64 flex items-end justify-between gap-2">
            {[45, 52, 58, 65, 72, 78, 85, 92, 98, 105, 112, 120].map((height, i) => (
              <div key={i} className="flex-1 bg-gradient-to-t from-blue-500 to-blue-400 rounded-t hover:from-blue-600 hover:to-blue-500 transition cursor-pointer" style={{ height: `${height / 1.2}%` }}></div>
            ))}
          </div>
          <div className="flex justify-between mt-4 text-xs text-gray-500">
            <span>Jan</span>
            <span>Dec</span>
          </div>
        </div>

        {/* Subscription Status */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Subscription Status</h3>
          <div className="flex items-center justify-center h-64">
            <div className="relative w-48 h-48">
              <svg className="w-full h-full transform -rotate-90">
                <circle cx="96" cy="96" r="80" fill="none" stroke="#e5e7eb" strokeWidth="16" />
                <circle cx="96" cy="96" r="80" fill="none" stroke="#3b82f6" strokeWidth="16" strokeDasharray="502" strokeDashoffset="125" />
                <circle cx="96" cy="96" r="80" fill="none" stroke="#10b981" strokeWidth="16" strokeDasharray="502" strokeDashoffset="251" />
                <circle cx="96" cy="96" r="80" fill="none" stroke="#f59e0b" strokeWidth="16" strokeDasharray="502" strokeDashoffset="376" />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="text-center">
                  <p className="text-3xl font-bold text-gray-900">89</p>
                  <p className="text-sm text-gray-500">Total</p>
                </div>
              </div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4 mt-4">
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                <span className="text-xs text-gray-600">Active</span>
              </div>
              <p className="text-lg font-semibold text-gray-900">67</p>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                <span className="text-xs text-gray-600">Trial</span>
              </div>
              <p className="text-lg font-semibold text-gray-900">15</p>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
                <span className="text-xs text-gray-600">Paused</span>
              </div>
              <p className="text-lg font-semibold text-gray-900">7</p>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Subscriptions and Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Subscriptions */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Subscriptions</h3>
          <div className="space-y-3">
            {[
              { customer: 'John Doe', plan: 'Premium Plan', status: 'Active', amount: '$99/mo', date: '2 hours ago' },
              { customer: 'Jane Smith', plan: 'Basic Plan', status: 'Trial', amount: '$29/mo', date: '5 hours ago' },
              { customer: 'Bob Johnson', plan: 'Enterprise Plan', status: 'Active', amount: '$299/mo', date: '1 day ago' },
              { customer: 'Alice Brown', plan: 'Premium Plan', status: 'Active', amount: '$99/mo', date: '2 days ago' },
            ].map((sub, i) => (
              <div key={i} className="flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg transition">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">{sub.customer}</p>
                  <p className="text-xs text-gray-500">{sub.plan}</p>
                </div>
                <div className="flex items-center gap-4">
                  <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                    sub.status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'
                  }`}>
                    {sub.status}
                  </span>
                  <span className="text-sm font-semibold text-gray-900 min-w-[80px] text-right">{sub.amount}</span>
                  <span className="text-xs text-gray-400 min-w-[80px] text-right">{sub.date}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
          <div className="space-y-3">
            <button className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition text-sm">
              + Create Customer
            </button>
            <button className="w-full px-4 py-3 bg-white border-2 border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition text-sm">
              + Create Subscription
            </button>
            <button className="w-full px-4 py-3 bg-white border-2 border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition text-sm">
              📦 View Deliveries
            </button>
            <div className="pt-3 border-t border-gray-200">
              <p className="text-xs text-gray-500 mb-2">Upcoming Renewals</p>
              <p className="text-2xl font-bold text-gray-900">23</p>
              <p className="text-xs text-gray-500">in next 7 days</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuthStore();
  const { selectedTenant } = useTenantStore();

  const isSuperAdmin = user?.role === 'SUPER_ADMIN';
  const showPlatformView = isSuperAdmin && !selectedTenant;

  return (
    <div className="p-6">
      {/* Page Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          {showPlatformView ? 'Platform Dashboard' : 'Dashboard'}
        </h1>
        <p className="text-gray-600 mt-1">
          {showPlatformView 
            ? 'Overview of all tenants and platform metrics' 
            : `Welcome back, ${user?.firstName}! Here's what's happening with ${selectedTenant?.name || 'your account'}.`
          }
        </p>
      </div>

      {/* Dashboard Content */}
      {showPlatformView ? <SuperAdminDashboard /> : <TenantAdminDashboard />}
    </div>
  );
}
