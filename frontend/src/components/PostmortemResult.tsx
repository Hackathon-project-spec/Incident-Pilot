// src/components/PostmortemResult.tsx
import React from 'react';
import Card from './Card';

type PostmortemResultProps = {
  data: any; // PostmortemResponse from backend
};

const PostmortemResult: React.FC<PostmortemResultProps> = ({ data }) => {
  if (!data) return null;
  const {
    incidentId,
    serviceName,
    retentionStatus,
    retentionId,
    reflectionStatus,
    reflectionSummary,
    status,
    message,
  } = data;

  return (
    <Card title="Postmortem Result">
      <p><strong>Incident ID:</strong> {incidentId}</p>
      <p><strong>Service:</strong> {serviceName}</p>
      <p><strong>Retention Status:</strong> {retentionStatus}</p>
      {retentionId && <p><strong>Retention ID:</strong> {retentionId}</p>}
      <p><strong>Reflection Status:</strong> {reflectionStatus}</p>
      {reflectionSummary && <p><strong>Reflection Summary:</strong> {reflectionSummary}</p>}
      <p><strong>Overall Status:</strong> {status}</p>
      {message && <p><strong>Message:</strong> {message}</p>}
    </Card>
  );
};

export default PostmortemResult;
