import React from 'react';

export const Textarea = ({
  id,
  name,
  label,
  placeholder,
  value,
  onChange,
  onBlur,
  rows = 4,
  error,
  helper,
  required = false,
  disabled = false,
  className = '',
  ...props
}) => {
  const textareaId = id || name;

  return (
    <div className="form-group">
      {label && (
        <label htmlFor={textareaId} className="form-label">
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}

      <textarea
        id={textareaId}
        name={name}
        rows={rows}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        disabled={disabled}
        className={`form-textarea ${error ? 'is-invalid' : ''} ${className}`}
        {...props}
      />

      {error && <span className="form-error">{error}</span>}
      {helper && !error && <span className="form-helper">{helper}</span>}
    </div>
  );
};

export default Textarea;
