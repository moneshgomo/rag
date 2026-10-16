import React from 'react';

export const Table = ({
  columns = [],
  data = [],
  keyExtractor = (item, idx) => item.id || idx,
  loading = false,
  emptyMessage = 'No records found.',
  className = '',
}) => {
  return (
    <div className={`table-container ${className}`}>
      <table className="table">
        <thead>
          <tr>
            {columns.map((col, idx) => (
              <th
                key={col.key || idx}
                style={{
                  width: col.width,
                  textAlign: col.align || 'left',
                }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length} style={{ textAlign: 'center', padding: '32px' }}>
                <span className="spinner" style={{ borderColor: 'var(--color-primary)', borderTopColor: 'transparent', margin: '0 auto 8px', display: 'block' }} />
                <span style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
                  Loading records...
                </span>
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} style={{ textAlign: 'center', padding: '36px', color: 'var(--color-text-muted)' }}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((item, idx) => (
              <tr key={keyExtractor(item, idx)}>
                {columns.map((col, cIdx) => (
                  <td
                    key={col.key || cIdx}
                    style={{ textAlign: col.align || 'left' }}
                  >
                    {col.render ? col.render(item, idx) : item[col.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

export default Table;
