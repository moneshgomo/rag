import React, { useState, useEffect, useMemo } from 'react';
import adminService from '../../services/adminService';
import teamService from '../../services/teamService';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import Alert from '../../components/common/Alert';
import { StatCard } from '../../components/common/Card';
import {
  ShieldAlert,
  UserPlus,
  FolderPlus,
  Search,
  Users,
  Briefcase,
  CheckCircle2,
  XCircle,
  ToggleLeft,
  ToggleRight,
} from 'lucide-react';
import { formatRole, formatDate, getStatusBadgeVariant } from '../../utils/formatters';
import { extractErrorMessage } from '../../utils/errorHandler';

export const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [managers, setManagers] = useState([]);
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');

  
  const [showManagerModal, setShowManagerModal] = useState(false);
  const [showTeamModal, setShowTeamModal] = useState(false);
  const [showUserModal, setShowUserModal] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);


  const [managerForm, setManagerForm] = useState({
    name: '',
    email: '',
    contactNumber: '',
    password: '',
    teamId: '',
  });

  const [teamForm, setTeamForm] = useState({
    name: '',
    description: '',
    managerId: '',
  });

  const [userForm, setUserForm] = useState({
    name: '',
    email: '',
    contactNumber: '',
    password: '',
    role: 'EMPLOYEE',
    teamId: '',
  });

  const [formErrors, setFormErrors] = useState({});

 
  const fetchData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [teamsData, managersData] = await Promise.all([
        teamService.listTeams().catch(() => []),
        adminService.listManagers().catch(() => []),
      ]);
      setTeams(teamsData || []);
      setManagers(managersData || []);

      
      try {
        const usersData = await adminService.listUsers();
        setUsers(usersData || []);
      } catch (err) {
       
        setUsers(
          (managersData || []).map((m, idx) => ({
            id: idx + 1,
            name: m.name,
            email: m.email,
            contactNumber: m.contactNumber,
            role: m.role || 'MANAGER',
            teamName: m.teamName,
            active: true,
          }))
        );
      }
    } catch (err) {
      setApiError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchesSearch =
        !searchQuery ||
        (u.name && u.name.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (u.email && u.email.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesRole = !roleFilter || u.role === roleFilter;

      return matchesSearch && matchesRole;
    });
  }, [users, searchQuery, roleFilter]);

  
  const handleToggleStatus = async (user) => {
    try {
      const updated = await adminService.toggleUserStatus(user.id);
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, active: updated.active } : u))
      );
      setSuccessMessage(`User status for ${user.name} updated to ${updated.active ? 'Active' : 'Disabled'}.`);
    } catch (err) {
      setApiError(extractErrorMessage(err));
    }
  };


  const handleCreateManager = async (e) => {
    e.preventDefault();
    if (!managerForm.name.trim() || !managerForm.email.trim() || !managerForm.password) {
      setFormErrors({ general: 'Name, email, and password are required.' });
      return;
    }

    setActionLoading(true);
    setFormErrors({});
    try {
      await adminService.createManager({
        name: managerForm.name.trim(),
        email: managerForm.email.trim(),
        contactNumber: managerForm.contactNumber.trim(),
        password: managerForm.password,
        teamId: managerForm.teamId ? Number(managerForm.teamId) : null,
      });

      setSuccessMessage(`Manager account for ${managerForm.name} created successfully.`);
      setShowManagerModal(false);
      setManagerForm({ name: '', email: '', contactNumber: '', password: '', teamId: '' });
      fetchData();
    } catch (err) {
      setFormErrors({ general: extractErrorMessage(err).message });
    } finally {
      setActionLoading(false);
    }
  };

  
  const handleCreateTeam = async (e) => {
    e.preventDefault();
    if (!teamForm.name.trim()) {
      setFormErrors({ general: 'Team name is required.' });
      return;
    }

    setActionLoading(true);
    setFormErrors({});
    try {
      await teamService.createTeam({
        name: teamForm.name.trim(),
        description: teamForm.description.trim(),
        managerId: teamForm.managerId ? Number(teamForm.managerId) : null,
      });

      setSuccessMessage(`Team "${teamForm.name}" created successfully.`);
      setShowTeamModal(false);
      setTeamForm({ name: '', description: '', managerId: '' });
      fetchData();
    } catch (err) {
      setFormErrors({ general: extractErrorMessage(err).message });
    } finally {
      setActionLoading(false);
    }
  };


  const handleCreateUser = async (e) => {
    e.preventDefault();
    if (!userForm.name.trim() || !userForm.email.trim() || !userForm.password) {
      setFormErrors({ general: 'Name, email, and password are required.' });
      return;
    }

    setActionLoading(true);
    setFormErrors({});
    try {
      await adminService.createUser({
        name: userForm.name.trim(),
        email: userForm.email.trim(),
        contactNumber: userForm.contactNumber.trim(),
        password: userForm.password,
        role: userForm.role,
        teamId: userForm.teamId ? Number(userForm.teamId) : null,
      });

      setSuccessMessage(`User account for ${userForm.name} created successfully.`);
      setShowUserModal(false);
      setUserForm({ name: '', email: '', contactNumber: '', password: '', role: 'EMPLOYEE', teamId: '' });
      fetchData();
    } catch (err) {
      setFormErrors({ general: extractErrorMessage(err).message });
    } finally {
      setActionLoading(false);
    }
  };

  const columns = [
    {
      header: 'User',
      key: 'name',
      render: (u) => (
        <div>
          <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{u.name}</div>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>ID: #{u.id || '—'}</div>
        </div>
      ),
    },
    {
      header: 'Email & Contact',
      key: 'email',
      render: (u) => (
        <div>
          <div>{u.email}</div>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>{u.contactNumber || '—'}</div>
        </div>
      ),
    },
    {
      header: 'Role',
      key: 'role',
      render: (u) => (
        <Badge variant={u.role === 'SUPER_ADMIN' ? 'danger' : u.role === 'MANAGER' ? 'info' : 'neutral'}>
          {formatRole(u.role)}
        </Badge>
      ),
    },
    {
      header: 'Team',
      key: 'teamName',
      render: (u) => u.teamName || 'Unassigned',
    },
    {
      header: 'Status',
      key: 'active',
      render: (u) => (
        <Badge variant={u.active ? 'success' : 'danger'}>
          {u.active ? 'Active' : 'Disabled'}
        </Badge>
      ),
    },
    {
      header: 'Actions',
      key: 'actions',
      align: 'right',
      render: (u) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => handleToggleStatus(u)}
          title={u.active ? 'Deactivate account' : 'Activate account'}
        >
          {u.active ? (
            <span style={{ color: 'var(--color-crimson)', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <ToggleRight size={16} /> Disable
            </span>
          ) : (
            <span style={{ color: 'var(--color-success)', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <ToggleLeft size={16} /> Enable
            </span>
          )}
        </Button>
      ),
    },
  ];

  return (
    <div>
      {}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="heading-page">Super Admin Control Console</h1>
          <p className="subheading" style={{ marginBottom: 0 }}>
            Manage organization hierarchy, managers, teams, and employee credentials.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <Button
            variant="secondary"
            icon={FolderPlus}
            onClick={() => {
              setFormErrors({});
              setShowTeamModal(true);
            }}
          >
            Create Team
          </Button>
          <Button
            variant="secondary"
            icon={UserPlus}
            onClick={() => {
              setFormErrors({});
              setShowManagerModal(true);
            }}
          >
            Add Manager
          </Button>
          <Button
            variant="primary"
            icon={UserPlus}
            onClick={() => {
              setFormErrors({});
              setShowUserModal(true);
            }}
          >
            Add User
          </Button>
        </div>
      </div>

      {apiError && (
        <Alert
          variant="danger"
          title="Operation Failed"
          message={apiError.message}
          statusCode={apiError.status}
          onClose={() => setApiError(null)}
        />
      )}

      {successMessage && (
        <Alert
          variant="success"
          message={successMessage}
          onClose={() => setSuccessMessage(null)}
        />
      )}

      {}
      <div className="grid-4" style={{ marginBottom: '28px' }}>
        <StatCard
          label="Total Users"
          value={users.length}
          helper="Registered in organization"
          icon={Users}
        />
        <StatCard
          label="Managers"
          value={managers.length}
          helper="Leading operational teams"
          icon={Briefcase}
        />
        <StatCard
          label="Active Teams"
          value={teams.length}
          helper="Engineering, Sales, etc."
          icon={Users}
        />
        <StatCard
          label="Active Accounts"
          value={users.filter((u) => u.active).length}
          helper="Permitted to sign in"
          icon={CheckCircle2}
        />
      </div>

      {}
      <div style={{ marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <h2 className="heading-section">Teams Overview</h2>
          <span style={{ fontSize: '13px', color: 'var(--color-text-secondary)' }}>
            {teams.length} configured teams
          </span>
        </div>

        <div className="grid-3">
          {teams.length === 0 && !loading && (
            <div style={{ gridColumn: '1 / -1', padding: '24px', textAlign: 'center', color: 'var(--color-text-muted)', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-lg)' }}>
              No teams configured yet. Click "Create Team" to set up your organizational units.
            </div>
          )}

          {teams.map((t) => (
            <div key={t.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                <h3 style={{ fontSize: '15px', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                  {t.name}
                </h3>
                <Badge variant="info">{t.memberCount ?? 0} members</Badge>
              </div>
              <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', marginBottom: '16px', minHeight: '38px' }}>
                {t.description || 'No description provided.'}
              </p>
              <div style={{ borderTop: '1px solid var(--color-border-subtle)', paddingTop: '10px', fontSize: '12px', display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Manager:</span>
                <span style={{ fontWeight: 500, color: 'var(--color-text-primary)' }}>
                  {t.managerName || 'Unassigned'}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px', flexWrap: 'wrap', gap: '12px' }}>
          <h2 className="heading-section">Managers List </h2>

          {}
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            

          </div>
        </div>

        <Table
          columns={columns}
          data={filteredUsers}
          loading={loading}
          emptyMessage="No users found matching your filters."
        />
      </div>

      {}
      <Modal
        isOpen={showManagerModal}
        onClose={() => setShowManagerModal(false)}
        title="Create Manager Account"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowManagerModal(false)} disabled={actionLoading}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleCreateManager} loading={actionLoading}>
              Create Manager
            </Button>
          </>
        }
      >
        {formErrors.general && (
          <Alert variant="danger" message={formErrors.general} />
        )}
        <form onSubmit={handleCreateManager}>
          <Input
            label="Full Name"
            placeholder="John Smith"
            value={managerForm.name}
            onChange={(e) => setManagerForm({ ...managerForm, name: e.target.value })}
            required
            autoFocus
          />
          <Input
            label="Corporate Email"
            type="email"
            placeholder="john.manager@finflow.com"
            value={managerForm.email}
            onChange={(e) => setManagerForm({ ...managerForm, email: e.target.value })}
            required
          />
          <Input
            label="Contact Number"
            type="tel"
            placeholder="+91 9876543210"
            value={managerForm.contactNumber}
            onChange={(e) => setManagerForm({ ...managerForm, contactNumber: e.target.value })}
          />
          <Input
            label="Temporary Password"
            type="password"
            placeholder="••••••••••••"
            value={managerForm.password}
            onChange={(e) => setManagerForm({ ...managerForm, password: e.target.value })}
            required
          />
          <Select
            label="Assign to Team (Optional)"
            placeholder="Select a team..."
            value={managerForm.teamId}
            onChange={(e) => setManagerForm({ ...managerForm, teamId: e.target.value })}
            options={teams.map((t) => ({ value: t.id, label: t.name }))}
          />
        </form>
      </Modal>

      {}
      <Modal
        isOpen={showTeamModal}
        onClose={() => setShowTeamModal(false)}
        title="Create Organizational Team"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowTeamModal(false)} disabled={actionLoading}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleCreateTeam} loading={actionLoading}>
              Create Team
            </Button>
          </>
        }
      >
        {formErrors.general && (
          <Alert variant="danger" message={formErrors.general} />
        )}
        <form onSubmit={handleCreateTeam}>
          <Input
            label="Team Name"
            placeholder="Engineering Team"
            value={teamForm.name}
            onChange={(e) => setTeamForm({ ...teamForm, name: e.target.value })}
            required
            autoFocus
          />
          <Input
            label="Description"
            placeholder="Frontend & Backend software engineers"
            value={teamForm.description}
            onChange={(e) => setTeamForm({ ...teamForm, description: e.target.value })}
          />
          <Select
            label="Assign Team Manager (Optional)"
            placeholder="Select a manager..."
            value={teamForm.managerId}
            onChange={(e) => setTeamForm({ ...teamForm, managerId: e.target.value })}
            options={users
              .filter((u) => u.role === 'MANAGER')
              .map((m) => ({ value: m.id, label: `${m.name} (${m.email})` }))}
          />
        </form>
      </Modal>

      {}
      <Modal
        isOpen={showUserModal}
        onClose={() => setShowUserModal(false)}
        title="Add User Account"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowUserModal(false)} disabled={actionLoading}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleCreateUser} loading={actionLoading}>
              Add User
            </Button>
          </>
        }
      >
        {formErrors.general && (
          <Alert variant="danger" message={formErrors.general} />
        )}
        <form onSubmit={handleCreateUser}>
          <Input
            label="Full Name"
            placeholder="Sarah Jenkins"
            value={userForm.name}
            onChange={(e) => setUserForm({ ...userForm, name: e.target.value })}
            required
            autoFocus
          />
          <Input
            label="Work Email"
            type="email"
            placeholder="sarah@finflow.com"
            value={userForm.email}
            onChange={(e) => setUserForm({ ...userForm, email: e.target.value })}
            required
          />
          <Input
            label="Contact Number"
            type="tel"
            placeholder="+91 9123456780"
            value={userForm.contactNumber}
            onChange={(e) => setUserForm({ ...userForm, contactNumber: e.target.value })}
          />
          <Select
            label="Account Role"
            value={userForm.role}
            onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}
            options={[
              { value: 'EMPLOYEE', label: 'Employee' },
              { value: 'FINANCE', label: 'Finance Head' },
              { value: 'MANAGER', label: 'Manager' },
            ]}
            required
          />
          <Select
            label="Assign to Team (Optional)"
            placeholder="Select a team..."
            value={userForm.teamId}
            onChange={(e) => setUserForm({ ...userForm, teamId: e.target.value })}
            options={teams.map((t) => ({ value: t.id, label: t.name }))}
          />
          <Input
            label="Initial Password"
            type="password"
            placeholder="••••••••••••"
            value={userForm.password}
            onChange={(e) => setUserForm({ ...userForm, password: e.target.value })}
            required
          />
        </form>
      </Modal>
    </div>
  );
};

export default AdminDashboard;
