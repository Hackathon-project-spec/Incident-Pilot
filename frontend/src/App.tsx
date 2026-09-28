import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import IncidentAnalysisPage from './pages/IncidentAnalysisPage';
import ComparisonPage from './pages/ComparisonPage';
import PostmortemPage from './pages/PostmortemPage';
import DemoControlsPage from './pages/DemoControlsPage';
import DemoControlCenter from './pages/DemoControlCenter';
import FullDemoPage from './pages/FullDemoPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/analysis" element={<IncidentAnalysisPage />} />
      <Route path="/comparison" element={<ComparisonPage />} />
      <Route path="/postmortem" element={<PostmortemPage />} />
      <Route path="/demo" element={<DemoControlsPage />} />
      <Route path="/demo-center" element={<DemoControlCenter />} />
      <Route path="/full-demo" element={<FullDemoPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
