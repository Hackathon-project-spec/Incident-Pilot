// hindsightVerification.js
// Stand‑alone Node script to verify the Hindsight integration lifecycle.
// It performs: RESET → RETAIN (seed) → RECALL (similar incident) → COMPARE → RECALL again.
// No secrets are printed; only status and relevant fields are logged.

const axios = require('axios');
const base = 'http://localhost:8080/api';

async function resetMemory() {
  console.log('🔄 Resetting demo memory...');
  const res = await axios.post(`${base}/demo/reset`);
  console.log('Reset response:', res.data);
}

async function retainDemoMemory() {
  console.log('🗃️ Seeding demo memory (retain)...');
  const res = await axios.post(`${base}/demo/seed-memory`);
  console.log('Retain response (PostmortemResponse):', res.data);
  return res.data;
}

async function recallSimilarIncident() {
  console.log('🔎 Recalling similar incident (without exact wording)...');
  const payload = {
    serviceName: 'checkout-api',
    symptoms: 'increasing request timeouts during checkout',
    logs: 'ERROR: DB connection timeout after many attempts',
    severity: 'SEV-1'
  };
  const res = await axios.post(`${base}/demo/similar-incident`, payload);
  console.log('Recall response (IncidentAnalysisResponse):');
  console.log(JSON.stringify(res.data, null, 2));
  return res.data;
}

async function getComparison() {
  console.log('⚖️ Getting without‑memory vs with‑memory comparison...');
  const res = await axios.get(`${base}/demo/compare`);
  console.log('Comparison response (IncidentComparisonResponse):');
  console.log(JSON.stringify(res.data, null, 2));
  return res.data;
}

(async () => {
  try {
    await resetMemory();
    await retainDemoMemory();
    const recall1 = await recallSimilarIncident();
    // Check if memory was used in the first recall (historicalIncidents or memoryBasedInsights)
    const memoryUsed1 = (recall1.historicalIncidents && recall1.historicalIncidents.length > 0) ||
                       (recall1.memoryBasedInsights && recall1.memoryBasedInsights.length > 0);
    console.log('Memory used on first recall?', memoryUsed1 ? 'YES' : 'NO');

    const comparison = await getComparison();
    const withMem = comparison.withMemoryDiagnosis;
    const withoutMem = comparison.withoutMemoryDiagnosis;
    const memoryUsedInComparison = comparison.recalledMemories && comparison.recalledMemories.length > 0;
    console.log('Memory used in comparison?', memoryUsedInComparison ? 'YES' : 'NO');

    // Final recall to ensure memory persists after comparison
    const recall2 = await recallSimilarIncident();
    const memoryUsed2 = (recall2.historicalIncidents && recall2.historicalIncidents.length > 0) ||
                       (recall2.memoryBasedInsights && recall2.memoryBasedInsights.length > 0);
    console.log('Memory used on second recall?', memoryUsed2 ? 'YES' : 'NO');

    console.log('\n=== Verification Summary ===');
    console.log('Reset successful:', true);
    console.log('Retain successful (response contains retentionId):', !!(await retainDemoMemory()).retentionId);
    console.log('First recall memory used:', memoryUsed1);
    console.log('Comparison memory used:', memoryUsedInComparison);
    console.log('Second recall memory used:', memoryUsed2);
    console.log('If Hindsight API key is configured, the above calls hit the cloud; otherwise they ran against the in‑memory fallback (see backend logs).');
  } catch (err) {
    console.error('❗ Error during verification:', err?.response?.data || err.message);
  }
})();
