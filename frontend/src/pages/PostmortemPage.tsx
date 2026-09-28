import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import Spinner from '../components/Spinner';
import { submitPostmortem } from '../api/api';

const PostmortemPage: React.FC = () => {
  const [form, setForm] = useState({
    incidentId: '',
    serviceName: '',
    actualRootCause: '',
    successfulFix: '',
    failedApproaches: '',
    preventionStrategy: '',
    lessonsLearned: '',
    symptoms: '',
    description: ''
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
      const payload = {
        incidentId: form.incidentId,
        serviceName: form.serviceName,
        actualRootCause: form.actualRootCause,
        successfulFix: form.successfulFix,
        failedApproaches: form.failedApproaches,
        preventionStrategy: form.preventionStrategy,
        lessonsLearned: form.lessonsLearned,
        symptoms: form.symptoms,
        description: form.description,
      };
      const data = await submitPostmortem(payload);
      setResult(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to submit postmortem');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <Navbar />
      <h1>Submit Postmortem</h1>
      <Card title="Postmortem Details">
        <form onSubmit={handleSubmit}>
          <label className="label">Incident ID</label>
          <input className="input" name="incidentId" value={form.incidentId} onChange={handleChange} required />

          <label className="label">Service Name</label>
          <input className="input" name="serviceName" value={form.serviceName} onChange={handleChange} required />

          <label className="label">Actual Root Cause</label>
          <input className="input" name="actualRootCause" value={form.actualRootCause} onChange={handleChange} />

          <label className="label">Successful Fix</label>
          <input className="input" name="successfulFix" value={form.successfulFix} onChange={handleChange} />

          <label className="label">Failed Approaches (comma separated)</label>
          <input className="input" name="failedApproaches" value={form.failedApproaches} onChange={handleChange} />

          <label className="label">Prevention Strategy</label>
          <input className="input" name="preventionStrategy" value={form.preventionStrategy} onChange={handleChange} />

          <label className="label">Lessons Learned</label>
          <input className="input" name="lessonsLearned" value={form.lessonsLearned} onChange={handleChange} />

          <label className="label">Symptoms (optional)</label>
          <textarea className="input" name="symptoms" value={form.symptoms} onChange={handleChange} rows={2} />

          <label className="label">Description (optional)</label>
          <textarea className="input" name="description" value={form.description} onChange={handleChange} rows={2} />

          <button className="button" type="submit" disabled={loading}>Submit</button>
        </form>
        {loading && <Spinner />}
        {error && <p style={{ color: 'var(--color-error)' }}>{error}</p>}
        {result && (
          <div style={{ marginTop: '1rem' }}>
            <h3>Response</h3>
            <pre>{JSON.stringify(result, null, 2)}</pre>
          </div>
        )}
      </Card>
    </div>
  );
};

export default PostmortemPage;
