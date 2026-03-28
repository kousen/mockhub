import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { exchangeOAuth2Code } from '@/api/auth';
import { useAuthStore } from '@/stores/auth-store';
import { ROUTES } from '@/lib/constants';

export function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const code = searchParams.get('code');
    const errorParam = searchParams.get('error');

    if (errorParam) {
      setError(
        errorParam === 'oauth_no_email'
          ? 'Could not retrieve your email from the provider. Please try a different sign-in method.'
          : 'Authentication failed. Please try again.',
      );
      return;
    }

    if (!code) {
      setError('No authentication code received.');
      return;
    }

    exchangeOAuth2Code(code)
      .then((response) => {
        login(response.accessToken, response.user);
        navigate(ROUTES.HOME);
      })
      .catch(() => {
        setError('Failed to complete sign-in. The code may have expired. Please try again.');
      });
  }, [searchParams, login, navigate]);

  if (error) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold">Sign-in Error</h1>
          <p className="mt-2 text-muted-foreground">{error}</p>
          <Button className="mt-4" onClick={() => navigate(ROUTES.LOGIN)}>
            Back to Login
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
      <div className="text-center">
        <Loader2 className="mx-auto h-8 w-8 animate-spin text-primary" />
        <p className="mt-4 text-muted-foreground">Completing sign-in...</p>
      </div>
    </div>
  );
}
