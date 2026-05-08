import { Server, Database, Activity, Settings, HardDrive, Cpu, MemoryStick, Zap } from 'lucide-react';

export default function SystemPage() {
  const systemStats = {
    uptime: '45 days, 12 hours',
    version: '1.0.0',
    environment: 'Production',
    region: 'US-East-1',
  };

  const healthMetrics = [
    { name: 'API Server', status: 'healthy', uptime: '99.98%', responseTime: '45ms' },
    { name: 'Database', status: 'healthy', uptime: '99.99%', responseTime: '12ms' },
    { name: 'Cache (Redis)', status: 'healthy', uptime: '99.95%', responseTime: '3ms' },
    { name: 'Message Queue', status: 'healthy', uptime: '99.97%', responseTime: '8ms' },
  ];

  const resourceUsage = [
    { name: 'CPU Usage', value: 45, max: 100, unit: '%', icon: Cpu, color: 'blue' },
    { name: 'Memory Usage', value: 6.2, max: 16, unit: 'GB', icon: MemoryStick, color: 'purple' },
    { name: 'Disk Usage', value: 234, max: 500, unit: 'GB', icon: HardDrive, color: 'green' },
    { name: 'Network I/O', value: 1.2, max: 10, unit: 'Gbps', icon: Zap, color: 'yellow' },
  ];

  const recentEvents = [
    { id: 1, type: 'info', message: 'Database backup completed successfully', timestamp: '2 hours ago' },
    { id: 2, type: 'warning', message: 'High memory usage detected on worker-3', timestamp: '5 hours ago' },
    { id: 3, type: 'success', message: 'System update deployed to production', timestamp: '1 day ago' },
    { id: 4, type: 'info', message: 'SSL certificate renewed', timestamp: '2 days ago' },
    { id: 5, type: 'info', message: 'Scheduled maintenance completed', timestamp: '3 days ago' },
  ];

  const getProgressColor = (color: string) => {
    switch (color) {
      case 'blue': return 'bg-blue-600';
      case 'purple': return 'bg-purple-600';
      case 'green': return 'bg-green-600';
      case 'yellow': return 'bg-yellow-600';
      default: return 'bg-gray-600';
    }
  };

  const getEventColor = (type: string) => {
    switch (type) {
      case 'success': return 'bg-green-100 text-green-800';
      case 'warning': return 'bg-yellow-100 text-yellow-800';
      case 'error': return 'bg-red-100 text-red-800';
      default: return 'bg-blue-100 text-blue-800';
    }
  };

  return (
    <div className="p-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">System</h1>
          <p className="text-gray-600 mt-1">Monitor platform health and performance</p>
        </div>
        <button className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition font-medium">
          <Settings className="w-5 h-5" />
          System Settings
        </button>
      </div>

      {/* System Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Server className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Uptime</h3>
          </div>
          <p className="text-lg font-bold text-gray-900">{systemStats.uptime}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <Activity className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Version</h3>
          </div>
          <p className="text-lg font-bold text-gray-900">{systemStats.version}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <Database className="w-5 h-5 text-green-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Environment</h3>
          </div>
          <p className="text-lg font-bold text-gray-900">{systemStats.environment}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 bg-yellow-100 rounded-lg flex items-center justify-center">
              <Server className="w-5 h-5 text-yellow-600" />
            </div>
            <h3 className="text-sm font-medium text-gray-600">Region</h3>
          </div>
          <p className="text-lg font-bold text-gray-900">{systemStats.region}</p>
        </div>
      </div>

      {/* Health Status */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Service Health</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {healthMetrics.map((service) => (
            <div key={service.name} className="border border-gray-200 rounded-lg p-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-medium text-gray-900">{service.name}</h3>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                  {service.status}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Uptime</p>
                  <p className="font-semibold text-gray-900">{service.uptime}</p>
                </div>
                <div>
                  <p className="text-gray-500">Response Time</p>
                  <p className="font-semibold text-gray-900">{service.responseTime}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Resource Usage */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Resource Usage</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {resourceUsage.map((resource) => {
            const Icon = resource.icon;
            const percentage = (resource.value / resource.max) * 100;
            return (
              <div key={resource.name} className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 bg-${resource.color}-100 rounded-lg flex items-center justify-center`}>
                      <Icon className={`w-5 h-5 text-${resource.color}-600`} />
                    </div>
                    <h3 className="font-medium text-gray-900">{resource.name}</h3>
                  </div>
                  <span className="text-sm font-semibold text-gray-900">
                    {resource.value} / {resource.max} {resource.unit}
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all ${getProgressColor(resource.color)}`}
                    style={{ width: `${percentage}%` }}
                  ></div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Recent Events */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Recent Events</h2>
        <div className="space-y-3">
          {recentEvents.map((event) => (
            <div key={event.id} className="flex items-start gap-3 p-3 hover:bg-gray-50 rounded-lg transition">
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getEventColor(event.type)} mt-0.5`}>
                {event.type}
              </span>
              <div className="flex-1">
                <p className="text-sm text-gray-900">{event.message}</p>
                <p className="text-xs text-gray-500 mt-1">{event.timestamp}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
