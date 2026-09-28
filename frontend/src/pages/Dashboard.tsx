import React from 'react';
import Navbar from '../components/Navbar';
import Card from '../components/Card';
import './Dashboard.css';

const Dashboard: React.FC = () => (
  <div className="container">
    <Navbar />
    <h1>Incident Pilot Dashboard</h1>
    <div className="grid">
      <Card title="Analyze Incident">
        <p>Submit a new incident and get AI‑driven diagnosis.</p>
        <a href="/analysis" className="button">Go</a>
      </Card>
      <Card title="Memory Comparison">
        <p>See how historical memory improves recommendations.</p>
        <a href="/comparison" className="button">Go</a>
      </Card>
      <Card title="Submit Postmortem">
        <p>Store resolved incident learnings for future recall.</p>
        <a href="/postmortem" className="button">Go</a>
      </Card>
      <Card title="Demo Controls">
        <p>Reset or seed demo memory quickly.</p>
        <a href="/demo" className="button">Go</a>
      </Card>
    </div>
  </div>
);

export default Dashboard;
