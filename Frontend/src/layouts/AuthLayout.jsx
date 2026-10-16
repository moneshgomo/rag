import React from 'react';
import { Link } from 'react-router-dom';

export const AuthLayout = ({ children, title, subtitle }) => {
  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#FFFFFF',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        padding: '32px 16px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '420px',
        }}
      >
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '8px',
              backgroundColor: 'var(--color-primary)',
              color: '#FFFFFF',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
              fontSize: '20px',
              marginBottom: '16px',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            F
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
            <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>
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
          FinFlow &copy; {new Date().getFullYear()} — Enterprise Operations & Leave Policy Intelligence
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
