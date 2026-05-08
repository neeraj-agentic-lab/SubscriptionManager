import { BarChart, Download, Calendar, TrendingUp, DollarSign, Users, Repeat } from 'lucide-react';

export default function ReportsPage() {
  const reports = [
    {
      id: 1,
      name: 'Revenue Report',
      description: 'Monthly revenue breakdown and trends',
      icon: DollarSign,
      color: 'green',
      lastGenerated: '2024-03-14',
    },
    {
      id: 2,
      name: 'Subscription Analytics',
      description: 'Active subscriptions, churn rate, and growth metrics',
      icon: Repeat,
      color: 'blue',
      lastGenerated: '2024-03-14',
    },
    {
      id: 3,
      name: 'Customer Insights',
      description: 'Customer acquisition, retention, and lifetime value',
      icon: Users,
      color: 'purple',
      lastGenerated: '2024-03-13',
    },
    {
      id: 4,
      name: 'Growth Metrics',
      description: 'MRR growth, expansion, and contraction analysis',
      icon: TrendingUp,
      color: 'orange',
      lastGenerated: '2024-03-13',
    },
  ];

  const getColorClasses = (color: string) => {
    switch (color) {
      case 'green': return { bg: 'bg-green-100', text: 'text-green-600', hover: 'hover:bg-green-50' };
      case 'blue': return { bg: 'bg-blue-100', text: 'text-blue-600', hover: 'hover:bg-blue-50' };
      case 'purple': return { bg: 'bg-purple-100', text: 'text-purple-600', hover: 'hover:bg-purple-50' };
      case 'orange': return { bg: 'bg-orange-100', text: 'text-orange-600', hover: 'hover:bg-orange-50' };
      default: return { bg: 'bg-gray-100', text: 'text-gray-600', hover: 'hover:bg-gray-50' };
    }
  };

  return (
    <div className="p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Reports</h1>
          <p className="text-gray-600 mt-1">Generate and download business reports</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {reports.map((report) => {
          const Icon = report.icon;
          const colors = getColorClasses(report.color);
          return (
            <div key={report.id} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition">
              <div className="flex items-start gap-4">
                <div className={`w-12 h-12 ${colors.bg} rounded-lg flex items-center justify-center flex-shrink-0`}>
                  <Icon className={`w-6 h-6 ${colors.text}`} />
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-gray-900 mb-1">{report.name}</h3>
                  <p className="text-sm text-gray-600 mb-4">{report.description}</p>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-sm text-gray-500">
                      <Calendar className="w-4 h-4" />
                      <span>Last: {new Date(report.lastGenerated).toLocaleDateString()}</span>
                    </div>
                    <button className="flex items-center gap-2 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition">
                      <Download className="w-4 h-4" />
                      Generate
                    </button>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="text-center">
          <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <BarChart className="w-8 h-8 text-blue-600" />
          </div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">Custom Reports</h3>
          <p className="text-gray-600 mb-6">Need a specific report? Create custom reports with your own parameters and filters.</p>
          <button className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-medium">
            Create Custom Report
          </button>
        </div>
      </div>
    </div>
  );
}
