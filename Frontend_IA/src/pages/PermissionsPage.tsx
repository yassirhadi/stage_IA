import { useEffect, useState } from 'react';
import { adminApi } from '../api/services';
import { Button } from '@/components/ui/button';
import { Plus, Edit, Trash2, Eye, Shield, Search } from 'lucide-react';
import { normalizePermissions } from '../lib/adminData';

interface Permission {
  id: number;
  name: string;
  description: string;
  category: string;
  module: string;
  roles: string[];
  roleCount: number;
  isCritical: boolean;
}

const ROLES = ['ADMIN', 'RSSI', 'AUDITEUR', 'LECTEUR', 'ANALYSTE'];

export default function PermissionsPage() {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterCategory, setFilterCategory] = useState('Toutes');
  const [filterModule, setFilterModule] = useState('Tous');
  
  // Modals
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedPermission, setSelectedPermission] = useState<Permission | null>(null);
  
  // Form state
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: '',
    module: '',
    roles: [] as string[]
  });

  const loadPermissions = async () => {
    try {
      const res = await adminApi.getPermissions();
      const data = Array.isArray(res.data.data) ? res.data.data : [];
      setPermissions(normalizePermissions(data));
    } catch (error) {
      console.error('Error loading permissions:', error);
    }
  };

  useEffect(() => {
    loadPermissions();
  }, []);

  const categories = Array.from(new Set(permissions.map(p => p.category)));
  const modules = Array.from(new Set(permissions.map(p => p.module)));

  const filteredPermissions = permissions.filter(p => {
    if (searchTerm) {
      const s = searchTerm.toLowerCase();
      if (!p.name.toLowerCase().includes(s) && !p.description.toLowerCase().includes(s)) return false;
    }
    if (filterCategory !== 'Toutes' && p.category !== filterCategory) return false;
    if (filterModule !== 'Tous' && p.module !== filterModule) return false;
    return true;
  });

  const getCategoryColor = (category: string) => {
    const colors: Record<string, string> = {
      'Utilisateurs': 'bg-blue-900/30 text-blue-200 border-blue-700',
      'Documents': 'bg-green-900/30 text-green-200 border-green-700',
      'Rapports': 'bg-purple-900/30 text-purple-200 border-purple-700',
      'Risques': 'bg-red-900/30 text-red-200 border-red-700',
      'Administration': 'bg-orange-900/30 text-orange-200 border-orange-700',
      'Paramètres': 'bg-gray-900/30 text-gray-200 border-gray-700',
      'Données sensibles': 'bg-pink-900/30 text-pink-200 border-pink-700',
      'Export': 'bg-cyan-900/30 text-cyan-200 border-cyan-700',
      'Sauvegardes': 'bg-yellow-900/30 text-yellow-200 border-yellow-700',
      'Inventaire': 'bg-indigo-900/30 text-indigo-200 border-indigo-700',
    };
    return colors[category] || 'bg-zinc-900/30 text-zinc-200 border-zinc-700';
  };

  const handleAddPermission = async () => {
    if (!formData.name || !formData.description || !formData.category || !formData.module) {
      alert('Veuillez remplir tous les champs');
      return;
    }

    try {
      await adminApi.createPermission({
        name: formData.name,
        description: formData.description,
        category: formData.category,
        module: formData.module,
      });
      setShowAddModal(false);
      setFormData({ name: '', description: '', category: '', module: '', roles: [] });
      await loadPermissions();
    } catch (error) {
      console.error('Error creating permission:', error);
      alert('Erreur lors de la création de la permission');
    }
  };

  const handleEditPermission = async () => {
    if (!selectedPermission) return;

    try {
      await adminApi.updatePermission(selectedPermission.id, {
        name: formData.name,
        description: formData.description,
        category: formData.category,
        module: formData.module,
      });
      setShowEditModal(false);
      await loadPermissions();
    } catch (error) {
      console.error('Error updating permission:', error);
      alert('Erreur lors de la modification de la permission');
    }
  };

  const handleDeletePermission = async (permission: Permission) => {
    if (permission.isCritical) {
      alert('Impossible de supprimer une permission critique');
      return;
    }
    if (confirm(`Êtes-vous sûr de vouloir supprimer la permission "${permission.name}" ?`)) {
      try {
        await adminApi.deletePermission(permission.id);
        await loadPermissions();
      } catch (error) {
        console.error('Error deleting permission:', error);
        alert('Erreur lors de la suppression de la permission');
      }
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Gestion des permissions</h2>
          <p className="text-muted-foreground">Gérer les permissions du système</p>
        </div>
        <Button onClick={() => setShowAddModal(true)}>
          <Plus size={16} style={{ marginRight: 8 }} />
          Ajouter une permission
        </Button>
      </div>

      {/* Filters */}
      <div className="card-panel mb-6">
        <div className="flex flex-wrap gap-4 items-center">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
            <input
              type="text"
              placeholder="Rechercher une permission..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <select
            value={filterCategory}
            onChange={e => setFilterCategory(e.target.value)}
            className="px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="Toutes">Toutes les catégories</option>
            {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
          </select>
          <select
            value={filterModule}
            onChange={e => setFilterModule(e.target.value)}
            className="px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="Tous">Tous les modules</option>
            {modules.map(mod => <option key={mod} value={mod}>{mod}</option>)}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="border rounded-lg overflow-hidden border-zinc-700">
        {filteredPermissions.length === 0 ? (
          <div className="p-8 text-center text-zinc-400">Aucune permission trouvée</div>
        ) : (
          <table className="w-full text-left text-sm text-zinc-200">
            <thead className="bg-zinc-800 text-zinc-400 border-b border-zinc-700">
              <tr>
                <th className="px-4 py-3">Nom</th>
                <th className="px-4 py-3">Description</th>
                <th className="px-4 py-3">Catégorie</th>
                <th className="px-4 py-3">Module</th>
                <th className="px-4 py-3">Rôles associés</th>
                <th className="px-4 py-3">Nb rôles</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-700">
              {filteredPermissions.map((p) => (
                <tr key={p.id} className="hover:bg-zinc-800/50">
                  <td className="px-4 py-3 font-medium">
                    <div className="flex items-center gap-2">
                      {p.isCritical && <Shield size={16} className="text-red-400" />}
                      {p.name}
                    </div>
                  </td>
                  <td className="px-4 py-3 max-w-xs truncate" title={p.description}>{p.description}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getCategoryColor(p.category)}`}>
                      {p.category}
                    </span>
                  </td>
                  <td className="px-4 py-3">{p.module}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1.5">
                      {p.roles.map((role) => (
                        <span key={role} className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-zinc-700 text-zinc-300 border border-zinc-600">
                          {role}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3">{p.roleCount}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => { setSelectedPermission(p); setShowDetailsModal(true); }}>
                        <Eye size={16} />
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => {
                        setSelectedPermission(p);
                        setFormData({ name: p.name, description: p.description, category: p.category, module: p.module, roles: p.roles });
                        setShowEditModal(true);
                      }}>
                        <Edit size={16} />
                      </Button>
                      <Button 
                        variant="destructive" 
                        size="sm" 
                        onClick={() => handleDeletePermission(p)}
                        disabled={p.isCritical}
                      >
                        <Trash2 size={16} />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Add Permission Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4" onClick={() => setShowAddModal(false)}>
          <div className="bg-zinc-900 text-zinc-100 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-zinc-700" onClick={(e) => e.stopPropagation()}>
            <div className="p-6 border-b border-zinc-700 flex justify-between items-center">
              <h2 className="text-xl font-bold">Ajouter une permission</h2>
              <Button variant="ghost" onClick={() => setShowAddModal(false)} className="text-zinc-100 hover:bg-zinc-800 hover:text-white">✕</Button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Nom</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={e => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Catégorie</label>
                <select
                  value={formData.category}
                  onChange={e => setFormData({ ...formData, category: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Sélectionner une catégorie</option>
                  {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Module</label>
                <select
                  value={formData.module}
                  onChange={e => setFormData({ ...formData, module: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Sélectionner un module</option>
                  {modules.map(mod => <option key={mod} value={mod}>{mod}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-2">Rôles associés</label>
                <div className="grid grid-cols-2 gap-2">
                  {ROLES.map(role => (
                    <label key={role} className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={formData.roles.includes(role)}
                        onChange={e => {
                          if (e.target.checked) {
                            setFormData({ ...formData, roles: [...formData.roles, role] });
                          } else {
                            setFormData({ ...formData, roles: formData.roles.filter(r => r !== role) });
                          }
                        }}
                        className="rounded bg-zinc-800 border-zinc-600"
                      />
                      <span className="text-zinc-300">{role}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" onClick={() => setShowAddModal(false)}>Annuler</Button>
                <Button onClick={handleAddPermission}>Créer</Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Edit Permission Modal */}
      {showEditModal && selectedPermission && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4" onClick={() => setShowEditModal(false)}>
          <div className="bg-zinc-900 text-zinc-100 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-zinc-700" onClick={(e) => e.stopPropagation()}>
            <div className="p-6 border-b border-zinc-700 flex justify-between items-center">
              <h2 className="text-xl font-bold">Modifier la permission</h2>
              <Button variant="ghost" onClick={() => setShowEditModal(false)} className="text-zinc-100 hover:bg-zinc-800 hover:text-white">✕</Button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Nom</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  disabled={selectedPermission.isCritical}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={e => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Catégorie</label>
                <select
                  value={formData.category}
                  onChange={e => setFormData({ ...formData, category: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Sélectionner une catégorie</option>
                  {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1">Module</label>
                <select
                  value={formData.module}
                  onChange={e => setFormData({ ...formData, module: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md bg-zinc-800 text-zinc-100 border-zinc-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Sélectionner un module</option>
                  {modules.map(mod => <option key={mod} value={mod}>{mod}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-2">Rôles associés</label>
                <div className="grid grid-cols-2 gap-2">
                  {ROLES.map(role => (
                    <label key={role} className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={formData.roles.includes(role)}
                        onChange={e => {
                          if (e.target.checked) {
                            setFormData({ ...formData, roles: [...formData.roles, role] });
                          } else {
                            setFormData({ ...formData, roles: formData.roles.filter(r => r !== role) });
                          }
                        }}
                        className="rounded bg-zinc-800 border-zinc-600"
                      />
                      <span className="text-zinc-300">{role}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <Button variant="outline" onClick={() => setShowEditModal(false)}>Annuler</Button>
                <Button onClick={handleEditPermission}>Enregistrer</Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Details Modal */}
      {showDetailsModal && selectedPermission && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4" onClick={() => setShowDetailsModal(false)}>
          <div className="bg-zinc-900 text-zinc-100 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-zinc-700" onClick={(e) => e.stopPropagation()}>
            <div className="p-6 border-b border-zinc-700 flex justify-between items-center">
              <h2 className="text-xl font-bold">Détails de la permission</h2>
              <Button variant="ghost" onClick={() => setShowDetailsModal(false)} className="text-zinc-100 hover:bg-zinc-800 hover:text-white">✕</Button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <span className="text-zinc-400">Nom:</span>
                <div className="flex items-center gap-2 mt-1">
                  {selectedPermission.isCritical && <Shield size={16} className="text-red-400" />}
                  <span className="text-zinc-100">{selectedPermission.name}</span>
                </div>
              </div>
              <div>
                <span className="text-zinc-400">Description:</span>
                <p className="text-zinc-300 mt-1">{selectedPermission.description}</p>
              </div>
              <div>
                <span className="text-zinc-400">Catégorie:</span>
                <span className={`ml-2 inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${getCategoryColor(selectedPermission.category)}`}>
                  {selectedPermission.category}
                </span>
              </div>
              <div>
                <span className="text-zinc-400">Module:</span>
                <span className="ml-2 text-zinc-100">{selectedPermission.module}</span>
              </div>
              <div>
                <span className="text-zinc-400">Critique:</span>
                <span className={`ml-2 inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${
                  selectedPermission.isCritical ? 'bg-red-900/30 text-red-200 border-red-700' : 'bg-green-900/30 text-green-200 border-green-700'
                }`}>
                  {selectedPermission.isCritical ? 'Oui' : 'Non'}
                </span>
              </div>
              <div>
                <span className="text-zinc-400">Rôles associés ({selectedPermission.roleCount}):</span>
                <div className="mt-2 flex flex-wrap gap-2">
                  {selectedPermission.roles.map(role => (
                    <span key={role} className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-zinc-700 text-zinc-300">
                      {role}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
