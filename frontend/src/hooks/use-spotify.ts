import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getSpotifyArtist, getSpotifyConnection, disconnectSpotify } from '@/api/spotify';

export function useSpotifyArtist(spotifyArtistId: string | null) {
  return useQuery({
    queryKey: ['spotify', 'artist', spotifyArtistId],
    queryFn: () => getSpotifyArtist(spotifyArtistId!),
    enabled: !!spotifyArtistId,
    staleTime: 60 * 60 * 1000, // 1 hour — artist data rarely changes
    retry: false,
  });
}

export function useSpotifyConnection() {
  return useQuery({
    queryKey: ['spotify', 'connection'],
    queryFn: () => getSpotifyConnection(),
    staleTime: 60 * 1000,
    retry: false,
  });
}

export function useDisconnectSpotify() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => disconnectSpotify(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['spotify', 'connection'] });
      queryClient.invalidateQueries({ queryKey: ['ai', 'recommendations'] });
    },
  });
}
