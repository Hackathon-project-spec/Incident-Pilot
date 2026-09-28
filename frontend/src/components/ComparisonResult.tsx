// src/components/ComparisonResult.tsx
import React from 'react';
import Card from './Card';
import './ComparisonResult.css';

/**
 * Displays the result of the comparison endpoint (`POST /api/incidents/compare`).
 * The component extracts useful fields from the raw response and presents them
 * in a clean, two‑column dashboard suitable for a hackathon demo.
 */
type ComparisonResultProps = {
  data: any; // IncidentComparisonResponse from backend
};

const ComparisonResult: React.FC<ComparisonResultProps> = ({ data }) => {
  if (!data) return null;

  const {
    serviceName,
    severity,
    recalledMemories = [],
    withoutMemoryDiagnosis = {},
    withMemoryDiagnosis = {},
    comparisonHighlights = [],
    memoryAdvantageDemonstrated,
    summary,
  } = data;

  // --- Helper to render lists safely (no empty bullets) ---
  const renderList = (items: any[], emptyMessage?: string) =>
    items && items.length > 0 ? (
      <ul>
        {items.map((it, i) => (
          <li key={i}>{it}</li>
        ))}
      </ul>
    ) : emptyMessage ? <p>{emptyMessage}</p> : null;

  // --- Without‑memory diagnosis fields ---
  const {
    possibleRootCauses = [],
    investigationSteps = [],
    recommendedFixes = [],
    confidenceScore,
  } = withoutMemoryDiagnosis;

  // --- With‑memory diagnosis fields ---
  const {
    possibleRootCauses: memRootCauses = [],
    investigationSteps: memInvestigation = [],
    recommendedFixes: memFixes = [],
    confidenceScore: memConfidence,
    historicalIncidents = [],
    memoryBasedInsights = [],
    failedApproachesToAvoid = [],
  } = withMemoryDiagnosis;

  // --- Use the first recalled memory string for the Historical Evidence card ---
  const historicalMemory = recalledMemories[0] || '';

  return (
    <div className="comparison-result">
      {/* Header */}
      <header className="cmp-header">
        <h2>Memory‑Augmented Diagnosis</h2>
        <div className="service-info">
          <span className="service-name">{serviceName}</span>
          <span className="severity-badge severity-{severity?.toLowerCase()}">{severity}</span>
        </div>
        {memoryAdvantageDemonstrated && (
          <div className="advantage-banner">
            <span className="checkmark">✓</span> Memory Advantage Demonstrated
            <p className="subtext">
              Hindsight connected the current incident with a relevant historical incident.
            </p>
          </div>
        )}
      </header>

      {/* Two‑column comparison */}
      <div className="comparison-grid">
        {/* LEFT – without memory */}
        <div className="panel left">
          <h3>WITHOUT MEMORY</h3>
          <p className="subtitle">Cold‑start diagnosis</p>
          {confidenceScore && (
            <p><strong>Confidence:</strong> {confidenceScore}</p>
          )}
          {renderList(possibleRootCauses, 'No root‑cause hypotheses.')}
          {renderList(investigationSteps, 'No investigation steps.')}
          {renderList(recommendedFixes, 'No recommended fixes.')}
        </div>

        {/* RIGHT – with memory */}
        <div className="panel right">
          <h3>WITH HINDSIGHT MEMORY</h3>
          <p className="subtitle">Historical memory‑assisted diagnosis</p>
          {memConfidence && (
            <p><strong>Confidence:</strong> {memConfidence}</p>
          )}
          {renderList(memRootCauses, 'No historical root‑cause hypotheses.')}
          {renderList(memInvestigation, 'No investigation steps.')}
          {renderList(memFixes, 'No recommended fixes.')}
          {renderList(memoryBasedInsights, 'No memory‑based insights.')}
          {renderList(failedApproachesToAvoid, 'No failed approaches to avoid.')}
        </div>
      </div>

      {/* Memory Impact */}
      <Card title="WHAT HINDSIGHT ADDED">
        <section>
          <h4>Historical Incident Recalled</h4>
          {recalledMemories && recalledMemories.length > 0 ? (
            <ul>
              {recalledMemories.map((m, i) => (
                <li key={i}>{m}</li>
              ))}
            </ul>
          ) : (
            <p>No historical memories were returned.</p>
          )}
        </section>
      </Card>

      {/* Historical Evidence */}
      <Card title="Historical Evidence">
        <section>
          {historicalMemory ? (
            <p>{historicalMemory}</p>
          ) : (
            <p>No historical memory available.</p>
          )}
        </section>
      </Card>

      {/* Comparison Highlights */}
      {comparisonHighlights && comparisonHighlights.length > 0 && (
        <Card title="Why Memory Helped">
          <ul>
            {comparisonHighlights.map((h, i) => (
              <li key={i}>{h}</li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
};

export default ComparisonResult;
