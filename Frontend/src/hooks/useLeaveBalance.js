import { useState, useEffect } from 'react';
import leaveService from '../services/leaveService';
import { extractErrorMessage } from '../utils/errorHandler';

export function useLeaveBalance() {
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchBalance = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await leaveService.getMyBalance();
      setBalance(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBalance();
  }, []);

  return { balance, loading, error, refetch: fetchBalance };
}

export default useLeaveBalance;
