import apiClient from './client';

export interface SpotifyArtist {
  id: string;
  name: string;
  genres: string[];
  followers: number;
  imageUrl: string | null;
}

export async function getSpotifyArtist(spotifyArtistId: string): Promise<SpotifyArtist> {
  const { data } = await apiClient.get<SpotifyArtist>(`/api/v1/spotify/artists/${spotifyArtistId}`);
  return data;
}
