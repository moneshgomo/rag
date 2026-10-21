import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import teamService from '../../services/teamService';
import leaveService from '../../services/leaveService';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import Alert from '../../components/common/Alert';
import { StatCard } from '../../components/common/Card';
import {
  Users,
  UserPlus,
  RefreshCw,
  Clock,
  CheckCircle,
  XCircle,
  FileText,
  AlertCircle,
  Send,
  Search,
  UserCheck,
  Plus,
} from 'lucide-react';
import { formatDate, formatLeaveType, formatRole, getStatusBadgeVariant } from '../../utils/formatters';
import { extractErrorMessage } from '../../utils/errorHandler';

export const ManagerDashboard = () => {
  const { user } = useAuth();

  const [team, setTeam] = useState(null);
  const [members, setMembers] = useState([]);
  const [pendingLeaves, setPendingLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Add Member Modal & Unassigned Employees
  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [unassignedEmployees, setUnassignedEmployees] = useState([]);
  const [loadingUnassigned, setLoadingUnassigned] = useState(false);
  const [emailSearchFilter, setEmailSearchFilter] = useState('');
  const [addMemberTab, setAddMemberTab] = useState('unassigned'); // 'unassigned' | 'new'

  const [memberForm, setMemberForm] = useState({
    name: '',
    email: '',
    contactNumber: '',
    password: '',
  });

  // Action Modal (Approve / Reject)
  const [selectedLeave, setSelectedLeave] = useState(null);
  const [actionType, setActionType] = useState(null); // 'APPROVE' | 'REJECT'
  const [actionComment, setActionComment] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState(null);

  const fetchManagerData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const managerTeamId = user?.teamId || user?.team?.id || user?.team_id;
      let effectiveTeamId = managerTeamId;
      let teamData = null;

      // If teamId not directly in user profile, try finding manager's team in listTeams
      if (!effectiveTeamId) {
        try {
          const allTeams = await teamService.listTeams();
          if (Array.isArray(allTeams)) {
            const found = allTeams.find(
              (t) => t.managerId === user?.id || t.managerEmail === user?.email || t.id === user?.teamId
            );
            if (found) {
              teamData = found;
              effectiveTeamId = found.id;
            }
          }
        } catch (e) {
          console.warn('Could not fetch teams list:', e);
        }
      }

      // Fetch team details if we have teamId
      if (effectiveTeamId) {
        try {
          const res = await teamService.getTeam(effectiveTeamId);
          if (res && !Array.isArray(res)) {
            teamData = res;
          } else if (Array.isArray(res)) {
            teamData = res.find((t) => t.id === effectiveTeamId) || res[0];
          }
        } catch (e) {
          console.warn(`Could not get team #${effectiveTeamId} details:`, e);
        }
      }

      // Fetch team members via GET /api/teams/{teamId}/members (with bearer token via api interceptor)
      let membersData = [];
      if (effectiveTeamId) {
        try {
          membersData = await teamService.listTeamMembers(effectiveTeamId);
        } catch (memErr) {
          console.error(`Could not fetch team members for team #${effectiveTeamId}:`, memErr);
          throw memErr;
        }
      }

      const teamName =
        teamData?.name ||
        membersData?.[0]?.teamName ||
        user?.teamName ||
        (effectiveTeamId ? `Team #${effectiveTeamId}` : 'My Team');

      setTeam(
        teamData
          ? { ...teamData, name: teamName, id: effectiveTeamId }
          : effectiveTeamId
          ? { id: effectiveTeamId, name: teamName }
          : null
      );
      setMembers(Array.isArray(membersData) ? membersData : []);

      // Fetch pending leaves
      try {
        const pending = await leaveService.getPendingLeaves();
        setPendingLeaves(pending || []);
      } catch (e) {
        console.warn('Could not fetch pending leaves:', e);
      }
    } catch (err) {
      setApiError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchManagerData();
  }, [user]);

  // Fetch unassigned employees with optional email filter
  const fetchUnassignedEmployees = async (emailFilter = '') => {
    setLoadingUnassigned(true);
    try {
      const data = await teamService.getUnassignedEmployees(emailFilter);
      setUnassignedEmployees(Array.isArray(data) ? data : []);
    } catch (err) {
      console.warn('Failed to fetch unassigned employees:', err);
    } finally {
      setLoadingUnassigned(false);
    }
  };

  const handleOpenAddMemberModal = () => {
    setActionError(null);
    setEmailSearchFilter('');
    setAddMemberTab('unassigned');
    setShowAddMemberModal(true);
    fetchUnassignedEmployees('');
  };

  const handleAssignExistingEmployee = async (employee) => {
    if (!team) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await teamService.addMemberToTeam(team.id, {
        userId: employee.id,
      });

      setSuccessMessage(`Employee ${employee.name} (${employee.email}) assigned to ${team.name} successfully.`);
      setShowAddMemberModal(false);
      fetchManagerData();
    } catch (err) {
      setActionError(extractErrorMessage(err).message);
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Add Member to Team (New Registration)
  const handleAddMember = async (e) => {
    e.preventDefault();
    if (!team) return;

    if (!memberForm.name.trim() || !memberForm.email.trim() || !memberForm.password) {
      setActionError('Name, email, and password are required.');
      return;
    }

    setActionLoading(true);
    setActionError(null);
    try {
      await teamService.addMemberToTeam(team.id, {
        name: memberForm.name.trim(),
        email: memberForm.email.trim(),
        contactNumber: memberForm.contactNumber.trim(),
        password: memberForm.password,
      });

      setSuccessMessage(`Employee ${memberForm.name} assigned to ${team.name} successfully.`);
      setShowAddMemberModal(false);
      setMemberForm({ name: '', email: '', contactNumber: '', password: '' });
      fetchManagerData();
    } catch (err) {
      setActionError(extractErrorMessage(err).message);
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Leave Approval / Rejection
  const handleLeaveDecision = async () => {
    if (!selectedLeave || !actionType) return;

    if (actionType === 'REJECT' && !actionComment.trim()) {
      setActionError('A rejection reason is mandatory to explain the decision.');
      return;
    }

    setActionLoading(true);
    setActionError(null);
    try {
      if (actionType === 'APPROVE') {
        await leaveService.approveLeave(selectedLeave.id, actionComment.trim());
        setSuccessMessage(`Leave request #${selectedLeave.id} approved successfully.`);
      } else {
        await leaveService.rejectLeave(selectedLeave.id, actionComment.trim());
        setSuccessMessage(`Leave request #${selectedLeave.id} rejected.`);
      }

      setSelectedLeave(null);
      setActionType(null);
      setActionComment('');
      fetchManagerData();
    } catch (err) {
      setActionError(extractErrorMessage(err).message);
    } finally {
      setActionLoading(false);
    }
  };

  const memberColumns = [
    {
      header: 'Employee',
      key: 'name',
      render: (m) => (
        <div>
          <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{m.name}</div>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>ID: #{m.id}</div>
        </div>
      ),
    },
    {
      header: 'Email',
      key: 'email',
    },
    {
      header: 'Contact Number',
      key: 'contactNumber',
      render: (m) => m.contactNumber || '—',
    },
    {
      header: 'Role',
      key: 'role',
      render: (m) => (
        <Badge variant={m.role === 'MANAGER' ? 'info' : 'neutral'}>
          {formatRole(m.role)}
        </Badge>
      ),
    },
    {
      header: 'Joined Date',
      key: 'createdAt',
      render: (m) => (m.createdAt ? formatDate(m.createdAt) : '—'),
    },
    {
      header: 'Status',
      key: 'active',
      render: (m) => (
        <Badge variant={m.active ? 'success' : 'danger'}>
          {m.active ? 'Active' : 'Disabled'}
        </Badge>
      ),
    },
  ];

  const pendingColumns = [
    {
      header: 'Employee',
      key: 'employeeName',
      render: (l) => (
        <div>
          <div style={{ fontWeight: 600 }}>{l.employeeName}</div>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>{l.employeeEmail}</div>
        </div>
      ),
    },
    {
      header: 'Category',
      key: 'leaveType',
      render: (l) => <Badge variant="neutral">{formatLeaveType(l.leaveType)}</Badge>,
    },
    {
      header: 'Dates & Duration',
      key: 'dates',
      render: (l) => (
        <div>
          <div>{formatDate(l.startDate)} &rarr; {formatDate(l.endDate)}</div>
          <div style={{ fontSize: '11px', color: 'var(--color-primary)', fontWeight: 500 }}>
            {l.daysCount} calendar day{l.daysCount > 1 ? 's' : ''}
          </div>
        </div>
      ),
    },
    {
      header: 'Reason & RAG Advisory',
      key: 'reason',
      render: (l) => (
        <div style={{ maxWidth: '300px' }}>
          <div style={{ fontSize: '13px', color: 'var(--color-text-primary)', marginBottom: '4px' }}>
            {l.reason}
          </div>
          {l.ragDecision && (
            <div style={{ fontSize: '11px', color: '#0369A1', background: '#F0F9FF', padding: '4px 6px', borderRadius: '4px', border: '1px solid #BAE6FD' }}>
              <strong>AI Check:</strong> {l.ragDecision}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Actions',
      key: 'actions',
      align: 'right',
      render: (l) => (
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <Button
            size="sm"
            variant="primary"
            onClick={() => {
              setSelectedLeave(l);
              setActionType('APPROVE');
              setActionComment('');
              setActionError(null);
            }}
          >
            Approve
          </Button>
          <Button
            size="sm"
            variant="crimson-outline"
            onClick={() => {
              setSelectedLeave(l);
              setActionType('REJECT');
              setActionComment('');
              setActionError(null);
            }}
          >
            Reject
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="heading-page">Manager Hub</h1>
          <p className="subheading" style={{ marginBottom: 0 }}>
            Oversee {team?.name ? `"${team.name}"` : 'your assigned team'}, manage team members, and review leave requests.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <Button
            variant="secondary"
            icon={RefreshCw}
            onClick={fetchManagerData}
            loading={loading}
          >
            Refresh
          </Button>
          {team && (
            <Button
              variant="primary"
              icon={UserPlus}
              onClick={handleOpenAddMemberModal}
            >
              Add Team Member
            </Button>
          )}
        </div>
      </div>

      {apiError && (
        <Alert
          variant="danger"
          title="Error"
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

      {/* Overview Stat Cards */}
      <div className="grid-3" style={{ marginBottom: '28px' }}>
        <StatCard
          label="Team Members"
          value={members.length}
          helper={team?.name || 'Assigned team'}
          icon={Users}
        />
        <StatCard
          label="Pending Leave Requests"
          value={pendingLeaves.length}
          helper="Awaiting review"
          icon={Clock}
        />
        <StatCard
          label="Active Accounts"
          value={members.filter((m) => m.active).length}
          helper="Team members active"
          icon={CheckCircle}
        />
      </div>

      {/* Pending Leave Requests Section */}
      <div style={{ marginBottom: '36px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <h2 className="heading-section">Pending Team Leave Requests</h2>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', margin: 0 }}>
              Requests submitted by employees belonging to your management reporting line.
            </p>
          </div>
          <Badge variant={pendingLeaves.length > 0 ? 'warning' : 'neutral'}>
            {pendingLeaves.length} pending
          </Badge>
        </div>

        <Table
          columns={pendingColumns}
          data={pendingLeaves}
          loading={loading}
          emptyMessage="No pending leave requests awaiting approval."
        />
      </div>

      {/* Team Members List */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <h2 className="heading-section">Team Members ({team?.name || 'My Team'})</h2>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', margin: 0 }}>
              Read-only list of verified members assigned to this team unit.
            </p>
          </div>
        </div>

        <Table
          columns={memberColumns}
          data={members}
          loading={loading}
          emptyMessage="No members assigned to this team yet. Use 'Add Team Member' to register developers or associates."
        />
      </div>

      {/* Modal: Add Team Member */}
      <Modal
        isOpen={showAddMemberModal}
        onClose={() => setShowAddMemberModal(false)}
        title={`Add Member to ${team?.name || 'Team'}`}
        footer={
          addMemberTab === 'new' ? (
            <>
              <Button variant="secondary" onClick={() => setShowAddMemberModal(false)} disabled={actionLoading}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleAddMember} loading={actionLoading}>
                Register &amp; Send Invite
              </Button>
            </>
          ) : (
            <Button variant="secondary" onClick={() => setShowAddMemberModal(false)} disabled={actionLoading}>
              Close
            </Button>
          )
        }
      >
        {actionError && <Alert variant="danger" message={actionError} />}

        {/* Tab Switcher: Existing Unassigned Employees vs Register New */}
        <div style={{ display: 'flex', backgroundColor: '#f1f5f9', borderRadius: '8px', padding: '4px', marginBottom: '18px' }}>
          <button
            type="button"
            onClick={() => setAddMemberTab('unassigned')}
            style={{
              flex: 1,
              padding: '8px 12px',
              fontSize: '13px',
              fontWeight: 600,
              borderRadius: '6px',
              border: 'none',
              cursor: 'pointer',
              backgroundColor: addMemberTab === 'unassigned' ? '#ffffff' : 'transparent',
              color: addMemberTab === 'unassigned' ? '#0f172a' : '#64748b',
              boxShadow: addMemberTab === 'unassigned' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            Existing Employees ({unassignedEmployees.length})
          </button>
          <button
            type="button"
            onClick={() => setAddMemberTab('new')}
            style={{
              flex: 1,
              padding: '8px 12px',
              fontSize: '13px',
              fontWeight: 600,
              borderRadius: '6px',
              border: 'none',
              cursor: 'pointer',
              backgroundColor: addMemberTab === 'new' ? '#ffffff' : 'transparent',
              color: addMemberTab === 'new' ? '#0f172a' : '#64748b',
              boxShadow: addMemberTab === 'new' ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            + Register New Employee
          </button>
        </div>

        {addMemberTab === 'unassigned' ? (
          <div>
            {/* Search filter input */}
            <div style={{ marginBottom: '16px' }}>
              <Input
                placeholder="Search unassigned employee by email..."
                value={emailSearchFilter}
                onChange={(e) => setEmailSearchFilter(e.target.value)}
                autoFocus
              />
            </div>

            {loadingUnassigned ? (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#64748b', fontSize: '13px' }}>
                Fetching unassigned employees...
              </div>
            ) : (
              <div>
                {(() => {
                  const filtered = unassignedEmployees.filter(
                    (emp) =>
                      !emailSearchFilter.trim() ||
                      emp.email?.toLowerCase().includes(emailSearchFilter.trim().toLowerCase()) ||
                      emp.name?.toLowerCase().includes(emailSearchFilter.trim().toLowerCase())
                  );

                  if (filtered.length === 0) {
                    return (
                      <div
                        style={{
                          textAlign: 'center',
                          padding: '24px 16px',
                          backgroundColor: '#f8fafc',
                          borderRadius: '8px',
                          border: '1px solid #e2e8f0',
                        }}
                      >
                        <Users size={32} style={{ color: '#94a3b8', margin: '0 auto 8px' }} />
                        <div style={{ fontSize: '14px', fontWeight: 600, color: '#334155', marginBottom: '4px' }}>
                          No unassigned employees found
                        </div>
                        <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '14px' }}>
                          {emailSearchFilter
                            ? `No unassigned employee matches "${emailSearchFilter}".`
                            : 'All existing employees are already assigned to teams.'}
                        </div>
                        <Button size="sm" variant="secondary" onClick={() => setAddMemberTab('new')}>
                          Register New Employee Instead
                        </Button>
                      </div>
                    );
                  }

                  return (
                    <div
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '8px',
                        maxHeight: '290px',
                        overflowY: 'auto',
                        paddingRight: '4px',
                      }}
                    >
                      {filtered.map((emp) => (
                        <div
                          key={emp.id}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '12px 14px',
                            backgroundColor: '#ffffff',
                            borderRadius: '8px',
                            border: '1px solid #e2e8f0',
                          }}
                        >
                          <div>
                            <div style={{ fontWeight: 600, fontSize: '14px', color: '#0f172a' }}>
                              {emp.name}
                            </div>
                            <div style={{ fontSize: '12px', color: 'var(--color-primary)', fontWeight: 500 }}>
                              {emp.email}
                            </div>
                            {emp.contactNumber && (
                              <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '2px' }}>
                                {emp.contactNumber}
                              </div>
                            )}
                          </div>
                          <Button
                            size="sm"
                            variant="primary"
                            disabled={actionLoading}
                            onClick={() => handleAssignExistingEmployee(emp)}
                          >
                            Add to Team
                          </Button>
                        </div>
                      ))}
                    </div>
                  );
                })()}
              </div>
            )}
          </div>
        ) : (
          <form onSubmit={handleAddMember}>
            <Input
              label="Full Name"
              placeholder="Aravind Dev"
              value={memberForm.name}
              onChange={(e) => setMemberForm({ ...memberForm, name: e.target.value })}
              required
              autoFocus
            />
            <Input
              label="Work Email"
              type="email"
              placeholder="aravind@finflow.com"
              value={memberForm.email}
              onChange={(e) => setMemberForm({ ...memberForm, email: e.target.value })}
              required
            />
            <Input
              label="Contact Number"
              type="tel"
              placeholder="+91 9123456789"
              value={memberForm.contactNumber}
              onChange={(e) => setMemberForm({ ...memberForm, contactNumber: e.target.value })}
            />
            <Input
              label="Temporary Password"
              type="password"
              placeholder="••••••••••••"
              value={memberForm.password}
              onChange={(e) => setMemberForm({ ...memberForm, password: e.target.value })}
              helper="The employee will receive an onboarding email to replace this with a permanent password"
              required
            />
          </form>
        )}
      </Modal>

      {/* Modal: Approve or Reject Leave */}
      <Modal
        isOpen={!!selectedLeave}
        onClose={() => {
          setSelectedLeave(null);
          setActionType(null);
          setActionError(null);
        }}
        title={actionType === 'APPROVE' ? 'Confirm Leave Approval' : 'Confirm Leave Rejection'}
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                setSelectedLeave(null);
                setActionType(null);
              }}
              disabled={actionLoading}
            >
              Cancel
            </Button>
            <Button
              variant={actionType === 'APPROVE' ? 'primary' : 'crimson'}
              onClick={handleLeaveDecision}
              loading={actionLoading}
            >
              {actionType === 'APPROVE' ? 'Confirm Approval' : 'Confirm Rejection'}
            </Button>
          </>
        }
      >
        {actionError && <Alert variant="danger" message={actionError} />}

        {selectedLeave && (
          <div>
            <div
              style={{
                backgroundColor: '#F9FAFB',
                padding: '12px 16px',
                borderRadius: 'var(--radius-md)',
                marginBottom: '16px',
                fontSize: '13px',
                border: '1px solid var(--color-border-subtle)',
              }}
            >
              <div style={{ marginBottom: '4px' }}>
                <strong>Employee:</strong> {selectedLeave.employeeName} ({selectedLeave.employeeEmail})
              </div>
              <div style={{ marginBottom: '4px' }}>
                <strong>Leave Category:</strong> {formatLeaveType(selectedLeave.leaveType)}
              </div>
              <div style={{ marginBottom: '4px' }}>
                <strong>Dates:</strong> {formatDate(selectedLeave.startDate)} to {formatDate(selectedLeave.endDate)} ({selectedLeave.daysCount} days)
              </div>
              <div>
                <strong>Reason:</strong> {selectedLeave.reason}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">
                Manager Comment {actionType === 'REJECT' && <span className="required">* (Mandatory for rejection)</span>}
              </label>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder={
                  actionType === 'APPROVE'
                    ? 'Optional notes (e.g., Please ensure handover of critical tasks)...'
                    : 'Explain reason for rejection (e.g., Critical sprint release scheduled)...'
                }
                value={actionComment}
                onChange={(e) => setActionComment(e.target.value)}
                autoFocus
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ManagerDashboard;
