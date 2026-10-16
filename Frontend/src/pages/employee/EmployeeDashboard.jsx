import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import leaveService from '../../services/leaveService';
import teamService from '../../services/teamService';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Alert from '../../components/common/Alert';
import Modal from '../../components/common/Modal';
import LeaveBalanceWidget from '../../components/leave/LeaveBalanceWidget';
import {
  FilePlus,
  Users,
  Calendar,
  Briefcase,
  Sparkles,
  Info,
  Clock,
} from 'lucide-react';
import { formatDate, formatDateTime, formatLeaveType, getStatusBadgeVariant } from '../../utils/formatters';
import { extractErrorMessage } from '../../utils/errorHandler';

export const EmployeeDashboard = () => {
  const { user } = useAuth();

  const [leaves, setLeaves] = useState([]);
  const [balance, setBalance] = useState(null);
  const [teamMembers, setTeamMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);

  const [selectedLeave, setSelectedLeave] = useState(null);

  useEffect(() => {
    const fetchEmployeeData = async () => {
      setLoading(true);
      setApiError(null);
      try {
        const [leavesData, balanceData] = await Promise.all([
          leaveService.getMyLeaves().catch(() => []),
          leaveService.getMyBalance().catch(() => null),
        ]);

        setLeaves(leavesData || []);
        setBalance(balanceData);

        if (user?.teamId) {
          try {
            const members = await teamService.listTeamMembers(user.teamId);
            setTeamMembers(members || []);
          } catch (teamErr) {
            console.warn('Could not fetch team members (check permissions):', teamErr);
          }
        }
      } catch (err) {
        setApiError(extractErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };

    fetchEmployeeData();
  }, [user]);

  const getTimeGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  };

  const columns = [
    {
      header: 'Category',
      key: 'leaveType',
      render: (l) => <Badge variant="neutral">{formatLeaveType(l.leaveType)}</Badge>,
    },
    {
      header: 'Dates',
      key: 'dates',
      render: (l) => (
        <div>
          <div>{formatDate(l.startDate)} &rarr; {formatDate(l.endDate)}</div>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
            {l.daysCount} calendar day{l.daysCount > 1 ? 's' : ''}
          </div>
        </div>
      ),
    },
    {
      header: 'Reason',
      key: 'reason',
      render: (l) => (
        <span style={{ maxWidth: '240px', display: 'inline-block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {l.reason}
        </span>
      ),
    },
    {
      header: 'Status',
      key: 'status',
      render: (l) => (
        <Badge variant={getStatusBadgeVariant(l.status)}>
          {l.status}
        </Badge>
      ),
    },
    {
      header: 'Submitted',
      key: 'createdAt',
      render: (l) => formatDate(l.createdAt),
    },
    {
      header: 'Actions',
      key: 'actions',
      align: 'right',
      render: (l) => (
        <Button
          size="sm"
          variant="secondary"
          onClick={() => setSelectedLeave(l)}
        >
          View Details
        </Button>
      ),
    },
  ];

  return (
    <div>
      {}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '28px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="heading-page">
            {getTimeGreeting()}, {user?.name || 'Colleague'}
          </h1>
          <p className="subheading" style={{ marginBottom: 0 }}>
            Welcome to your FinFlow workspace. Track your quota, submit leave applications, and view team details.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <Link to="/leave/request">
            <Button variant="primary" icon={FilePlus}>
              Request Leave
            </Button>
          </Link>
        </div>
      </div>

      {apiError && (
        <Alert
          variant="danger"
          title="Error Loading Dashboard"
          message={apiError.message}
          statusCode={apiError.status}
          onClose={() => setApiError(null)}
        />
      )}

      {}
      <LeaveBalanceWidget balance={balance} loading={loading} />

      {}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '20px',
          marginBottom: '32px',
        }}
      >
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Briefcase size={18} color="var(--color-primary)" />
            <h3 className="card-title">My Department & Team</h3>
          </div>
          <div style={{ fontSize: '15px', fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: '4px' }}>
            {user?.teamName || 'Independent Contributor'}
          </div>
          <div style={{ fontSize: '13px', color: 'var(--color-text-secondary)' }}>
            Reporting Line: {user?.managerName ? `${user.managerName}` : 'Operations Management'}
          </div>
        </div>

        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Calendar size={18} color="var(--color-primary)" />
            <h3 className="card-title">Leave Policy Entitlement</h3>
          </div>
          <div style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>
            You are enrolled in the <strong>Standard 20-Day Annual Policy</strong>. Casual leaves allow up to 3 consecutive days.
          </div>
        </div>
      </div>

      {}
      <div style={{ marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <h2 className="heading-section">My Leave Requests</h2>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', margin: 0 }}>
              Recent personal leave submissions, approvals, and manager decisions.
            </p>
          </div>

          <Link to="/leave/request">
            <Button size="sm" variant="secondary" icon={FilePlus}>
              New Request
            </Button>
          </Link>
        </div>

        <Table
          columns={columns}
          data={leaves}
          loading={loading}
          emptyMessage="You have not submitted any leave requests yet."
        />
      </div>

      {}
      {teamMembers.length > 0 && (
        <div>
          <div style={{ marginBottom: '14px' }}>
            <h2 className="heading-section">Team Members ({user?.teamName})</h2>
            <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', margin: 0 }}>
              Read-only view of colleagues working in your operational unit.
            </p>
          </div>

          <Table
            columns={[
              { header: 'Colleague', key: 'name', render: (m) => <strong>{m.name}</strong> },
              { header: 'Email', key: 'email' },
              { header: 'Contact', key: 'contactNumber', render: (m) => m.contactNumber || '—' },
            ]}
            data={teamMembers}
            loading={loading}
            emptyMessage="No colleagues found in this team."
          />
        </div>
      )}

      {}
      <Modal
        isOpen={!!selectedLeave}
        onClose={() => setSelectedLeave(null)}
        title="Leave Request Details"
        footer={
          <Button variant="secondary" onClick={() => setSelectedLeave(null)}>
            Close
          </Button>
        }
      >
        {selectedLeave && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', fontSize: '13px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Status:</span>
              <Badge variant={getStatusBadgeVariant(selectedLeave.status)}>
                {selectedLeave.status}
              </Badge>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Leave Category:</span>
              <strong>{formatLeaveType(selectedLeave.leaveType)}</strong>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Requested Dates:</span>
              <span>{formatDate(selectedLeave.startDate)} to {formatDate(selectedLeave.endDate)}</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Duration:</span>
              <span>{selectedLeave.daysCount} calendar days</span>
            </div>

            <div>
              <span style={{ color: 'var(--color-text-muted)', display: 'block', marginBottom: '4px' }}>
                Reason for Leave:
              </span>
              <div
                style={{
                  backgroundColor: '#F9FAFB',
                  padding: '10px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border-subtle)',
                  color: 'var(--color-text-primary)',
                }}
              >
                {selectedLeave.reason}
              </div>
            </div>

            {selectedLeave.ragDecision && (
              <div
                style={{
                  backgroundColor: '#F0F9FF',
                  padding: '12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid #BAE6FD',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#0369A1', fontWeight: 600, marginBottom: '4px' }}>
                  <Sparkles size={14} /> AI Policy Advisory
                </div>
                <div style={{ color: '#0C4A6E', fontSize: '12px', lineHeight: 1.5 }}>
                  {selectedLeave.ragDecision}
                </div>
              </div>
            )}

            {selectedLeave.managerComment && (
              <div
                style={{
                  backgroundColor: '#FEFCE8',
                  padding: '12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid #FEF08A',
                }}
              >
                <div style={{ fontWeight: 600, color: '#854D0E', marginBottom: '4px' }}>
                  Manager Remarks:
                </div>
                <div style={{ color: '#713F12', fontSize: '12px' }}>
                  "{selectedLeave.managerComment}"
                </div>
              </div>
            )}

            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', borderTop: '1px solid var(--color-border-subtle)', paddingTop: '10px' }}>
              Submitted on {formatDateTime(selectedLeave.createdAt)}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default EmployeeDashboard;
