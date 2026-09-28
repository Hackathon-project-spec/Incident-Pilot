import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import Spinner from '../components/Spinner';
import { demoReset, demoSeedMemory, demoSimilarIncident } from '../api/api';

const DemoControlsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string>('');
  const [error, setError] = useState<string>('');

  const handleAction = async (action: () => Promise<any>, successMsg: string) => {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      await action();
      setMessage(successMsg);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Demo action failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSimilar = async () => {
    const payload = {
      serviceName: 'Checkout API',
      symptoms: 'Latency increased from 200ms to 4 seconds',
      logs: 'Database connection timeout',
      severity: 'SEV-1'
    };
    await handleAction(() => demoSimilarIncident(payload), 'Similar incident analyzed');
  };

  return (
    <div className="container">
      <Navbar />
      <h1>Demo Controls</h1>
      <Card title="Demo Operations">
        <button className="button" onClick={() => handleAction(demoReset, 'Memory reset')}>Reset Memory</button>
        <button className="button" onClick={() => handleAction(demoSeedMemory, 'Memory seeded')} style={{ marginLeft: '0.5rem' }}>Seed Memory</button>
        <button className="button" onClick={handleSimilar} style={{ marginLeft: '0.5rem' }}>Run Similar Incident</button>
        {loading && <Spinner />}
        {message && <p style={{ color: 'var(--color-success)' }}>{message}</p>}
        {error && <p style={{ color: 'var(--color-error)' }}>{error}</p>}
      </Card>
    </div>
  );
};

export default DemoControlsPage;
