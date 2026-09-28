// src/pages/FullDemoPage.tsx
import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import Spinner from '../components/Spinner';

import ComparisonResult from '../components/ComparisonResult';
import PostmortemResult from '../components/PostmortemResult';
import {
  demoReset,
  demoSeedMemory,
  analyzeIncident,
  compareIncident,
  submitPostmortem,
} from '../api/api';
import './FullDemoPage.css';

/**
 * Full end‑to‑end demonstration of the Incident Pilot workflow.
 * Each step is gated by a button so the presenter can walk the audience through the process.
 */
const FullDemoPage: React.FC = () => {
  // Demo memory reset / seed
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [message, setMessage] = useState<string>('');

  // New incident payload (Step 4)
  const [newIncident, setNewIncident] = useState({
    serviceName: '',
    symptoms: '',
    logs: '',
    severity: '',
  });

  // Diagnosis results returned by the comparison endpoint (Step 5)
  const [withoutMem, setWithoutMem] = useState<any>(null);
  const [withMem, setWithMem] = useState<any>(null);

  // Postmortem result (Step 9)
  const [postmortem, setPostmortem] = useState<any>(null);

  // Helper to run async actions with consistent UI handling
  const runAction = async (
    action: () => Promise<any>,
    successMsg: string,
    onSuccess?: (data: any) => void,
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

  // STEP 2 – reset demo memory
  const handleReset = () => runAction(demoReset, 'Memory reset');

  // STEP 2 – seed demo memory
  const handleSeed = () => runAction(demoSeedMemory, 'Memory seeded');

  // STEP 4 – update new incident form (user edits fields manually)
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setNewIncident({ ...newIncident, [e.target.name]: e.target.value });

  // STEP 5 – run comparison (both without‑memory and with‑memory diagnoses)
  const handleCompare = async () => {
    if (!newIncident.serviceName || !newIncident.severity) {
      setError('Service and Severity are required for comparison');
      return;
    }
    await runAction(
      () => compareIncident(newIncident),
      'Comparison received',
      (data) => {
        setWithMem(data?.withMemoryDiagnosis);
        setWithoutMem(data?.withoutMemoryDiagnosis);
      },
    );
  };

  // STEP 9 – submit postmortem (uses the diagnosis returned by the comparison)
  const handleSubmitPostmortem = async () => {
    const payload = {
      incidentId: withMem?.incidentId || 'INC-UNKNOWN',
      serviceName: withMem?.serviceName || newIncident.serviceName,
      actualRootCause: withMem?.possibleRootCauses?.[0] || '',
      successfulFix: withMem?.recommendedFixes?.[0] || '',
      failedApproaches: withMem?.failedApproachesToAvoid?.join(', ') || '',
      preventionStrategy: withMem?.memoryBasedInsights?.join(', ') || '',
      lessonsLearned: withMem?.memoryBasedInsights?.join(', ') || '',
    };
    await runAction(
      () => submitPostmortem(payload),
      'Postmortem submitted',
      (data) => setPostmortem(data),
    );
  };

  // STEP 11 – submit another related incident (regular analysis with recall)
  const handleSubmitRelatedIncident = async () => {
    await runAction(
      () => analyzeIncident(newIncident),
      'Related incident analyzed',
      (data) => {
        console.log('Related incident result', data);
      },
    );
  };

  return (
    <div className="container">
      <Navbar />
      <h1>Full Incident Pilot Demo</h1>

      {/* Demo control section */}
      <Card title="Demo Controls (Reset / Seed)">
        <button className="button" onClick={handleReset} disabled={loading}>
          Reset Memory
        </button>
        <button
          className="button"
          onClick={handleSeed}
          disabled={loading}
          style={{ marginLeft: '0.5rem' }}
        >
          Seed Memory
        </button>
      </Card>

      {/* New incident form */}
      <Card title="Step 4 – New Incident (similar to historical)">
        <form>
          <label className="label">Service *</label>
          <input
            className="input"
            name="serviceName"
            value={newIncident.serviceName}
            onChange={handleChange}
            placeholder="e.g., Checkout API"
            required
          />
          <label className="label">Severity *</label>
          <input
            className="input"
            name="severity"
            value={newIncident.severity}
            onChange={handleChange}
            placeholder="SEV-1"
            required
          />
          <label className="label">Symptoms</label>
          <textarea
            className="input"
            name="symptoms"
            rows={2}
            value={newIncident.symptoms}
            onChange={handleChange}
            placeholder="Describe symptoms..."
          />
          <label className="label">Logs / Error Info</label>
          <textarea
            className="input"
            name="logs"
            rows={2}
            value={newIncident.logs}
            onChange={handleChange}
            placeholder="Paste logs"
          />
        </form>
      </Card>

      {/* Comparison step */}
      <Card title="Step 5 – Run Comparison (Without vs With Memory)">
        <button className="button" onClick={handleCompare} disabled={loading}>
          Run Comparison
        </button>
        {withMem && withoutMem && (
          <ComparisonResult
            data={{
              serviceName: newIncident.serviceName,
              severity: newIncident.severity,
              symptoms: newIncident.symptoms,
              logs: newIncident.logs,
              recalledMemories: withMem?.historicalIncidents || [],
              withoutMemoryDiagnosis: withoutMem,
              withMemoryDiagnosis: withMem,
              comparisonHighlights: [],
            }}
          />
        )}
      </Card>

      {/* Postmortem submission */}
      <Card title="Step 9 – Submit Postmortem">
        <button className="button" onClick={handleSubmitPostmortem} disabled={loading}>
          Submit Postmortem
        </button>
        {postmortem && <PostmortemResult data={postmortem} />}
      </Card>

      {/* Submit another related incident */}
      <Card title="Step 11 – Submit Another Related Incident">
        <button className="button" onClick={handleSubmitRelatedIncident} disabled={loading}>
          Submit Related Incident
        </button>
      </Card>

      {loading && <Spinner />}
      {error && <p style={{ color: 'var(--color-error)' }}>{error}</p>}
      {message && <p style={{ color: 'var(--color-success)' }}>{message}</p>}
    </div>
  );
};

export default FullDemoPage;
