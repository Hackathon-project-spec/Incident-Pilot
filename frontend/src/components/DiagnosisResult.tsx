// src/components/DiagnosisResult.tsx
import React from 'react';
import Card from './Card';

type DiagnosisResultProps = {
  data: any; // response from /api/incidents/analyze
};

const DiagnosisResult: React.FC<DiagnosisResultProps> = ({ data }) => {
  if (!data) return null;

  const {
    serviceName,
    severity,
    possibleRootCauses = [],
    investigationSteps = [],
    recommendedFixes = [],
    historicalIncidents = [],
    memoryBasedInsights = [],
    failedApproachesToAvoid = [],
    confidenceScore,
  } = data;

  const memoryUsed =
    (historicalIncidents && historicalIncidents.length > 0) ||
    (memoryBasedInsights && memoryBasedInsights.length > 0) ||
    (failedApproachesToAvoid && failedApproachesToAvoid.length > 0);

  return (
    <Card title="Diagnosis Result">
      <div>
        <p><strong>Service:</strong> {serviceName}</p>
        <p><strong>Severity:</strong> {severity}</p>
        {confidenceScore && (
          <p><strong>Confidence:</strong> {confidenceScore}</p>
        )}
        <p>
          <strong>Memory Used:</strong>{' '}
          {memoryUsed ? (
            <span style={{ color: 'var(--color-success)' }}>Yes</span>
          ) : (
            <span style={{ color: 'var(--color-muted)' }}>No</span>
          )}
        </p>
        {possibleRootCauses.length > 0 && (
          <section>
            <h4>Root Cause Hypothesis</h4>
            <ul>
              {possibleRootCauses.map((c: string, i: number) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          </section>
        )}
        {investigationSteps.length > 0 && (
          <section>
            <h4>Investigation Steps</h4>
            <ol>
              {investigationSteps.map((s: string, i: number) => (
                <li key={i}>{s}</li>
              ))}
            </ol>
          </section>
        )}
        {recommendedFixes.length > 0 && (
          <section>
            <h4>Recommended Fixes</h4>
            <ul>
              {recommendedFixes.map((f: string, i: number) => (
                <li key={i}>{f}</li>
              ))}
            </ul>
          </section>
        )}
        {historicalIncidents.length > 0 && (
          <section>
            <h4>Related Historical Incidents</h4>
            <ul>
              {historicalIncidents.map((h: string, i: number) => (
                <li key={i}>{h}</li>
              ))}
            </ul>
          </section>
        )}
        {memoryBasedInsights.length > 0 && (
          <section>
            <h4>Memory‑Based Insights</h4>
            <ul>
              {memoryBasedInsights.map((i: string, idx: number) => (
                <li key={idx}>{i}</li>
              ))}
            </ul>
          </section>
        )}
        {failedApproachesToAvoid.length > 0 && (
          <section>
            <h4>Failed Approaches To Avoid</h4>
            <ul>
              {failedApproachesToAvoid.map((f: string, i: number) => (
                <li key={i}>{f}</li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </Card>
  );
};

export default DiagnosisResult;
