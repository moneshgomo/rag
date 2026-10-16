import React from 'react';
import { Calendar, CheckCircle2, Clock } from 'lucide-react';

export const LeaveBalanceWidget = ({ balance, loading = false }) => {
  if (loading) {
    return (
      <div className="grid-3" style={{ marginBottom: '24px' }}>
        <div className="stat-card" style={{ padding: '24px', textAlign: 'center' }}>
          <span className="spinner" style={{ margin: '0 auto', borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
        </div>
        <div className="stat-card" style={{ padding: '24px', textAlign: 'center' }}>
          <span className="spinner" style={{ margin: '0 auto', borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
        </div>
        <div className="stat-card" style={{ padding: '24px', textAlign: 'center' }}>
          <span className="spinner" style={{ margin: '0 auto', borderColor: 'var(--color-primary)', borderTopColor: 'transparent' }} />
        </div>
      </div>
    );
  }

  const remaining = balance?.remainingDays ?? 20;
  const used = balance?.usedDays ?? 0;
  const total = balance?.totalDays ?? 20;
  const year = balance?.year ?? new Date().getFullYear();

  return (
    <div className="grid-3" style={{ marginBottom: '24px' }}>
      <div className="stat-card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span className="stat-label">Available Balance</span>
          <Calendar size={18} color="var(--color-primary)" />
        </div>
        <div className="stat-value" style={{ color: 'var(--color-primary)' }}>
          {remaining} <span style={{ fontSize: '14px', fontWeight: 500, color: 'var(--color-text-muted)' }}>days</span>
        </div>
        <span className="stat-helper">Quota for Year {year}</span>
      </div>

      <div className="stat-card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span className="stat-label">Consumed Leaves</span>
          <CheckCircle2 size={18} color="var(--color-success)" />
        </div>
        <div className="stat-value">
          {used} <span style={{ fontSize: '14px', fontWeight: 500, color: 'var(--color-text-muted)' }}>days</span>
        </div>
        <span className="stat-helper">Approved so far</span>
      </div>

      <div className="stat-card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span className="stat-label">Total Annual Allowance</span>
          <Clock size={18} color="var(--color-text-muted)" />
        </div>
        <div className="stat-value">
          {total} <span style={{ fontSize: '14px', fontWeight: 500, color: 'var(--color-text-muted)' }}>days</span>
        </div>
        <span className="stat-helper">Standard organizational allocation</span>
      </div>
    </div>
  );
};

export default LeaveBalanceWidget;
