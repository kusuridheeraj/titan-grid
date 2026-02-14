import { useState } from 'react';
import { Layout } from '@/components/layout/Layout';
import { Overview } from '@/features/Overview';
import { CryptexDashboard } from '@/features/cryptex/CryptexDashboard';
import { NexusDashboard } from '@/features/nexus/NexusDashboard';
// ... imports

// ... inside App component
{ activeTab === 'cryptex' && <CryptexDashboard /> }
const [activeTab, setActiveTab] = useState('dashboard');
const [isLoggedIn, setIsLoggedIn] = useState(false); // Simple login simulations

if (!isLoggedIn) {
  return (
    <div className="h-screen w-full flex items-center justify-center bg-background relative overflow-hidden">
      {/* Background Effects */}
      <div className="absolute inset-0 bg-grid-white/[0.02] bg-[length:30px_30px]" />
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-primary/20 rounded-full blur-[100px]" />

      <div className="relative z-10 bg-card/50 backdrop-blur-xl border border-border/50 p-8 rounded-2xl shadow-2xl w-full max-w-md text-center space-y-6">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight">TITAN GRID</h1>
          <p className="text-muted-foreground">Secure System Access</p>
        </div>

        <Button
          className="w-full h-12 text-lg font-medium shadow-[0_0_20px_rgba(59,130,246,0.5)] hover:shadow-[0_0_30px_rgba(59,130,246,0.6)] transition-all duration-300"
          onClick={() => setIsLoggedIn(true)}
        >
          Enter Mission Control
        </Button>

        <p className="text-xs text-muted-foreground">Authorized Personnel Only • v1.0.0</p>
      </div>
    </div>
  );
}

return (
  <Layout activeTab={activeTab} onTabChange={setActiveTab}>
    {activeTab === 'dashboard' && <Overview />}
    {activeTab === 'aegis' && <AegisDashboard />}
    {activeTab === 'cryptex' && <CryptexDashboard />}
    {activeTab === 'nexus' && <NexusDashboard />}
  </Layout>
);
}

export default App;
