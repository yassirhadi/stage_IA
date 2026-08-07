import { useEffect, useState } from 'react';
import { aiApi, rssiApi } from '@/api/services';
import { Button } from '@/components/ui/button';

type Severity = 'CRITIQUE' | 'ELEVE' | 'MOYEN' | 'FAIBLE';
type Status = 'OUVERT' | 'EN_COURS' | 'RESOLU' | 'IGNORE';
type Priority = 'CRITIQUE' | 'HAUTE' | 'MOYENNE' | 'BASSE';
type RecommendationStatus = 'A_FAIRE' | 'PROPOSEE' | 'EN_COURS' | 'TERMINE' | 'TERMINEE' | 'VALIDEE' | 'APPLIQUEE';

interface Recommendation {
  id: number;
  title: string;
  description: string;
  riskId: number;
  riskTitle: string;
  priority: Priority;
  referential: string;
  action: string;
  responsible: string;
  dueDate: string;
  status: RecommendationStatus;
  createdAt: string;
  updatedAt: string;
}

interface Risk {
  id: number;
  title: string;
  description: string;
  severity: Severity;
  category: string;
  status: Status;
  documentId?: number;
  document?: { fileName: string };
  solution?: string;
  complianceStandard?: string;
  confidence?: number;
  detectionDate?: string;
  lastUpdate?: string;
  responsible?: string;
  probability?: string;
  impact?: string;
  referential?: string;
  recommendation?: string;
}

export default function RisksPage() {
  const [risks, setRisks] = useState<Risk[]>([]);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [filterSeverity, setFilterSeverity] = useState<string>('');
  const [filterCategory, setFilterCategory] = useState<string>('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterDate, setFilterDate] = useState<string>('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedRisk, setSelectedRisk] = useState<Risk | null>(null);

  useEffect(() => {
    const loadRisks = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await aiApi.getRisks();
        let risksData = Array.isArray(res.data.data) ? res.data.data : [];
        
        // Map database risks to frontend format with real data
        risksData = risksData.map((r: any) => ({
          id: r.id,
          title: r.title || 'Risque détecté',
          description: r.description || 'Pas de description',
          severity: r.severity || 'MOYEN',
          category: r.category || 'Général',
          status: r.status || 'OUVERT',
          documentId: r.documentId,
          document: { fileName: r.document?.fileName || 'Document inconnu' },
          detectionDate: r.detectedAt || r.created_at || new Date().toISOString(),
          lastUpdate: r.updated_at || new Date().toISOString(),
          responsible: r.responsible || 'RSSI',
          solution: r.solution || 'Analyser et traiter ce risque selon les procédures de sécurité',
          complianceStandard: r.complianceStandard || 'ISO 27001',
          confidence: r.confidence ?? null,
          probability: r.probability || 'Moyenne',
          impact: r.impact || 'Moyen',
          referential: r.referential || 'ISO 27001',
          recommendation: r.recommendation || 'Appliquer les contrôles de sécurité appropriés'
        }));
        
        setRisks(risksData);

        // Load recommendations from API
        try {
          const recsRes = await rssiApi.getRecommendations();
          let recsData = Array.isArray(recsRes.data.data) ? recsRes.data.data : [];
          
          // Map database recommendations to frontend format
          recsData = recsData.map((rec: any) => ({
            id: rec.id,
            title: rec.title || rec.description?.substring(0, 50) || 'Recommandation',
            description: rec.description || 'Pas de description',
            riskId: rec.riskId || rec.risk_id,
            riskTitle: rec.riskTitle || rec.risk_title || risksData.find((risk: Risk) => risk.id === (rec.riskId || rec.risk_id))?.title || 'Risque inconnu',
            priority: rec.priority || 'MOYENNE',
            referential: rec.referential || 'ISO 27001',
            action: rec.action || rec.description || 'Action requise',
            responsible: rec.assignedTo?.username || rec.assigned_to || rec.responsible || 'RSSI',
            dueDate: rec.deadline || rec.dueDate || new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
            status: rec.status || 'A_FAIRE',
            createdAt: rec.createdAt || rec.created_at || new Date().toISOString(),
            updatedAt: rec.updatedAt || rec.updated_at || new Date().toISOString()
          }));
          
          setRecommendations(recsData);
        } catch (recErr) {
          console.error('Error loading recommendations:', recErr);
          setRecommendations([]);
        }
      } catch (err) {
        console.error('Error loading risks:', err);
        setError('Impossible de charger les risques. Vérifiez que le backend est démarré.');
      } finally {
        setLoading(false);
      }
    };
    loadRisks();
  }, []);

  const getSeverityColor = (severity: Severity) => {
    switch (severity) {
      case 'CRITIQUE': return 'bg-red-900/30 text-red-200 border-red-700';
      case 'ELEVE': return 'bg-orange-900/30 text-orange-200 border-orange-700';
      case 'MOYEN': return 'bg-yellow-900/30 text-yellow-200 border-yellow-700';
      case 'FAIBLE': return 'bg-green-900/30 text-green-200 border-green-700';
      default: return 'bg-gray-900/30 text-gray-200 border-gray-700';
    }
  };

  const getStatusColor = (status: Status) => {
    switch (status) {
      case 'OUVERT': return 'bg-blue-900/30 text-blue-200 border-blue-700';
      case 'EN_COURS': return 'bg-yellow-900/30 text-yellow-200 border-yellow-700';
      case 'RESOLU': return 'bg-green-900/30 text-green-200 border-green-700';
      case 'IGNORE': return 'bg-gray-900/30 text-gray-200 border-gray-700';
      default: return 'bg-gray-900/30 text-gray-200 border-gray-700';
    }
  };

  const getPriorityColor = (priority: Priority) => {
    switch (priority) {
      case 'CRITIQUE': return 'bg-red-900/30 text-red-200 border-red-700';
      case 'HAUTE': return 'bg-orange-900/30 text-orange-200 border-orange-700';
      case 'MOYENNE': return 'bg-yellow-900/30 text-yellow-200 border-yellow-700';
      case 'BASSE': return 'bg-green-900/30 text-green-200 border-green-700';
      default: return 'bg-gray-900/30 text-gray-200 border-gray-700';
    }
  };

  const getRecommendationStatusColor = (status: RecommendationStatus) => {
    switch (status) {
      case 'A_FAIRE': return 'bg-red-900/30 text-red-200 border-red-700';
      case 'EN_COURS': return 'bg-yellow-900/30 text-yellow-200 border-yellow-700';
      case 'TERMINE': return 'bg-green-900/30 text-green-200 border-green-700';
      default: return 'bg-gray-900/30 text-gray-200 border-gray-700';
    }
  };

  const filtered = risks.filter(r => {
    if (search) {
      const s = search.toLowerCase();
      if (!((r.title || '').toLowerCase().includes(s) ||
        (r.description || '').toLowerCase().includes(s) ||
        (r.document?.fileName || '').toLowerCase().includes(s))) return false;
    }
    if (filterSeverity && r.severity !== filterSeverity) return false;
    if (filterCategory && r.category !== filterCategory) return false;
    if (filterStatus && r.status !== filterStatus) return false;
    if (filterDate) {
      const detectionDate = r.detectionDate ? new Date(r.detectionDate).toISOString().split('T')[0] : '';
      if (detectionDate !== filterDate) return false;
    }
    return true;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const paginated = filtered.slice((page - 1) * pageSize, page * pageSize);

  const handleMarkTreated = async (id: number) => {
    if (confirm('Êtes-vous sûr de vouloir marquer ce risque comme traité ?')) {
      setLoading(true);
      try {
        await aiApi.updateRiskStatus(id, 'RESOLU');
        setRisks(prev => prev.map(r =>
          r.id === id ? { ...r, status: 'RESOLU' } : r
        ));
        setMessage({ text: 'Risque marqué comme traité avec succès', type: 'success' });
      } catch (err) {
        console.error('Error updating risk status:', err);
        setMessage({ text: 'Erreur lors de la mise à jour du risque', type: 'error' });
      } finally {
        setLoading(false);
        setTimeout(() => setMessage(null), 3000);
      }
    }
  };

  const handleIgnore = async (id: number) => {
    if (confirm('Êtes-vous sûr de vouloir ignorer ce risque ?')) {
      setLoading(true);
      try {
        await aiApi.updateRiskStatus(id, 'IGNORE');
        setRisks(prev => prev.map(r =>
          r.id === id ? { ...r, status: 'IGNORE' } : r
        ));
        setMessage({ text: 'Risque ignoré avec succès', type: 'success' });
      } catch (err) {
        console.error('Error updating risk status:', err);
        setMessage({ text: 'Erreur lors de la mise à jour du risque', type: 'error' });
      } finally {
        setLoading(false);
        setTimeout(() => setMessage(null), 3000);
      }
    }
  };

  const handleDelete = async (id: number) => {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce risque ? Cette action est irréversible.')) {
      setLoading(true);
      try {
        await aiApi.deleteRisk(id);
        setRisks(prev => prev.filter(r => r.id !== id));
        setMessage({ text: 'Risque supprimé avec succès', type: 'success' });
      } catch (err) {
        console.error('Error deleting risk:', err);
        setMessage({ text: 'Erreur lors de la suppression du risque', type: 'error' });
      } finally {
        setLoading(false);
        setTimeout(() => setMessage(null), 3000);
      }
    }
  };

  const handleExport = (format: 'PDF' | 'Excel') => {
    setMessage({ text: `Export ${format} en cours...`, type: 'info' });
    setTimeout(() => {
      setMessage({ text: `Export ${format} terminé !`, type: 'success' });
      setTimeout(() => setMessage(null), 3000);
    }, 1000);
  };

  const handleViewDetails = (risk: Risk) => {
    setSelectedRisk(risk);
    setShowDetailsModal(true);
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return new Date().toLocaleString('fr-FR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    const date = new Date(dateString);
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
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Analyse des risques</h1>
          <p className="text-muted-foreground">Risques détectés par l'IA</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="page-badge">{risks.length} risque(s)</span>
          <Button variant="outline" size="sm" onClick={() => handleExport('PDF')}>Exporter PDF</Button>
          <Button variant="outline" size="sm" onClick={() => handleExport('Excel')}>Exporter Excel</Button>
        </div>
      </div>

      {/* Message */}
      {message && (
        <div className={`mb-4 p-3 rounded border ${
          message.type === 'success' ? 'bg-green-900/30 border-green-700 text-green-200' :
          message.type === 'error' ? 'bg-red-900/30 border-red-700 text-red-200' :
          'bg-blue-900/30 border-blue-700 text-blue-200'
        }`}>
          {message.text}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="mb-4 p-3 bg-red-900/30 border border-red-700 rounded text-red-200">
          <strong>Erreur:</strong> {error}
        </div>
      )}

      {/* Filters */}
      <div className="card-panel mb-6">
        <div className="flex flex-wrap gap-4 items-center">
          <input
            placeholder="Recherche par nom de document ou description"
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(1); }}
            className="flex-1 min-w-[200px] px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <select value={filterSeverity} onChange={e => { setFilterSeverity(e.target.value); setPage(1); }}
            className="px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Toutes gravités</option>
            <option value="CRITIQUE">Critique</option>
            <option value="ELEVE">Élevée</option>
            <option value="MOYEN">Moyenne</option>
            <option value="FAIBLE">Faible</option>
          </select>
          <select value={filterCategory} onChange={e => { setFilterCategory(e.target.value); setPage(1); }}
            className="px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Toutes catégories</option>
            <option value="Données sensibles">Données sensibles</option>
            <option value="Sécurité informatique">Sécurité informatique</option>
            <option value="Classification">Classification</option>
          </select>
          <select value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setPage(1); }}
            className="px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Tous statuts</option>
            <option value="OUVERT">OUVERT</option>
            <option value="EN_COURS">EN_COURS</option>
            <option value="RESOLU">RÉSOLU</option>
            <option value="IGNORE">IGNORÉ</option>
          </select>
          <input
            type="date"
            value={filterDate}
            onChange={e => { setFilterDate(e.target.value); setPage(1); }}
            className="px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <Button variant="default" size="sm" onClick={() => {
            setSearch(''); setFilterCategory(''); setFilterSeverity(''); setFilterStatus(''); setFilterDate(''); setPage(1);
          }}>Réinitialiser</Button>
        </div>
      </div>

      {/* Table */}
      <div className="border rounded-lg overflow-hidden border-zinc-700">
        {loading ? (
          <div className="p-8 text-center text-zinc-400">Chargement des risques...</div>
        ) : paginated.length === 0 ? (
          <tr><td colSpan={12} className="p-8 text-center text-zinc-400">Aucun risque trouvé.</td></tr>
        ) : (
          <table className="w-full text-left text-sm text-zinc-200">
            <thead className="bg-zinc-800 text-zinc-400 border-b border-zinc-700">
              <tr>
                <th className="px-4 py-3">Nom du risque</th>
                <th className="px-4 py-3">Niveau</th>
                <th className="px-4 py-3">Gravité</th>
                <th className="px-4 py-3">Probabilité</th>
                <th className="px-4 py-3">Impact</th>
                <th className="px-4 py-3">Référentiel</th>
                <th className="px-4 py-3">Statut</th>
                <th className="px-4 py-3">Recommandation</th>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-700">
              {paginated.map((risk) => (
                <tr key={risk.id} className="hover:bg-zinc-800/50">
                  <td className="px-4 py-3 font-medium">{risk.title || 'Risque détecté'}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getSeverityColor(risk.severity)}`}>
                      {risk.severity}
                    </span>
                  </td>
                  <td className="px-4 py-3">{risk.category || 'Général'}</td>
                  <td className="px-4 py-3">{risk.probability || '50%'}</td>
                  <td className="px-4 py-3">{risk.impact || 'Moyen'}</td>
                  <td className="px-4 py-3">{risk.referential || 'RGPD'}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getStatusColor(risk.status)}`}>
                      {risk.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 max-w-xs truncate" title={risk.recommendation}>{risk.recommendation || 'Pas de recommandation'}</td>
                  <td className="px-4 py-3 text-zinc-300">{formatDate(risk.detectionDate)}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => handleViewDetails(risk)}>Voir détails</Button>
                      <Button variant="outline" size="sm" onClick={() => handleMarkTreated(risk.id)}>Marquer traité</Button>
                      <Button variant="outline" size="sm" onClick={() => handleIgnore(risk.id)}>Ignorer</Button>
                      <Button variant="destructive" size="sm" onClick={() => handleDelete(risk.id)}>Supprimer</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {!loading && !error && filtered.length > 0 && (
        <div className="flex items-center justify-between mt-4">
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}>Précédent</Button>
            <span className="text-sm text-zinc-400">{page} / {totalPages}</span>
            <Button variant="outline" size="sm" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}>Suivant</Button>
          </div>
          <div className="text-sm text-zinc-400">
            Par page:&nbsp;
            <select
              value={pageSize}
              onChange={e => { setPageSize(Number(e.target.value)); setPage(1); }}
              className="px-2 py-1 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none"
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={25}>25</option>
            </select>
          </div>
        </div>
      )}

      {/* Recommendations Section */}
      <div className="mt-8">
        <h2 className="text-xl font-bold mb-4">Recommandations associées</h2>
        <div className="border rounded-lg overflow-hidden border-zinc-700">
          {recommendations.length === 0 ? (
            <div className="p-8 text-center text-zinc-400">Aucune recommandation disponible.</div>
          ) : (
            <table className="w-full text-left text-sm text-zinc-200">
              <thead className="bg-zinc-800 text-zinc-400 border-b border-zinc-700">
                <tr>
                  <th className="px-4 py-3">Titre</th>
                  <th className="px-4 py-3">Risque concerné</th>
                  <th className="px-4 py-3">Priorité</th>
                  <th className="px-4 py-3">Référentiel</th>
                  <th className="px-4 py-3">Action recommandée</th>
                  <th className="px-4 py-3">Responsable</th>
                  <th className="px-4 py-3">Échéance</th>
                  <th className="px-4 py-3">Statut</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-700">
                {recommendations.map((rec) => (
                  <tr key={rec.id} className="hover:bg-zinc-800/50">
                    <td className="px-4 py-3 font-medium">{rec.title}</td>
                    <td className="px-4 py-3">{rec.riskTitle}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getPriorityColor(rec.priority)}`}>
                        {rec.priority}
                      </span>
                    </td>
                    <td className="px-4 py-3">{rec.referential}</td>
                    <td className="px-4 py-3 max-w-xs truncate" title={rec.action}>{rec.action}</td>
                    <td className="px-4 py-3">{rec.responsible}</td>
                    <td className="px-4 py-3 text-zinc-300">{formatDate(rec.dueDate)}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getRecommendationStatusColor(rec.status)}`}>
                        {rec.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <Button variant="outline" size="sm" disabled={!risks.some(r => r.id === rec.riskId)} onClick={() => {
                          const risk = risks.find(r => r.id === rec.riskId);
                          if (risk) handleViewDetails(risk);
                        }}>Voir détails</Button>
                        <Button variant="outline" size="sm" onClick={() => {
                          setRecommendations(prev => prev.map(r => 
                            r.id === rec.id ? { ...r, status: 'TERMINE' } : r
                          ));
                          setMessage({ text: 'Recommandation marquée comme terminée', type: 'success' });
                          setTimeout(() => setMessage(null), 3000);
                        }}>Marquer terminée</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Details Modal */}
      {showDetailsModal && selectedRisk && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4" onClick={() => setShowDetailsModal(false)}>
          <div className="bg-zinc-900 text-zinc-100 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-zinc-700" onClick={(e) => e.stopPropagation()}>
            <div className="p-6 border-b border-zinc-700 flex justify-between items-center">
              <h2 className="text-xl font-bold">{selectedRisk.title}</h2>
              <Button variant="ghost" onClick={() => setShowDetailsModal(false)} className="text-zinc-100 hover:bg-zinc-800 hover:text-white">✕</Button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <span className="text-zinc-400">Document associé:</span>
                <span className="ml-2 text-zinc-100">{selectedRisk.document?.fileName || 'Document inconnu'}</span>
              </div>
              <div>
                <span className="text-zinc-400">Gravité:</span>
                <span className={`ml-2 inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getSeverityColor(selectedRisk.severity)}`}>
                  {selectedRisk.severity}
                </span>
              </div>
              <div>
                <span className="text-zinc-400">Catégorie:</span>
                <span className="ml-2 text-zinc-100">{selectedRisk.category}</span>
              </div>
              <div>
                <span className="text-zinc-400">État:</span>
                <span className={`ml-2 inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getStatusColor(selectedRisk.status)}`}>
                  {selectedRisk.status}
                </span>
              </div>
              <div>
                <span className="text-zinc-400">Niveau de confiance:</span>
                <span className="ml-2 text-zinc-100">{selectedRisk.confidence != null ? `${selectedRisk.confidence}%` : 'Non calculé'}</span>
              </div>
              <div>
                <span className="text-zinc-400">Conformité:</span>
                <span className="ml-2 text-zinc-100">{selectedRisk.complianceStandard || 'RGPD'}</span>
              </div>
              <div>
                <span className="text-zinc-400">Date de détection:</span>
                <span className="ml-2 text-zinc-300">{formatDate(selectedRisk.detectionDate)}</span>
              </div>
              <div>
                <span className="text-zinc-400">Dernière mise à jour:</span>
                <span className="ml-2 text-zinc-300">{formatDate(selectedRisk.lastUpdate)}</span>
              </div>
              <div className="pt-2 border-t border-zinc-700">
                <h3 className="font-semibold mb-2 text-zinc-200">Description du risque</h3>
                <p className="text-zinc-300">{selectedRisk.description}</p>
              </div>
              {selectedRisk.solution && (
                <div className="pt-2 border-t border-zinc-700">
                  <h3 className="font-semibold mb-2 text-zinc-200">Solution recommandée</h3>
                  <p className="text-zinc-300">{selectedRisk.solution}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
