import { useCallback, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EventGrid } from '@/components/events/EventGrid';
import { EventSearch } from '@/components/events/EventSearch';
import { CategoryNav } from '@/components/events/CategoryNav';
import { useFeaturedEvents } from '@/hooks/use-events';
import { ROUTES } from '@/lib/constants';

export function HomePage() {
  const navigate = useNavigate();
  const { data: featuredEvents, isLoading } = useFeaturedEvents();
  const [searchValue, setSearchValue] = useState('');

  const handleSearch = useCallback(
    (q: string) => {
      setSearchValue(q);
      if (q.trim().length > 0) {
        navigate(`${ROUTES.EVENTS}?q=${encodeURIComponent(q)}`);
      }
    },
    [navigate],
  );

  const handleCategorySelect = useCallback(
    (category: string | undefined) => {
      if (category) {
        navigate(`${ROUTES.EVENTS}?category=${encodeURIComponent(category)}`);
      } else {
        navigate(ROUTES.EVENTS);
      }
    },
    [navigate],
  );

  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-primary/10 via-background to-primary/5 py-16 sm:py-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
              Find Your Next
              <span className="text-primary"> Live Experience</span>
            </h1>
            <p className="mt-4 text-lg text-muted-foreground sm:text-xl">
              MockHub is a secondary ticket marketplace where fans buy and sell tickets to
              concerts, festivals, and live events. Discover amazing shows at fair prices.
            </p>
            <div className="mx-auto mt-8 max-w-xl">
              <EventSearch
                value={searchValue}
                onChange={handleSearch}
                placeholder="Search events, artists, venues..."
              />
            </div>
          </div>
        </div>
      </section>

      {/* Category Navigation */}
      <section className="border-b py-4">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <CategoryNav onCategoryChange={handleCategorySelect} />
        </div>
      </section>

      {/* Featured Events Section */}
      <section className="py-12 sm:py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-8 flex items-center justify-between">
            <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
              Featured Events
            </h2>
            <Button variant="ghost" asChild>
              <Link to={ROUTES.EVENTS}>
                View all
                <ArrowRight className="ml-1 h-4 w-4" />
              </Link>
            </Button>
          </div>
          <EventGrid events={featuredEvents ?? []} isLoading={isLoading} />
        </div>
      </section>

      {/* Browse All CTA */}
      <section className="border-t bg-muted/30 py-12">
        <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
          <h3 className="text-lg font-semibold">Looking for something specific?</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            Browse our full catalog of events with advanced filtering, sorting, and
            search.
          </p>
          <Button className="mt-4" asChild>
            <Link to={ROUTES.EVENTS}>
              Browse All Events
              <ArrowRight className="ml-2 h-4 w-4" />
            </Link>
          </Button>
        </div>
      </section>

      {/* Teaching Note Section */}
      <section className="border-t border-border bg-muted/30 py-12">
        <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
          <h3 className="text-lg font-semibold">Built for Learning</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            MockHub is a teaching platform designed for undergraduate AI students. It
            demonstrates modern web development patterns including React 19, TypeScript,
            Spring Boot, and AI integration with real-world architecture decisions.
          </p>
        </div>
      </section>
    </div>
  );
}
