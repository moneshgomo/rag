import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/common/Button';
import { Home } from 'lucide-react';

export const NotFound = () => {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '24px',
        backgroundColor: '#FFFFFF',
      }}
    >
      <div
        style={{
          fontSize: '72px',
          fontWeight: 800,
          color: 'var(--color-primary)',
          lineHeight: 1,
          marginBottom: '12px',
          letterSpacing: '-0.04em',
        }}
      >
        404
      </div>
      <h1 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '8px' }}>
        Page Not Found
      </h1>
      <p
        style={{
          fontSize: '14px',
          color: 'var(--color-text-secondary)',
          maxWidth: '400px',
          marginBottom: '24px',
        }}
      >
        The requested URL was not found on this workspace. It may have moved or requires different authorization.
      </p>
      <Link to="/login">
        <Button variant="primary" icon={Home}>
          Return to Workspace
        </Button>
      </Link>
    </div>
  );
};

export default NotFound;
