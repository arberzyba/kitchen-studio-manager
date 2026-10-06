import { Navigate, Route, Routes } from 'react-router'
import { LoginPage } from './auth/LoginPage'
import { RequireAuth } from './auth/RequireAuth'
import { CustomerDetailPage } from './customers/CustomerDetailPage'
import { CustomerFormPage } from './customers/CustomerFormPage'
import { CustomersPage } from './customers/CustomersPage'
import { CalendarPage } from './calendar/CalendarPage'
import { AppLayout } from './layout/AppLayout'
import { OrderDetailPage } from './orders/OrderDetailPage'
import { OrdersPage } from './orders/OrdersPage'
import { HomePage } from './pages/HomePage'
import { ProductsPage } from './products/ProductsPage'
import { QuoteDetailPage } from './quotes/QuoteDetailPage'
import { QuoteFormPage } from './quotes/QuoteFormPage'
import { QuotesPage } from './quotes/QuotesPage'
import { SupplierOrderDetailPage } from './supplierorders/SupplierOrderDetailPage'
import { SupplierOrdersPage } from './supplierorders/SupplierOrdersPage'
import { SuppliersPage } from './suppliers/SuppliersPage'
import { UsersPage } from './users/UsersPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route element={<RequireAuth roles={['ADMIN', 'SALES', 'OFFICE']} />}>
            <Route path="/customers" element={<CustomersPage />} />
            <Route path="/customers/:id" element={<CustomerDetailPage />} />
            <Route path="/quotes" element={<QuotesPage />} />
            <Route path="/quotes/:id" element={<QuoteDetailPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/:id" element={<OrderDetailPage />} />
            <Route path="/supplier-orders" element={<SupplierOrdersPage />} />
            <Route
              path="/supplier-orders/:id"
              element={<SupplierOrderDetailPage />}
            />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/suppliers" element={<SuppliersPage />} />
          </Route>
          <Route element={<RequireAuth roles={['ADMIN', 'SALES']} />}>
            <Route path="/customers/new" element={<CustomerFormPage />} />
            <Route path="/customers/:id/edit" element={<CustomerFormPage />} />
            <Route path="/quotes/new" element={<QuoteFormPage />} />
            <Route path="/quotes/:id/edit" element={<QuoteFormPage />} />
          </Route>
          <Route element={<RequireAuth roles={['ADMIN']} />}>
            <Route path="/users" element={<UsersPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
