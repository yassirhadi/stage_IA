export interface NormalizedPermission {
  id: number;
  name: string;
  description: string;
  category: string;
  module: string;
  roles: string[];
  roleCount: number;
  isCritical: boolean;
}

const resolveCategory = (name: string) => {
  const value = (name || '').toLowerCase();
  if (value.includes('user')) return 'Utilisateurs';
  if (value.includes('document')) return 'Documents';
  if (value.includes('report')) return 'Rapports';
  if (value.includes('risk')) return 'Risques';
  if (value.includes('setting') || value.includes('permission') || value.includes('role')) return 'Administration';
  if (value.includes('backup')) return 'Sauvegardes';
  if (value.includes('inventory')) return 'Inventaire';
  if (value.includes('sensitive')) return 'Données sensibles';
  return 'Autre';
};

const resolveModule = (name: string) => {
  const value = (name || '').toLowerCase();
  if (value.includes('user')) return 'Users';
  if (value.includes('document')) return 'Documents';
  if (value.includes('report')) return 'Reports';
  if (value.includes('risk')) return 'Risks';
  if (value.includes('setting')) return 'Settings';
  if (value.includes('permission')) return 'Admin';
  if (value.includes('role')) return 'Admin';
  if (value.includes('backup')) return 'Backups';
  if (value.includes('inventory')) return 'Inventory';
  if (value.includes('sensitive')) return 'SensitiveData';
  return 'Autre';
};

export const normalizePermissions = (data: any[]): NormalizedPermission[] => {
  return (data || []).map((item: any, index: number) => {
    const name = item?.name || `permission_${index + 1}`;
    const roles = Array.isArray(item?.roles) ? item.roles.map((role: any) => String(role?.name || role || '')) : [];

    return {
      id: item?.id ?? index + 1,
      name,
      description: item?.description || 'Aucune description',
      category: item?.category || resolveCategory(name),
      module: item?.module || resolveModule(name),
      roles: roles.filter(Boolean),
      roleCount: roles.length,
      isCritical: /delete|create|manage|edit/i.test(name),
    };
  });
};
