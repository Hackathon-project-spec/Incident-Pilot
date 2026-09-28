import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import Spinner from '../components/Spinner';
import DiagnosisResult from '../components/DiagnosisResult';
import { analyzeIncident } from '../api/api';

const IncidentAnalysisPage: React.FC = () => {
  const [form, setForm] = useState({
    serviceName: '',
    symptoms: '',
    logs: '',
    severity: ''
  });
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const validate = () => {
    return form.serviceName.trim() !== '' && form.severity.trim() !== '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) {
      setError('Service Name and Severity are required.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await analyzeIncident(form);
      setResult(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to analyze incident');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <Navbar />
      <h1>Incident Analysis</h1>
      <Card title="Submit Incident">
        <form onSubmit={handleSubmit}>
          <label className="label">Service Name *</label>
          <input
            className="input"
            name="serviceName"
            value={form.serviceName}
            onChange={handleChange}
            placeholder="e.g., Checkout API"
            required
          />

          <label className="label">Severity *</label>
          <input
            className="input"
            name="severity"
            value={form.severity}
            onChange={handleChange}
            placeholder="e.g., SEV-1"
            required
          />

          <label className="label">Symptoms</label>
          <textarea
            className="input"
            name="symptoms"
            value={form.symptoms}
            onChange={handleChange}
            rows={3}
            placeholder="Describe symptoms, e.g., Latency increased..."
          />

          <label className="label">Logs / Error Information</label>
          <textarea
            className="input"
            name="logs"
            value={form.logs}
            onChange={handleChange}
            rows={3}
            placeholder="Paste relevant log snippets"
          />

          <button className="button" type="submit" disabled={loading}>
            Analyze
          </button>
        </form>
        {loading && <Spinner />}
        {error && <p style={{ color: 'var(--color-error)' }}>{error}</p>}
        {result && <DiagnosisResult data={result} />}
      </Card>
    </div>
  );
};

export default IncidentAnalysisPage;
