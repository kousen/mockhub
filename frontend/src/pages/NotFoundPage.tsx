import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { ROUTES } from '@/lib/constants';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[calc(100vh-10rem)] flex-col items-center justify-center px-4 text-center">
      <h1 className="text-6xl font-bold text-primary">404</h1>
      <h2 className="mt-4 text-2xl font-semibold">Page Not Found</h2>
      <p className="mt-2 text-muted-foreground">
        The page you are looking for does not exist or has been moved.
      </p>
      <Button className="mt-6" asChild>
        <Link to={ROUTES.HOME}>Back to Home</Link>
      </Button>
    </div>
  );
}
