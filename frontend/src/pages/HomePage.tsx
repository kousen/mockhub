import { Link } from 'react-router';
import { ArrowRight, Calendar, MapPin, Ticket } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { ROUTES } from '@/lib/constants';

/** Placeholder event data for the home page. */
const placeholderEvents = [
  {
    id: 1,
    title: 'Summer Music Festival',
    venue: 'Madison Square Garden',
    date: 'Aug 15, 2026',
    priceFrom: 89,
  },
  {
    id: 2,
    title: 'Jazz in the Park',
    venue: 'Central Park Amphitheater',
    date: 'Sep 3, 2026',
    priceFrom: 45,
  },
  {
    id: 3,
    title: 'Rock Legends Tour',
    venue: 'Barclays Center',
    date: 'Oct 20, 2026',
    priceFrom: 120,
  },
];

export function HomePage() {
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
              MockHub is a secondary ticket marketplace where fans buy and sell tickets to concerts,
              festivals, and live events. Discover amazing shows at fair prices.
            </p>
            <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
              <Button size="lg" asChild>
                <Link to={ROUTES.EVENTS}>
                  Browse Events
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Link>
              </Button>
              <Button size="lg" variant="outline" asChild>
                <Link to={ROUTES.REGISTER}>Create Account</Link>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Events Section */}
      <section className="py-12 sm:py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-8 flex items-center justify-between">
            <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">Featured Events</h2>
            <Button variant="ghost" asChild>
              <Link to={ROUTES.EVENTS}>
                View all
                <ArrowRight className="ml-1 h-4 w-4" />
              </Link>
            </Button>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {placeholderEvents.map((event) => (
              <Card key={event.id} className="transition-shadow hover:shadow-lg">
                <CardHeader>
                  <div className="mb-2 flex h-40 items-center justify-center rounded-md bg-muted">
                    <Ticket className="h-12 w-12 text-muted-foreground/50" />
                  </div>
                  <CardTitle className="text-lg">{event.title}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <MapPin className="h-4 w-4" />
                    {event.venue}
                  </div>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-4 w-4" />
                    {event.date}
                  </div>
                </CardContent>
                <CardFooter className="flex items-center justify-between">
                  <span className="text-sm font-semibold">From ${event.priceFrom}</span>
                  <Button size="sm" variant="outline" asChild>
                    <Link to={ROUTES.EVENTS}>View Tickets</Link>
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Teaching Note Section */}
      <section className="border-t border-border bg-muted/30 py-12">
        <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
          <h3 className="text-lg font-semibold">Built for Learning</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            MockHub is a teaching platform designed for undergraduate AI students. It demonstrates
            modern web development patterns including React 19, TypeScript, Spring Boot, and AI
            integration with real-world architecture decisions.
          </p>
        </div>
      </section>
    </div>
  );
}
