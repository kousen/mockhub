import { Link } from 'react-router';
import { Calendar, MapPin, Sparkles, Star } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useRecommendations } from '@/hooks/use-ai';
import { formatCurrency, formatShortDate } from '@/lib/formatters';

function RecommendationSkeleton() {
  return (
    <div className="flex gap-4 overflow-x-auto pb-2">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="w-72 shrink-0">
          <Card className="h-full">
            <CardContent className="space-y-3">
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-10 w-full" />
            </CardContent>
          </Card>
        </div>
      ))}
    </div>
  );
}

/**
 * AI-powered event recommendations section for the homepage.
 * Displays a horizontally scrollable list of recommended events.
 * Hides entirely if AI is unavailable or no recommendations exist.
 */
export function RecommendationsSection() {
  const { data: recommendations, isLoading } = useRecommendations();

  // Hide entirely if no recommendations or AI unavailable
  if (!isLoading && (!recommendations || recommendations.length === 0)) {
    return null;
  }

  return (
    <section className="py-12 sm:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-8 flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-primary" />
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">Recommended for You</h2>
        </div>

        {isLoading ? (
          <RecommendationSkeleton />
        ) : (
          <div className="flex gap-4 overflow-x-auto pb-2">
            {recommendations?.map((rec) => (
              <Link
                key={rec.eventId}
                to={`/events/${rec.eventSlug}`}
                className="w-72 shrink-0 transition-transform hover:scale-[1.02]"
              >
                <Card className="h-full">
                  <CardContent className="space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="line-clamp-2 text-sm font-semibold leading-tight">
                        {rec.eventName}
                      </h3>
                      <Badge variant="secondary" className="shrink-0 text-xs">
                        <Star className="mr-0.5 h-3 w-3" />
                        {Math.round(rec.relevanceScore * 100)}%
                      </Badge>
                    </div>

                    <div className="space-y-1.5 text-xs text-muted-foreground">
                      <div className="flex items-center gap-1.5">
                        <MapPin className="h-3 w-3 shrink-0" />
                        <span className="truncate">
                          {rec.venueName}, {rec.city}
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <Calendar className="h-3 w-3 shrink-0" />
                        {formatShortDate(rec.eventDate)}
                      </div>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-emerald-700 dark:text-emerald-400">
                        From {formatCurrency(rec.minPrice)}
                      </span>
                    </div>

                    <p className="line-clamp-2 text-xs italic text-muted-foreground">
                      {rec.reason}
                    </p>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
