import { useEffect, useState } from 'react';
import { securityApi } from '../api/services';
import '../styles/Pages.css';

interface SecurityScore {
  id?: number;
  overallScore?: number | null;
  documentsScore?: number | null;
  risksScore?: number | null;
  complianceScore?: number | null;
  recommendationsScore?: number | null;
  calculatedAt?: string;
}

function unwrapSecurityScore(payload: any): SecurityScore | null {
  if (!payload) return null;

  if (payload && typeof payload === 'object') {
    const maybeResponse = payload as { data?: unknown };
    if ('data' in maybeResponse && maybeResponse.data !== undefined) {
      const nested = maybeResponse.data as any;
      if (nested && typeof nested === 'object') {
        if ('data' in nested && nested.data !== undefined && typeof nested.data === 'object') {
          return nested.data as SecurityScore;
        }
        if ('overallScore' in nested || 'documentsScore' in nested || 'complianceScore' in nested) {
          return nested as SecurityScore;
        }
      }
    }
    if ('overallScore' in payload || 'documentsScore' in payload || 'complianceScore' in payload) {
      return payload as SecurityScore;
    }
  }

  return null;
}

export default function SecurityScorePage() {
  const [score, setScore] = useState<SecurityScore | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const calculated = await securityApi.calculate();
      const freshScore = unwrapSecurityScore(calculated?.data);

      if (freshScore) {
        setScore(freshScore);
        return;
      }

      const res = await securityApi.getLatest();
      setScore(unwrapSecurityScore(res?.data) ?? null);
    } catch (e) {
      console.error(e);
      setError('Impossible de charger le score de sécurité.');
    } finally {
      setLoading(false);
    }
  };

  const calculate = async () => {
    setLoading(true);
    try {
      const res = await securityApi.calculate();
      setScore(unwrapSecurityScore(res?.data) ?? null);
    } catch (e) {
      console.error(e);
      setError('Impossible de calculer le score.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const formatDate = (d?: string) => {
    if (!d) return new Date().toLocaleString('fr-FR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    try {
      const date = new Date(d);
      // Use local computer time
      return date.toLocaleString('fr-FR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    } catch { return new Date().toLocaleString('fr-FR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }); }
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Score de sécurité</h2>
          <p className="page-subtitle">Indice global de sécurité du système</p>
        </div>
        <button className="btn-primary" onClick={calculate} disabled={loading}>
          {loading ? 'Calcul...' : 'Recalculer'}
        </button>
      </div>

      {error && (
        <div className="alert alert-error">
          <strong>Erreur:</strong> {error}
        </div>
      )}

      {score ? (
        <div className="score-overview">
          <div className="score-circle-large">
            <div className="score-value-large">{score.overallScore ?? 0}%</div>
            <div className="score-label">Score global</div>
            <div className="score-status">Calculé le {formatDate(score.calculatedAt)}</div>
          </div>
        </div>
      ) : (
        <div className="info-card">
          <p>Aucun score disponible. Cliquez sur "Recalculer" pour générer un score.</p>
        </div>
      )}

      <div className="info-cards-grid">
        <div className="info-card">
          <h3>Documents</h3>
          <ul>
            <li>Score: {score?.documentsScore ?? 0}%</li>
            <li>Classification des documents</li>
          </ul>
        </div>
        <div className="info-card">
          <h3>Risques</h3>
          <ul>
            <li>Score: {score?.risksScore ?? 0}%</li>
            <li>Gestion des risques</li>
          </ul>
        </div>
        <div className="info-card">
          <h3>Conformité</h3>
          <ul>
            <li>Score: {score?.complianceScore ?? 0}%</li>
            <li>Référentiels appliqués</li>
          </ul>
        </div>
        <div className="info-card">
          <h3>Recommandations</h3>
          <ul>
            <li>Score: {score?.recommendationsScore ?? 0}%</li>
            <li>Actions correctives</li>
          </ul>
        </div>
      </div>
    </div>
  );
}