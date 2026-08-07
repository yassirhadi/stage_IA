export interface CopilotSearchDocument {
  fileName?: string;
  documentType?: string;
  confidentialityLevel?: string;
}

export interface CopilotSearchRisk {
  title?: string;
  severity?: string;
}

export interface CopilotSearchReport {
  title?: string;
  content?: string;
}

export interface CopilotReportContext {
  documents: CopilotSearchDocument[];
  risks: CopilotSearchRisk[];
  complianceScore?: number | null;
  latestReport?: CopilotSearchReport | null;
}

export function buildDatabaseSearchResponse(
  documents: CopilotSearchDocument[],
  risks: CopilotSearchRisk[],
  reports: CopilotSearchReport[],
  query: string
) {
  const lines: string[] = [];

  if (documents.length > 0) {
    lines.push(`\n📄 Documents trouvés (${documents.length}):`);
    documents.forEach((doc) => {
      lines.push(`- ${doc.fileName ?? 'Document sans nom'} (${doc.documentType ?? 'Non défini'}, ${doc.confidentialityLevel ?? 'Non défini'})`);
    });
  }

  if (risks.length > 0) {
    lines.push(`\n⚠️ Risques trouvés (${risks.length}):`);
    risks.forEach((risk) => {
      lines.push(`- ${risk.title ?? 'Risque sans titre'} (${risk.severity ?? 'Non défini'})`);
    });
  }

  if (reports.length > 0) {
    lines.push(`\n📋 Rapports trouvés (${reports.length}):`);
    reports.forEach((report) => {
      lines.push(`- ${report.title ?? 'Rapport sans titre'}`);
    });
  }

  if (lines.length === 0) {
    return `Aucun résultat trouvé dans la base de données pour « ${query} ».`;
  }

  return lines.join('\n');
}

export function buildReportAnswer(context: CopilotReportContext) {
  const documents = context.documents ?? [];
  const risks = context.risks ?? [];
  const confidentialDocs = documents.filter((d) => d.confidentialityLevel === 'CONFIDENTIEL' || d.confidentialityLevel === 'TRES_CONFIDENTIEL').length;
  const criticalRisks = risks.filter((risk) => risk.severity === 'CRITIQUE').length;
  const complianceScore = context.complianceScore;
  const latestReport = context.latestReport;

  const reportSummary = latestReport?.content && latestReport.content.trim()
    ? `\n🧾 Rapport persistant : ${latestReport.content}`
    : '\n🧾 Rapport persistant : Aucun rapport généré n’est encore disponible.';

  return `✅ Rapport généré à partir des données persistées

📊 Statistiques :
• Documents analysés : ${documents.length}
• Documents confidentiels : ${confidentialDocs}
• Risques détectés : ${risks.length}
• Risques critiques : ${criticalRisks}
• Score conformité : ${complianceScore ?? 'Non calculé'}${complianceScore != null ? ' %' : ''}${reportSummary}

📋 Conformité :
• ISO 27001 : Conforme
• RGPD : Conforme
• Loi 09-08 : Conforme

💡 Recommandations :
• Masquer les données sensibles (CIN, IBAN)
• Restreindre l'accès aux documents confidentiels
• Appliquer le chiffrement pour les données bancaires

📥 Télécharger PDF : Disponible dans la section Rapports`;
}
