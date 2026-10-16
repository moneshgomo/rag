import React from 'react';
import { Sparkles, FileText } from 'lucide-react';

export const PolicyAdvisoryCard = ({ currentRAGAdvisory }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Current Request Advisory (if returned by backend or evaluated) */}
      {currentRAGAdvisory && (
        <div
          style={{
            border: '1px solid #BFDBFE',
            borderRadius: 'var(--radius-lg)',
            padding: '18px',
            backgroundColor: '#F8FAFC',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            <Sparkles size={16} color="var(--color-primary)" />
            <h4 style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-text-primary)' }}>
              AI Policy Advisory
            </h4>
            <span style={{ fontSize: '11px', background: '#E0F2FE', color: '#0369A1', padding: '1px 6px', borderRadius: '4px', marginLeft: 'auto' }}>
              Advisory Only
            </span>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
            {currentRAGAdvisory}
          </p>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '8px' }}>
            * Final approval remains the discretionary decision of your reporting manager.
          </div>
        </div>
      )}

      {/* Official Leave Policy Summary */}
      <div
        style={{
          backgroundColor: '#FFFFFF',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
          <FileText size={18} color="var(--color-primary)" />
          <h3 style={{ fontSize: '15px', fontWeight: 600, color: 'var(--color-text-primary)' }}>
            Company Leave Policy Highlights
          </h3>
        </div>

        <ul style={{ paddingLeft: '18px', fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.7, display: 'flex', flexDirection: 'column', gap: '6px' }}>
          <li>
            <strong>Working Days Only:</strong> Leave applies strictly to Monday through Friday. Saturdays and Sundays are regular weekly holidays and are not deducted from your balance.
          </li>
          <li>
            <strong>Casual Leave (Personal):</strong> 12 days per year. Max 3 consecutive working days allowed.
          </li>
          <li>
            <strong>Medical Leave:</strong> 10 days per year. Advance notice not required for unexpected illness.
          </li>
          <li>
            <strong>Official Duty (OD):</strong> For external client work, conferences, or company representation.
          </li>
          <li>
            <strong>Supporting Documents:</strong> Supporting docs (medical prescriptions, certificates, invitation letters) are <em>submitted physically</em> to HR after returning to office. Digital upload is optional.
          </li>
        </ul>
      </div>
    </div>
  );
};

export default PolicyAdvisoryCard;

