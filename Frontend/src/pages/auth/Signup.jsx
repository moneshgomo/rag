import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import AuthLayout from '../../layouts/AuthLayout';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';

export const Signup = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    contactNumber: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [loading, setLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const validate = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Full name is required.';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Work email is required.';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Please provide a valid email address.';
    }

    if (!formData.contactNumber.trim()) {
      newErrors.contactNumber = 'Contact number is required.';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required.';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters.';
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match.';
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
      const result = await register({
        name: formData.name.trim(),
        email: formData.email.trim(),
        contactNumber: formData.contactNumber.trim(),
        password: formData.password,
      });

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
      title="Create your account"
      subtitle="Get started with your FinFlow workspace."
    >
      {apiError && (
        <Alert
          variant="danger"
          title="Registration failed"
          message={apiError.message}
          statusCode={apiError.status}
          onClose={() => setApiError(null)}
        />
      )}

      <form onSubmit={handleSubmit} noValidate>
        <Input
          id="name"
          name="name"
          label="Full name"
          placeholder="Jane Doe"
          value={formData.name}
          onChange={handleChange}
          error={errors.name}
          required
          autoFocus
          disabled={loading}
        />

        <Input
          id="email"
          name="email"
          label="Work email"
          type="email"
          placeholder="jane@company.com"
          value={formData.email}
          onChange={handleChange}
          error={errors.email}
          required
          autoComplete="email"
          disabled={loading}
        />

        <Input
          id="contactNumber"
          name="contactNumber"
          label="Contact number"
          type="tel"
          placeholder="+91 9876543210"
          value={formData.contactNumber}
          onChange={handleChange}
          error={errors.contactNumber}
          required
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
          autoComplete="new-password"
          disabled={loading}
        />

        <Input
          id="confirmPassword"
          name="confirmPassword"
          label="Confirm password"
          type="password"
          placeholder="••••••••••••"
          value={formData.confirmPassword}
          onChange={handleChange}
          error={errors.confirmPassword}
          required
          autoComplete="new-password"
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
            Create account
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
        Already have an account?{' '}
        <Link
          to="/login"
          style={{ fontWeight: 600, color: 'var(--color-primary)' }}
        >
          Sign in
        </Link>
      </div>
    </AuthLayout>
  );
};

export default Signup;
