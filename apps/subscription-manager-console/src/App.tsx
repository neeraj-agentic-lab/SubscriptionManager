import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import TenantsPage from './pages/TenantsPage';
import TenantDetailPage from './pages/TenantDetailPage';
import UsersPage from './pages/UsersPage';
import UserDetailPage from './pages/UserDetailPage';
import SystemPage from './pages/SystemPage';
import CustomersPage from './pages/CustomersPage';
import CustomerDetailPage from './pages/CustomerDetailPage';
import PlansPage from './pages/PlansPage';
import SubscriptionsPage from './pages/SubscriptionsPage';
import SubscriptionDetailPage from './pages/SubscriptionDetailPage';
import DeliveriesPage from './pages/DeliveriesPage';
import WebhooksPage from './pages/WebhooksPage';
import APIClientsPage from './pages/APIClientsPage';
import ReportsPage from './pages/ReportsPage';
import Header from './components/layout/Header';
import Sidebar from './components/layout/Sidebar';
import ErrorBoundary from './components/ErrorBoundary';
import TenantGuard from './components/TenantGuard';

function App() {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <BrowserRouter>
      <div className="flex h-screen bg-gray-50">
        <Sidebar />
        <div className="flex-1 flex flex-col overflow-hidden">
          <Header />
          <main className="flex-1 overflow-y-auto">
            <ErrorBoundary>
              <Routes>
                <Route path="/" element={<DashboardPage />} />
                {/* Platform View Routes */}
                <Route path="/tenants" element={<TenantsPage />} />
                <Route path="/tenants/:tenantId" element={<TenantDetailPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/users/:userId" element={<UserDetailPage />} />
                <Route path="/system" element={<SystemPage />} />
                {/* Tenant View Routes - Protected by TenantGuard */}
                <Route path="/customers" element={<TenantGuard><CustomersPage /></TenantGuard>} />
                <Route path="/customers/:id" element={<TenantGuard><CustomerDetailPage /></TenantGuard>} />
                <Route path="/plans" element={<TenantGuard><PlansPage /></TenantGuard>} />
                <Route path="/subscriptions" element={<TenantGuard><SubscriptionsPage /></TenantGuard>} />
                <Route path="/subscriptions/:id" element={<TenantGuard><SubscriptionDetailPage /></TenantGuard>} />
                <Route path="/deliveries" element={<TenantGuard><DeliveriesPage /></TenantGuard>} />
                <Route path="/webhooks" element={<TenantGuard><WebhooksPage /></TenantGuard>} />
                <Route path="/api-clients" element={<TenantGuard><APIClientsPage /></TenantGuard>} />
                <Route path="/reports" element={<TenantGuard><ReportsPage /></TenantGuard>} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </ErrorBoundary>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App
