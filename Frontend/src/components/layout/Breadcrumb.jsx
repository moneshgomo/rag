import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

export const Breadcrumb = ({ items = [] }) => {
  if (!items || items.length === 0) return null;

  return (
    <nav aria-label="Breadcrumb" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
      {items.map((item, index) => {
        const isLast = index === items.length - 1;
        return (
          <React.Fragment key={index}>
            {index > 0 && <ChevronRight size={14} color="var(--color-text-muted)" />}
            {isLast || !item.to ? (
              <span
                style={{
                  fontSize: 'var(--font-size-sm)',
                  fontWeight: isLast ? 600 : 400,
                  color: isLast ? 'var(--color-text-primary)' : 'var(--color-text-secondary)',
                }}
              >
                {item.label}
              </span>
            ) : (
              <Link
                to={item.to}
                style={{
                  fontSize: 'var(--font-size-sm)',
                  color: 'var(--color-text-secondary)',
                  textDecoration: 'none',
                }}
              >
                {item.label}
              </Link>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};

export default Breadcrumb;
