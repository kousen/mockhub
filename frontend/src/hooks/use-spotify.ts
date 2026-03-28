import { useQuery } from '@tanstack/react-query';
import { getSpotifyArtist } from '@/api/spotify';

export function useSpotifyArtist(spotifyArtistId: string | null) {
  return useQuery({
    queryKey: ['spotify', 'artist', spotifyArtistId],
    queryFn: () => getSpotifyArtist(spotifyArtistId!),
    enabled: !!spotifyArtistId,
    staleTime: 60 * 60 * 1000, // 1 hour — artist data rarely changes
    retry: false,
  });
}
