import React from 'react';
import { AlertCircle, CheckCircle, Info } from 'lucide-react';

export const Alert = ({
  variant = 'danger', // danger | success | info
  title,
  message,
  statusCode,
  onClose,
  className = '',
}) => {
  if (!message && !title) return null;

  const IconComponent =
    variant === 'success' ? CheckCircle : variant === 'info' ? Info : AlertCircle;

  return (
    <div className={`alert alert-${variant} ${className}`}>
      <IconComponent size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          {title && <strong style={{ fontWeight: 600 }}>{title}</strong>}
          {statusCode !== undefined && statusCode !== null && statusCode !== 0 && (
            <span
              style={{
                fontSize: '11px',
                padding: '1px 6px',
                borderRadius: '4px',
                background: 'rgba(0, 0, 0, 0.08)',
                fontWeight: 600,
                fontFamily: 'monospace',
              }}
            >
              Status {statusCode}
            </span>
          )}
        </div>
        {message && <div style={{ marginTop: title ? '3px' : 0 }}>{message}</div>}
      </div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          style={{ background: 'none', border: 'none', cursor: 'pointer', opacity: 0.7, padding: '2px' }}
        >
          &times;
        </button>
      )}
    </div>
  );
};

export default Alert;
