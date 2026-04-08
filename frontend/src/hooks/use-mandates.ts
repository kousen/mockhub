import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as mandatesApi from '@/api/mandates';
import type { CreateMandateRequest } from '@/types/mandate';
import { useAuthStore } from '@/stores/auth-store';

export function useMyMandates() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  return useQuery({
    queryKey: ['my-mandates'],
    queryFn: () => mandatesApi.getMyMandates(),
    enabled: isAuthenticated,
  });
}

export function useCreateMandate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateMandateRequest) => mandatesApi.createMandate(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-mandates'] });
    },
  });
}

export function useRevokeMandate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (mandateId: string) => mandatesApi.revokeMandate(mandateId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-mandates'] });
    },
  });
}
