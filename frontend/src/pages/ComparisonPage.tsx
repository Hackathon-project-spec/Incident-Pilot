import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import Spinner from '../components/Spinner';
import ComparisonResult from '../components/ComparisonResult';
import { compareIncident } from '../api/api';

const ComparisonPage: React.FC = () => {
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = await compareIncident(form);
      setResult(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Comparison failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <Navbar />
      <h1>Memory Comparison</h1>
      <Card title="Compare With/Without Memory">
        <form onSubmit={handleSubmit}>
          <label className="label">Service Name</label>
          <input className="input" name="serviceName" value={form.serviceName} onChange={handleChange} required />

          <label className="label">Symptoms</label>
          <textarea className="input" name="symptoms" value={form.symptoms} onChange={handleChange} rows={3} />

          <label className="label">Logs</label>
          <textarea className="input" name="logs" value={form.logs} onChange={handleChange} rows={3} />

          <label className="label">Severity</label>
          <input className="input" name="severity" value={form.severity} onChange={handleChange} />

          <button className="button" type="submit" disabled={loading}>Compare</button>
        </form>
        {loading && <Spinner />}
        {error && <p style={{ color: 'var(--color-error)' }}>{error}</p>}
        {result && <ComparisonResult data={result} />}
      </Card>
    </div>
  );
};

export default ComparisonPage;
