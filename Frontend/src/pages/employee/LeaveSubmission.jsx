import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import leaveService from '../../services/leaveService';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import Alert from '../../components/common/Alert';
import PolicyAdvisoryCard from '../../components/leave/PolicyAdvisoryCard';
import { FilePlus, ArrowLeft, Info, Calendar } from 'lucide-react';
import { extractErrorMessage } from '../../utils/errorHandler';

const isWeekend = (dateStr) => {
  if (!dateStr) return false;
  const [year, month, day] = dateStr.split('-').map(Number);
  const date = new Date(year, month - 1, day, 12, 0, 0);
  const dayOfWeek = date.getDay(); 
  return dayOfWeek === 0 || dayOfWeek === 6;
};

const calculateWorkingDays = (startDateStr, endDateStr) => {
  if (!startDateStr || !endDateStr) return 0;
  const [sY, sM, sD] = startDateStr.split('-').map(Number);
  const [eY, eM, eD] = endDateStr.split('-').map(Number);
  const start = new Date(sY, sM - 1, sD, 12, 0, 0);
  const end = new Date(eY, eM - 1, eD, 12, 0, 0);

  if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) {
    return 0;
  }

  let workingDays = 0;
  const current = new Date(start);
  while (current <= end) {
    const day = current.getDay();
    if (day !== 0 && day !== 6) {
      workingDays++;
    }
    current.setDate(current.getDate() + 1);
  }
  return workingDays;
};

export const LeaveSubmission = () => {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    leaveType: 'CASUAL',
    startDate: '',
    endDate: '',
    reason: '',
  });

  const [durationDays, setDurationDays] = useState(0);
  const [balance, setBalance] = useState(null);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState(null);
  const [ragAdvisory, setRagAdvisory] = useState(null);

  useEffect(() => {
    const fetchBalance = async () => {
      try {
        const bal = await leaveService.getMyBalance();
        setBalance(bal);
      } catch (e) {
        console.warn('Could not fetch leave balance:', e);
      }
    };
    fetchBalance();
  }, []);

  useEffect(() => {
    if (formData.startDate && formData.endDate) {
      const workingDays = calculateWorkingDays(formData.startDate, formData.endDate);
      setDurationDays(workingDays);
    } else {
      setDurationDays(0);
    }
  }, [formData.startDate, formData.endDate]);

  const validate = () => {
    const newErrors = {};

    if (!formData.leaveType) {
      newErrors.leaveType = 'Please select a leave category.';
    }

    if (!formData.startDate) {
      newErrors.startDate = 'Start date is required.';
    } else if (isWeekend(formData.startDate)) {
      newErrors.startDate = 'Leave cannot start on Saturday or Sunday. Please choose a weekday (Monday – Friday).';
    }

    if (!formData.endDate) {
      newErrors.endDate = 'End date is required.';
    } else if (isWeekend(formData.endDate)) {
      newErrors.endDate = 'Leave cannot end on Saturday or Sunday. Please choose a weekday (Monday – Friday).';
    }

    if (formData.startDate && formData.endDate) {
      const [sY, sM, sD] = formData.startDate.split('-').map(Number);
      const [eY, eM, eD] = formData.endDate.split('-').map(Number);
      const start = new Date(sY, sM - 1, sD, 12, 0, 0);
      const end = new Date(eY, eM - 1, eD, 12, 0, 0);
      if (start > end) {
        newErrors.endDate = 'End date cannot be prior to start date.';
      } else if (durationDays === 0) {
        newErrors.general = 'The selected date range contains no working days (Monday – Friday). Saturdays and Sundays are holidays.';
      }
    }

    if (!formData.reason.trim()) {
      newErrors.reason = 'Please state a brief reason for your leave request.';
    } else if (formData.reason.trim().length < 5) {
      newErrors.reason = 'Please provide a descriptive reason (at least 5 characters).';
    }

    if (balance && durationDays > balance.remainingDays) {
      newErrors.general = `Insufficient balance. Requested: ${durationDays} working days, Available: ${balance.remainingDays} days.`;
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
    if (errors.general) {
      setErrors((prev) => ({ ...prev, general: null }));
    }
    if (apiError) setApiError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setApiError(null);

    try {
      const result = await leaveService.submitLeave({
        leaveType: formData.leaveType,
        startDate: formData.startDate,
        endDate: formData.endDate,
        daysCount: durationDays,
        reason: formData.reason.trim(),
      });

      // Show RAG evaluation if provided
      if (result.ragDecision) {
        setRagAdvisory(result.ragDecision);
      }

      setTimeout(() => {
        navigate('/employee/dashboard');
      }, 1000);
    } catch (err) {
      setApiError(extractErrorMessage(err));
      setLoading(false);
    }
  };

  return (
    <div>
      {}
      <div style={{ marginBottom: '24px' }}>
        <button
          type="button"
          onClick={() => navigate('/employee/dashboard')}
          className="btn-ghost"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', padding: '4px 0', marginBottom: '8px' }}
        >
          <ArrowLeft size={14} /> Back to Dashboard
        </button>
        <h1 className="heading-page">Request Leave</h1>
        <p className="subheading" style={{ marginBottom: 0 }}>
          Submit your leave dates for manager review. Your request is automatically checked against company policy.
        </p>
      </div>

      {apiError && (
        <Alert
          variant="danger"
          title="Submission Failed"
          message={apiError.message}
          statusCode={apiError.status}
          onClose={() => setApiError(null)}
        />
      )}

      {errors.general && (
        <Alert
          variant="danger"
          message={errors.general}
        />
      )}

      {}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1.2fr 0.8fr',
          gap: '32px',
          alignItems: 'start',
        }}
      >
        {}
        <div
          style={{
            backgroundColor: '#FFFFFF',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-lg)',
            padding: '28px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <form onSubmit={handleSubmit} noValidate>
            <Select
              id="leaveType"
              name="leaveType"
              label="Leave Category"
              value={formData.leaveType}
              onChange={handleChange}
              options={[
                { value: 'CASUAL', label: 'Casual Leave (Personal events, appointments, family commitments)' },
                { value: 'MEDICAL', label: 'Medical Leave (Illness, doctor visits, surgery recovery)' },
                { value: 'OFFICIAL_DUTY', label: 'Official Duty (Client site, conference, company representation)' },
              ]}
              error={errors.leaveType}
              required
            />

            <div className="grid-2">
              <Input
                id="startDate"
                name="startDate"
                label="Start Date"
                type="date"
                value={formData.startDate}
                onChange={handleChange}
                error={errors.startDate}
                helper={formData.startDate && isWeekend(formData.startDate) ? '⚠️ Saturday/Sunday is a holiday' : ''}
                required
                min={new Date().toISOString().split('T')[0]}
              />

              <Input
                id="endDate"
                name="endDate"
                label="End Date"
                type="date"
                value={formData.endDate}
                onChange={handleChange}
                error={errors.endDate}
                helper={formData.endDate && isWeekend(formData.endDate) ? '⚠️ Saturday/Sunday is a holiday' : ''}
                required
                min={formData.startDate || new Date().toISOString().split('T')[0]}
              />
            </div>

            {}
            <div
              style={{
                marginBottom: '14px',
                fontSize: '12px',
                color: 'var(--color-text-secondary)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <Info size={14} color="var(--color-primary)" />
              <span>
                Leave can only be requested for <strong>Monday through Friday</strong>. Saturday & Sunday are holidays and excluded from leave counts.
              </span>
            </div>

            {durationDays > 0 && (
              <div
                style={{
                  marginBottom: '16px',
                  padding: '10px 14px',
                  backgroundColor: '#F0FDF4',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid #BBF7D0',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: '13px',
                  color: '#15803D',
                  fontWeight: 500,
                }}
              >
                <Calendar size={16} />
                <span>
                  Calculated Duration: <strong>{durationDays}</strong> working day{durationDays > 1 ? 's' : ''} (Saturday & Sunday excluded)
                </span>
              </div>
            )}

            {formData.startDate && formData.endDate && durationDays === 0 && (
              <div
                style={{
                  marginBottom: '16px',
                  padding: '10px 14px',
                  backgroundColor: '#FEF2F2',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid #FECACA',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: '13px',
                  color: '#B91C1C',
                }}
              >
                <Info size={16} />
                <span>
                  The selected dates fall on a weekend. Saturday and Sunday are holidays and do not require leave requests.
                </span>
              </div>
            )}

            <Textarea
              id="reason"
              name="reason"
              label="Reason for Leave"
              placeholder="State a brief purpose for your leave (e.g., attending a family wedding, seasonal viral flu rest)..."
              value={formData.reason}
              onChange={handleChange}
              error={errors.reason}
              rows={4}
              required
            />

            {}
            <div
              style={{
                padding: '12px 14px',
                backgroundColor: '#F9FAFB',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border-subtle)',
                marginBottom: '24px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '8px',
                fontSize: '12px',
                color: 'var(--color-text-secondary)',
                lineHeight: 1.5,
              }}
            >
              <Info size={16} style={{ flexShrink: 0, marginTop: '2px', color: 'var(--color-text-muted)' }} />
              <div>
                <strong>Physical Document Submission:</strong> Medical prescriptions, hospital receipts, or event invitations are to be submitted physically to HR upon return to office where applicable. No digital upload is required.
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <Button
                type="submit"
                variant="primary"
                loading={loading}
                disabled={loading}
                icon={FilePlus}
              >
                Submit Leave Request
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate('/employee/dashboard')}
                disabled={loading}
              >
                Cancel
              </Button>
            </div>
          </form>
        </div>

        {}
        <div>
          <PolicyAdvisoryCard currentRAGAdvisory={ragAdvisory} />
        </div>
      </div>

      <style>{`
        @media (max-width: 900px) {
          div[style*="grid-template-columns: 1.2fr 0.8fr"] {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
};

export default LeaveSubmission;
