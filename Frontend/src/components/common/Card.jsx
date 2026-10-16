import React from 'react';

export const Card = ({
  children,
  title,
  action,
  className = '',
  ...props
}) => {
  return (
    <div className={`card ${className}`} {...props}>
      {(title || action) && (
        <div className="card-header">
          {title && <h3 className="card-title">{title}</h3>}
          {action && <div className="card-action">{action}</div>}
        </div>
      )}
      {children}
    </div>
  );
};

export const StatCard = ({
  label,
  value,
  helper,
  icon: Icon,
  className = '',
}) => {
  return (
    <div className={`stat-card ${className}`}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span className="stat-label">{label}</span>
        {Icon && <Icon size={18} color="var(--color-primary)" />}
      </div>
      <div className="stat-value">{value}</div>
      {helper && <span className="stat-helper">{helper}</span>}
    </div>
  );
};

export default Card;
