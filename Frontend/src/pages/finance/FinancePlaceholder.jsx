import React from 'react';
import { Coins, Sparkles, CreditCard, Receipt } from 'lucide-react';

export const FinancePlaceholder = () => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        textAlign: 'center',
        padding: '32px 16px',
      }}
    >
      <div
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '16px',
          backgroundColor: '#E8F3FF',
          color: 'var(--color-primary)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '20px',
        }}
      >
        <Coins size={28} />
      </div>

      <h1
        style={{
          fontSize: '24px',
          fontWeight: 700,
          color: 'var(--color-text-primary)',
          marginBottom: '8px',
          letterSpacing: '-0.02em',
        }}
      >
        Finance Module — Coming Soon
      </h1>

      <p
        style={{
          fontSize: '14px',
          color: 'var(--color-text-secondary)',
          maxWidth: '480px',
          lineHeight: 1.6,
          marginBottom: '32px',
        }}
      >
        Automated invoice processing, AI bill verification, employee reimbursement claims, and single-click Razorpay disbursements are currently in testing.
      </p>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '16px',
          maxWidth: '680px',
          width: '100%',
          textAlign: 'left',
        }}
      >
        <div className="card" style={{ padding: '16px' }}>
          <Receipt size={20} color="var(--color-primary)" style={{ marginBottom: '8px' }} />
          <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '4px' }}>PDF Bill Extraction</h4>
          <p style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            Instant optical bill parsing and amount validation.
          </p>
        </div>

        <div className="card" style={{ padding: '16px' }}>
          <Sparkles size={20} color="var(--color-primary)" style={{ marginBottom: '8px' }} />
          <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '4px' }}>AI Claim Audit</h4>
          <p style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            Verification against company reimbursement allowances.
          </p>
        </div>

        <div className="card" style={{ padding: '16px' }}>
          <CreditCard size={20} color="var(--color-primary)" style={{ marginBottom: '8px' }} />
          <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '4px' }}>Direct Disbursement</h4>
          <p style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            Single-click Razorpay bank transfers.
          </p>
        </div>
      </div>
    </div>
  );
};

export default FinancePlaceholder;
