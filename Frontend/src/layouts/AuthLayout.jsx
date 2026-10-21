import React from 'react';
import finflowLogo from '../assets/image.png';

export const AuthLayout = ({ children, title, subtitle, showHero = true }) => {
  if (!showHero) {
    return (
      <div
        style={{
          minHeight: '100vh',
          backgroundColor: '#FAFAFA',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '32px 16px',
        }}
      >
        <div style={{ width: '100%', maxWidth: '420px' }}>
          {/* Brand Header */}
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '14px' }}>
              <img
                src={finflowLogo}
                alt="FinFlow - Finance & HR Policy Assistant"
                style={{
                  height: '52px',
                  maxWidth: '240px',
                  objectFit: 'contain',
                }}
              />
            </div>
            <h1
              style={{
                fontSize: '24px',
                fontWeight: 700,
                color: 'var(--color-text-primary)',
                letterSpacing: '-0.02em',
                marginBottom: '6px',
              }}
            >
              {title}
            </h1>
            {subtitle && (
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', margin: 0 }}>
                {subtitle}
              </p>
            )}
          </div>

          {/* Auth Box */}
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--color-border)',
              padding: '28px',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            {children}
          </div>

          {/* Minimal Footer */}
          <div
            style={{
              textAlign: 'center',
              marginTop: '24px',
              fontSize: '12px',
              color: 'var(--color-text-muted)',
            }}
          >
            FinFlow &copy; {new Date().getFullYear()} — Finance &amp; HR Policy Assistant
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-split-wrapper">
      {/* Left Corporate IT Enterprise Hero Pane */}
      <div className="auth-hero-pane">
        <div className="auth-hero-overlay" />
        <div className="auth-hero-content">
          {/* Brand Header */}
          <div className="auth-hero-brand">
            <div
              style={{
                backgroundColor: '#FFFFFF',
                borderRadius: '12px',
                padding: '6px 14px',
                display: 'inline-flex',
                alignItems: 'center',
                boxShadow: '0 4px 16px rgba(0, 0, 0, 0.2)',
              }}
            >
              <img
                src={finflowLogo}
                alt="FinFlow - Finance & HR Policy Assistant"
                style={{ height: '36px', width: 'auto', display: 'block', objectFit: 'contain' }}
              />
            </div>
            <span
              className="auth-hero-badge"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                backdropFilter: 'blur(8px)',
                color: '#FFFFFF',
                border: '1px solid rgba(255, 255, 255, 0.25)',
              }}
            >
              Enterprise Service Management
            </span>
          </div>

          {/* Hero Core Content */}
          <div className="auth-hero-main">
            <h2 className="auth-hero-headline">
              Intelligent Finance &amp; HR Policy Assistant
            </h2>
            <p className="auth-hero-desc">
              Automated RAG policy compliance, multi-tier organizational hierarchy, and frictionless expense &amp; leave workflows engineered for high-performance enterprises.
            </p>

            <div className="auth-hero-features">
              <div className="auth-hero-card">
                <span className="auth-hero-card-icon">🧠</span>
                <div>
                  <div className="auth-hero-card-title">RAG-Powered HR &amp; Policy Intelligence</div>
                  <div className="auth-hero-card-sub">Instant semantic verification against internal HR policies and employee leave guidelines.</div>
                </div>
              </div>
              <div className="auth-hero-card">
                <span className="auth-hero-card-icon">🏢</span>
                <div>
                  <div className="auth-hero-card-title">Multi-Tier Enterprise Hierarchy</div>
                  <div className="auth-hero-card-sub">Super Admins, Managers, and Employees organized in streamlined, secure team workspaces.</div>
                </div>
              </div>
              <div className="auth-hero-card">
                <span className="auth-hero-card-icon">⚡</span>
                <div>
                  <div className="auth-hero-card-title">Automated Expense &amp; Finance Approvals</div>
                  <div className="auth-hero-card-sub">Real-time expense validation, Razorpay settlement integration, and automated audits.</div>
                </div>
              </div>
            </div>
          </div>

          {/* Trust & Compliance Footer */}
          <div className="auth-hero-footer">
            <div className="auth-hero-metric">
              <span style={{ color: '#10B981' }}>●</span> 99.98% System Uptime
            </div>
            <div className="auth-hero-metric">
              <span>🔒</span> Zero-Trust Access
            </div>
          
          </div>
        </div>
      </div>

      {/* Right Authentication Form Pane */}
      <div className="auth-form-pane">
        <div className="auth-form-container">
          {/* Brand header on form side */}
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '14px' }}>
              <img
                src={finflowLogo}
                alt="FinFlow - Finance & HR Policy Assistant"
                style={{
                  height: '52px',
                  maxWidth: '240px',
                  objectFit: 'contain',
                }}
              />
            </div>
            <h1
              style={{
                fontSize: '24px',
                fontWeight: 700,
                color: 'var(--color-text-primary)',
                letterSpacing: '-0.02em',
                marginBottom: '6px',
              }}
            >
              {title}
            </h1>
            {subtitle && (
              <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)', margin: 0 }}>
                {subtitle}
              </p>
            )}
          </div>

          {/* Auth Card Box */}
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--color-border)',
              padding: '28px',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            {children}
          </div>

         
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
