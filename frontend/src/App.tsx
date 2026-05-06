import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import IncidentDetail from './pages/IncidentDetail';
import RcaForm from './pages/RcaForm';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen">
        <Sidebar />
        <main className="flex-1 ml-64 p-6 lg:p-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/incidents" element={<Dashboard />} />
            <Route path="/incidents/:id" element={<IncidentDetail />} />
            <Route path="/incidents/:id/rca" element={<RcaForm />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
