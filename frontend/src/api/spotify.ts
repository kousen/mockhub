import apiClient from './client';
import { isAxiosError } from 'axios';

export interface SpotifyArtist {
  id: string;
  name: string;
  genres: string[];
  followers: number;
  imageUrl: string | null;
}

export interface SpotifyConnectionStatus {
  connected: boolean;
  scopeUpgradeNeeded: boolean;
  spotifyDisplayName: string | null;
  connectedAt: string | null;
}

export async function getSpotifyArtist(spotifyArtistId: string): Promise<SpotifyArtist> {
  const { data } = await apiClient.get<SpotifyArtist>(`/api/v1/spotify/artists/${spotifyArtistId}`);
  return data;
}

export async function getSpotifyConnection(): Promise<SpotifyConnectionStatus> {
  try {
    const response = await apiClient.get<SpotifyConnectionStatus>('/spotify/connection');
    return response.data;
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 401) {
      return {
        connected: false,
        scopeUpgradeNeeded: false,
        spotifyDisplayName: null,
        connectedAt: null,
      };
    }
    throw error;
  }
}

export async function disconnectSpotify(): Promise<void> {
  await apiClient.delete('/spotify/connection');
}
