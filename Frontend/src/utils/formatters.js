export function formatDate(dateString) {
  if (!dateString) return '—';
  try {
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return dateString;
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
}

export function formatDateTime(dateString) {
  if (!dateString) return '—';
  try {
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return dateString;
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateString;
  }
}

export function formatRole(role) {
  switch (role) {
    case 'SUPER_ADMIN':
      return 'Super Admin';
    case 'MANAGER':
      return 'Manager';
    case 'EMPLOYEE':
      return 'Employee';
    case 'FINANCE':
      return 'Finance Head';
    default:
      return role || '—';
  }
}

export function formatLeaveType(type) {
  switch (type) {
    case 'CASUAL':
      return 'Casual Leave';
    case 'MEDICAL':
      return 'Medical Leave';
    case 'OFFICIAL_DUTY':
      return 'Official Duty (OD)';
    case 'SICK':
      return 'Medical / Sick Leave';
    default:
      return type || 'Leave';
  }
}

export function getStatusBadgeVariant(status) {
  switch (status) {
    case 'APPROVED':
    case 'PAID':
    case 'ACTIVE':
    case true:
      return 'success';
    case 'PENDING':
    case 'MANAGER_REVIEW':
    case 'FINANCE_REVIEW':
      return 'warning';
    case 'REJECTED':
    case 'INACTIVE':
    case false:
      return 'danger';
    default:
      return 'neutral';
  }
}
