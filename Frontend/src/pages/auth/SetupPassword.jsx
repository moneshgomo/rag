import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import authService from '../../services/authService';
import AuthLayout from '../../layouts/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import { extractErrorMessage } from '../../utils/errorHandler';
import { CheckCircle2, AlertTriangle, KeyRound, ArrowRight } from 'lucide-react';

export const SetupPassword = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const queryToken = new URLSearchParams(location.search).get('token') || '';

  const [isValidating, setIsValidating] = useState(!!queryToken);
  const [tokenInfo, setTokenInfo] = useState(null); // { valid, email, name, role, message }

  const [formData, setFormData] = useState({
    token: queryToken,
    temporaryPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [setupComplete, setSetupComplete] = useState(false);

  // Regeneration state
  const [resendEmail, setResendEmail] = useState('');
  const [resending, setResending] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(null);

  // Validate token if present in URL
  useEffect(() => {
    if (!queryToken) {
      setIsValidating(false);
      return;
    }

    const validateToken = async () => {
      setIsValidating(true);
      try {
        const res = await authService.validateSetupToken(queryToken);
        setTokenInfo(res);
        if (res.email) {
          setResendEmail(res.email);
        }
      } catch (err) {
        const errObj = extractErrorMessage(err);
        setTokenInfo({ valid: false, message: errObj.message });
      } finally {
        setIsValidating(false);
      }
    };

    validateToken();
  }, [queryToken]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
    if (apiError) setApiError(null);
  };

  const validate = () => {
    const newErrors = {};

    const effectiveToken = (formData.token || queryToken).trim();
    if (!effectiveToken) {
      newErrors.token = 'Setup token is required.';
    }

    if (!formData.newPassword) {
      newErrors.newPassword = 'New permanent password is required.';
    } else if (formData.newPassword.length < 6) {
      newErrors.newPassword = 'Password must be at least 6 characters.';
    }

    if (formData.newPassword && formData.temporaryPassword && formData.newPassword === formData.temporaryPassword) {
      newErrors.newPassword = 'New permanent password must be different from your temporary password.';
    }

    if (formData.newPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setApiError(null);

    const effectiveToken = (formData.token || queryToken).trim();

    try {
      await authService.setupPassword({
        token: effectiveToken,
        temporaryPassword: formData.temporaryPassword.trim() || undefined,
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword,
      });
      setSetupComplete(true);
    } catch (err) {
      const errObj = extractErrorMessage(err);
      setApiError(errObj);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegenerate = async (e) => {
    e.preventDefault();
    if (!resendEmail.trim()) {
      setApiError({ message: 'Please enter your registered work email.' });
      return;
    }

    setResending(true);
    setApiError(null);
    setResendSuccess(null);

    try {
      const res = await authService.regenerateSetupLink({
        email: resendEmail.trim(),
        token: queryToken || undefined,
      });
      setResendSuccess(res.message || 'A new setup link has been sent to your email.');
    } catch (err) {
      const errObj = extractErrorMessage(err);
      setApiError(errObj);
    } finally {
      setResending(false);
    }
  };

  // 1. Loading State while validating token
  if (isValidating) {
    return (
      <AuthLayout title="Verifying Setup Link" subtitle="Please wait while we validate your token...">
        <div style={{ textAlign: 'center', padding: '36px 0', color: '#64748b', fontSize: '14px' }}>
          Checking activation credentials...
        </div>
      </AuthLayout>
    );
  }

  // 2. Success Screen
  if (setupComplete) {
    return (
      <AuthLayout title="Account Ready" subtitle="Your permanent password has been successfully configured.">
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div
            style={{
              width: '60px',
              height: '60px',
              borderRadius: '50%',
              backgroundColor: '#ecfdf5',
              color: '#059669',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px',
            }}
          >
            <CheckCircle2 size={36} />
          </div>

          <h3 style={{ fontSize: '18px', fontWeight: 600, color: '#0f172a', marginBottom: '8px' }}>
            Password Setup Complete!
          </h3>
          <p style={{ fontSize: '14px', color: '#64748b', lineHeight: 1.6, marginBottom: '28px' }}>
            Your temporary credentials have been replaced. You can now securely sign in to your FinFlow workspace using your permanent password.
          </p>

          <Button
            variant="primary"
            block
            onClick={() => navigate('/login', { replace: true })}
          >
            Proceed to Sign In
          </Button>
        </div>
      </AuthLayout>
    );
  }

  // 3. Simple message when token is invalid or expired
  if (tokenInfo && !tokenInfo.valid) {
    return (
      <AuthLayout
        title="Setup Link Expired"
        subtitle="Your password setup link is invalid or has expired."
      >
        <div style={{ padding: '8px 0' }}>
          <div
            style={{
              backgroundColor: '#fffbeb',
              border: '1px solid #fde68a',
              borderRadius: '8px',
              padding: '16px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '12px',
            }}
          >
            <AlertTriangle size={22} style={{ color: '#d97706', flexShrink: 0, marginTop: '2px' }} />
            <div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#92400e', marginBottom: '4px' }}>
                Token is Expired
              </div>
              <div style={{ fontSize: '13px', color: '#b45309', lineHeight: 1.5 }}>
                Your password setup token has expired. If you want to set your password, please request a new password setup link below.
              </div>
            </div>
          </div>

          {resendSuccess ? (
            <div
              style={{
                backgroundColor: '#f0fdf4',
                border: '1px solid #bbf7d0',
                borderRadius: '8px',
                padding: '16px',
                marginBottom: '20px',
                textAlign: 'center',
              }}
            >
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#166534', marginBottom: '4px' }}>
                New Setup Link Sent
              </div>
              <div style={{ fontSize: '13px', color: '#15803d' }}>
                {resendSuccess}
              </div>
            </div>
          ) : (
            <form onSubmit={handleRegenerate} noValidate>
              {apiError && (
                <Alert
                  variant="danger"
                  message={apiError.message}
                  onClose={() => setApiError(null)}
                />
              )}

              <Input
                id="resendEmail"
                name="resendEmail"
                label="Registered Work Email"
                type="email"
                placeholder="you@company.com"
                value={resendEmail}
                onChange={(e) => setResendEmail(e.target.value)}
                required
                autoFocus
              />

              <div style={{ marginTop: '18px' }}>
                <Button
                  type="submit"
                  variant="primary"
                  block
                  loading={resending}
                >
                  Request a New Setup Link
                </Button>
              </div>
            </form>
          )}

          <div
            style={{
              textAlign: 'center',
              marginTop: '24px',
              paddingTop: '16px',
              borderTop: '1px solid var(--color-border-subtle)',
            }}
          >
            <Link to="/login" style={{ fontSize: '13px', color: '#64748b', textDecoration: 'none' }}>
              &larr; Back to <span style={{ color: 'var(--color-primary)', fontWeight: 600 }}>Sign in</span>
            </Link>
          </div>
        </div>
      </AuthLayout>
    );
  }

  // 4. Active Setup Password Form (Valid token or direct entry)
  return (
    <AuthLayout
      title="Set Permanent Password"
      subtitle={
        tokenInfo?.name
          ? `Welcome ${tokenInfo.name}! Set your permanent password to activate your account.`
          : 'Set your permanent password to activate your FinFlow account.'
      }
    >
      {/* Account Info Badge if token is valid */}
      {tokenInfo?.valid && (
        <div
          style={{
            marginBottom: '20px',
            backgroundColor: '#f0fdf4',
            border: '1px solid #bbf7d0',
            borderRadius: '8px',
            padding: '12px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
          }}
        >
          <KeyRound size={20} style={{ color: '#16a34a', flexShrink: 0 }} />
          <div>
            <div style={{ fontSize: '13px', fontWeight: 600, color: '#166534' }}>
              {tokenInfo.name} ({tokenInfo.role})
            </div>
            <div style={{ fontSize: '12px', color: '#15803d' }}>
              {tokenInfo.email}
            </div>
          </div>
        </div>
      )}

      {apiError && (
        <Alert
          variant="danger"
          title="Setup failed"
          message={apiError.message}
          onClose={() => setApiError(null)}
        />
      )}

      <form onSubmit={handleSubmit} noValidate>
        {/* Token input shown only if not provided in URL */}
        {!queryToken && (
          <Input
            id="token"
            name="token"
            label="Setup Token"
            type="text"
            placeholder="Paste token from email link"
            value={formData.token}
            onChange={handleChange}
            error={errors.token}
            helper="Copy the token from your onboarding email"
            required
          />
        )}

        <Input
          id="temporaryPassword"
          name="temporaryPassword"
          label="Temporary Password"
          type="password"
          placeholder="Enter temporary password from email"
          value={formData.temporaryPassword}
          onChange={handleChange}
          error={errors.temporaryPassword}
          helper="Found in the welcome email sent to you"
          autoComplete="current-password"
        />

        <Input
          id="newPassword"
          name="newPassword"
          label="New Permanent Password"
          type="password"
          placeholder="Minimum 6 characters"
          value={formData.newPassword}
          onChange={handleChange}
          error={errors.newPassword}
          helper="Must be different from your temporary password (min. 6 characters)"
          autoComplete="new-password"
          required
        />

        <Input
          id="confirmPassword"
          name="confirmPassword"
          label="Confirm Permanent Password"
          type="password"
          placeholder="Re-enter permanent password"
          value={formData.confirmPassword}
          onChange={handleChange}
          error={errors.confirmPassword}
          autoComplete="new-password"
          required
        />

        <div style={{ marginTop: '22px' }}>
          <Button
            type="submit"
            variant="primary"
            block
            loading={submitting}
          >
            Activate Account &amp; Save Password
          </Button>
        </div>

        <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '13px', color: '#64748b' }}>
          Token expired or having trouble?{' '}
          <button
            type="button"
            onClick={() => {
              setTokenInfo({ valid: false, message: 'Requested new setup link' });
            }}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--color-primary)',
              fontWeight: 600,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            Request a new setup link
          </button>
        </div>

        <div
          style={{
            textAlign: 'center',
            marginTop: '20px',
            paddingTop: '16px',
            borderTop: '1px solid var(--color-border-subtle)',
          }}
        >
          <Link to="/login" style={{ fontSize: '13px', color: '#64748b', textDecoration: 'none' }}>
            &larr; Return to <span style={{ color: 'var(--color-primary)', fontWeight: 600 }}>Sign in</span>
          </Link>
        </div>
      </form>
    </AuthLayout>
  );
};

export default SetupPassword;
