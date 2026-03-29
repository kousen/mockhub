import apiClient from './client';
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UserDto,
  UpdateProfileRequest,
} from '@/types/auth';

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/auth/login', data);
  return response.data;
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/auth/register', data);
  return response.data;
}

export async function refreshToken(): Promise<{ accessToken: string }> {
  const response = await apiClient.post<{ accessToken: string }>('/auth/refresh');
  return response.data;
}

export async function getMe(): Promise<UserDto> {
  const response = await apiClient.get<UserDto>('/auth/me');
  return response.data;
}

export async function updateMe(data: UpdateProfileRequest): Promise<UserDto> {
  const response = await apiClient.put<UserDto>('/auth/me', data);
  return response.data;
}

export async function exchangeOAuth2Code(code: string): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>(`/auth/oauth2/exchange?code=${code}`);
  return response.data;
}

export async function getLinkedProviders(): Promise<string[]> {
  const response = await apiClient.get<string[]>('/auth/me/providers');
  return response.data;
}

export async function unlinkProvider(provider: string): Promise<void> {
  await apiClient.delete(`/auth/me/providers/${provider}`);
}
