// src/pages/DemoControlCenter.tsx
import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import Spinner from '../components/Spinner';
import DiagnosisResult from '../components/DiagnosisResult';
import ComparisonResult from '../components/ComparisonResult';
import {
  demoReset,
  demoSeedMemory,
  demoSeedAll,
  demoGetComparison,
  demoSimilarIncident,
} from '../api/api';
import './DemoControlCenter.css';

/**
 * Central hub for all hackathon demo controls.
 * Uses the exact backend endpoints defined in DemoController – no invented paths.
 */
const DemoControlCenter: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [message, setMessage] = useState<string>('');
  const [incident, setIncident] = useState<any>(null); // result of similar‑incident
  const [comparison, setComparison] = useState<any>(null); // IncidentComparisonResponse

  const runAction = async (
    action: () => Promise<any>,
    successMsg: string,
    onSuccess?: (data: any) => void
  ) => {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const data = await action();
      setMessage(successMsg);
      if (onSuccess) onSuccess(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Action failed');
    } finally {
      setLoading(false);
    }
  };

  // ----- demo actions ---------------------------------------------------
  const handleReset = () => runAction(demoReset, 'Memory reset successful');

  const handleSeedMemory = () =>
    runAction(demoSeedMemory, 'Demo memory seeded (single incident)');

  const handleSeedAll = () =>
    runAction(demoSeedAll, 'All demo postmortems seeded');

  const handleLoadDemoIncident = () =>
    runAction(
      () => demoSimilarIncident({
        serviceName: 'Checkout API',
        symptoms: 'High latency and database connection timeouts',
        logs: 'ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool',
        severity: 'SEV-1',
      }),
      'Demo incident loaded',
      data => setIncident(data)
    );

  const handleRunComparison = () =>
    runAction(
      demoGetComparison,
      'Demo comparison retrieved',
      data => setComparison(data)
    );

  return (
    <div className="container">
      <Navbar />
      <h1>Demo Control Center</h1>

      <Card title="Memory Operations">
        <button className="button" onClick={handleReset} disabled={loading}>Reset Memory</button>
        <button className="button" onClick={handleSeedMemory} disabled={loading} style={{ marginLeft: '0.5rem' }}>Seed Demo Memory</button>
        <button className="button" onClick={handleSeedAll} disabled={loading} style={{ marginLeft: '0.5rem' }}>Seed All Demo Data</button>
      </Card>

      <Card title="Demo Incident & Comparison">
        <button className="button" onClick={handleLoadDemoIncident} disabled={loading}>Load Demo Incident</button>
        <button className="button" onClick={handleRunComparison} disabled={loading} style={{ marginLeft: '0.5rem' }}>Run Demo Comparison</button>
      </Card>

      {loading && <Spinner />}
      {error && <p style={{ color: 'var(--color-error)' }}>{error}</p>}
      {message && <p style={{ color: 'var(--color-success)' }}>{message}</p>}

      {/* Display the loaded demo incident (if any) */}
      {incident && (
        <Card title="Loaded Demo Incident">
          <DiagnosisResult data={incident} />
        </Card>
      )}

      {/* Display side‑by‑side comparison when available */}
      {comparison && (
        <ComparisonResult data={comparison} />
      )}
    </div>
  );
};

export default DemoControlCenter;
