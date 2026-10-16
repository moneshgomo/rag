import { useState, useEffect } from 'react';
import teamService from '../services/teamService';
import { extractErrorMessage } from '../utils/errorHandler';

export function useTeam(teamId) {
  const [team, setTeam] = useState(null);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchTeamData = async () => {
    if (!teamId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [teamData, membersData] = await Promise.all([
        teamService.getTeam(teamId).catch(() => null),
        teamService.listTeamMembers(teamId).catch(() => []),
      ]);
      setTeam(teamData);
      setMembers(membersData || []);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamData();
  }, [teamId]);

  return { team, members, loading, error, refetch: fetchTeamData };
}

export default useTeam;
