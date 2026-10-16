import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import leaveService from '../../services/leaveService';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import { ShieldCheck, AlertCircle, CheckCircle, ArrowRight } from 'lucide-react';
import { extractErrorMessage } from '../../utils/errorHandler';

export const EmailLeaveAction = () => {
  const { token } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [resultHtml, setResultHtml] = useState(null);
  const [error, setError] = useState(null);

  const handleConfirmAction = async () => {
    if (!token) return;

    setLoading(true);
    setError(null);
    try {
      const response = await leaveService.executeTokenAction(token);
      setCompleted(true);
      setResultHtml(typeof response === 'string' ? response : null);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#FFFFFF',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px 16px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '480px',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-lg)',
          padding: '36px',
          boxShadow: 'var(--shadow-sm)',
          textAlign: 'center',
        }}
      >
        <div
          style={{
            width: '48px',
            height: '48px',
            borderRadius: '12px',
            backgroundColor: completed ? 'var(--color-success-bg)' : 'var(--color-primary-light)',
            color: completed ? 'var(--color-success)' : 'var(--color-primary)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '20px',
          }}
        >
          {completed ? <CheckCircle size={24} /> : <ShieldCheck size={24} />}
        </div>

        <h1
          style={{
            fontSize: '22px',
            fontWeight: 700,
            color: 'var(--color-text-primary)',
            marginBottom: '8px',
            letterSpacing: '-0.02em',
          }}
        >
          {completed ? 'Action Completed' : 'Confirm Action'}
        </h1>

        <p
          style={{
            fontSize: '14px',
            color: 'var(--color-text-secondary)',
            marginBottom: '24px',
            lineHeight: 1.5,
          }}
        >
          {completed
            ? 'Your decision has been immutably recorded in FinFlow and notifications have been dispatched.'
            : 'You are executing a secure, single-use action token generated from your email notification.'}
        </p>

        {error && (
          <div style={{ textAlign: 'left', marginBottom: '20px' }}>
            <Alert
              variant="danger"
              title="Token Action Failed"
              message={error.message}
              statusCode={error.status}
            />
          </div>
        )}

        {!completed ? (
          <div>
            <div
              style={{
                backgroundColor: '#F8FAFC',
                padding: '14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid #E2E8F0',
                fontSize: '12px',
                color: 'var(--color-text-muted)',
                marginBottom: '24px',
                textAlign: 'left',
              }}
            >
              <strong>Security Notice:</strong> Opening the link does not automatically commit changes. Clicking the confirmation button below will execute the decision directly in the FinFlow database.
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <Button
                variant="primary"
                onClick={handleConfirmAction}
                loading={loading}
                disabled={loading}
              >
                Confirm & Execute Action
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate('/login')}
                disabled={loading}
              >
                Go to Login
              </Button>
            </div>
          </div>
        ) : (
          <div>
            {resultHtml && (
              <div
                style={{
                  fontSize: '13px',
                  color: 'var(--color-text-secondary)',
                  marginBottom: '24px',
                  padding: '12px',
                  backgroundColor: '#F0FDF4',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid #BBF7D0',
                }}
              >
                Status: Recorded in system.
              </div>
            )}
            <Button
              variant="primary"
              onClick={() => navigate('/login')}
              icon={ArrowRight}
            >
              Return to FinFlow Workspace
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

export default EmailLeaveAction;
