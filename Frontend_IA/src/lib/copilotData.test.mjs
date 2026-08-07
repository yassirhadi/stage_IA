import test from 'node:test';
import assert from 'node:assert/strict';
import { buildDatabaseSearchResponse, buildReportAnswer } from './copilotData.js';

test('buildDatabaseSearchResponse ignores synthetic risks when no backend risks are provided', () => {
  const response = buildDatabaseSearchResponse(
    [{ fileName: 'contrat.docx', documentType: 'CONTRAT', confidentialityLevel: 'CONFIDENTIEL' }],
    [],
    [{ title: 'Rapport conformité', content: 'Contenu réel du rapport' }],
    'risques'
  );

  assert.ok(response.includes('Documents trouvés'));
  assert.ok(response.includes('Rapports trouvés'));
  assert.ok(!response.includes('Données bancaires exposées'));
});

test('buildReportAnswer uses the latest persisted report content when present', () => {
  const answer = buildReportAnswer({
    documents: [{ fileName: 'facture.pdf', confidentialityLevel: 'CONFIDENTIEL' }],
    risks: [{ severity: 'CRITIQUE' }],
    complianceScore: 82,
    latestReport: {
      title: 'Rapport conformité',
      content: 'Rapport réel généré à partir des données persistées.'
    }
  });

  assert.ok(answer.includes('Rapport réel généré'));
  assert.ok(answer.includes('82'));
});
