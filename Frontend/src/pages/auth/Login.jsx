import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import AuthLayout from '../../layouts/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';

export const Login = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isExpired = new URLSearchParams(location.search).get('expired');

  const validate = () => {
    const newErrors = {};
    if (!formData.email.trim()) {
      newErrors.email = 'Work email is required.';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Please provide a valid email address.';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
    if (apiError) setApiError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setApiError(null);

    try {
      const result = await login(formData.email.trim(), formData.password);
      
      // Role-based redirection strictly based on backend response
      if (result.role === 'SUPER_ADMIN') {
        navigate('/admin/dashboard', { replace: true });
      } else if (result.role === 'MANAGER') {
        navigate('/manager/dashboard', { replace: true });
      } else {
        navigate('/employee/dashboard', { replace: true });
      }
    } catch (err) {
      setApiError(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to your FinFlow workspace."
    >
      {isExpired && (
        <Alert
          variant="info"
          message="Your session has expired. Please sign in again."
        />
      )}

      {apiError && (
        <Alert
          variant="danger"
          title="Sign in failed"
          message={apiError.message}
          statusCode={apiError.status}
          onClose={() => setApiError(null)}
        />
      )}

      <form onSubmit={handleSubmit} noValidate>
        <Input
          id="email"
          name="email"
          label="Work email"
          type="email"
          placeholder="name@company.com"
          value={formData.email}
          onChange={handleChange}
          error={errors.email}
          required
          autoComplete="email"
          autoFocus
          disabled={loading}
        />

        <Input
          id="password"
          name="password"
          label="Password"
          type="password"
          placeholder="••••••••••••"
          value={formData.password}
          onChange={handleChange}
          error={errors.password}
          required
          autoComplete="current-password"
          disabled={loading}
        />

        <div style={{ marginTop: '20px' }}>
          <Button
            type="submit"
            variant="primary"
            block
            loading={loading}
            disabled={loading}
          >
            Sign in
          </Button>
        </div>
      </form>

      <div
        style={{
          marginTop: '24px',
          textAlign: 'center',
          fontSize: '13px',
          color: 'var(--color-text-secondary)',
          borderTop: '1px solid var(--color-border-subtle)',
          paddingTop: '16px',
        }}
      >
        Don't have an account?{' '}
        <Link
          to="/signup"
          style={{ fontWeight: 600, color: 'var(--color-primary)' }}
        >
          Sign up
        </Link>
      </div>
    </AuthLayout>
  );
};

export default Login;
