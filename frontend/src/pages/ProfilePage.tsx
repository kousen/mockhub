import { useState } from 'react';
import { Mail, Phone, Save, User } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import { useAuthStore } from '@/stores/auth-store';
import { getOAuth2Url } from '@/components/auth/SocialLoginButtons';
import {
  useCurrentUser,
  useLinkedProviders,
  useUnlinkProvider,
  useUpdateProfile,
} from '@/hooks/use-auth';
import type { UserDto } from '@/types/auth';

function ProfileForm({ user }: { user: UserDto }) {
  const updateProfile = useUpdateProfile();

  const [firstName, setFirstName] = useState(user.firstName);
  const [lastName, setLastName] = useState(user.lastName);
  const [phone, setPhone] = useState(user.phone ?? '');
  const [saved, setSaved] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateProfile.mutate(
      { firstName, lastName, phone },
      {
        onSuccess: () => {
          setSaved(true);
          setTimeout(() => setSaved(false), 3000);
        },
      },
    );
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Personal Information</h2>
        <p className="text-sm text-muted-foreground">Update your name and contact details.</p>
      </div>

      <div className="space-y-4">
        <div>
          <label htmlFor="email" className="mb-1 block text-sm font-medium">
            Email
          </label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="email"
              type="email"
              value={user.email}
              disabled
              className="pl-10 opacity-60"
            />
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Email cannot be changed — it is your account identifier.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="firstName" className="mb-1 block text-sm font-medium">
              First Name
            </label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="firstName"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="pl-10"
                required
              />
            </div>
          </div>
          <div>
            <label htmlFor="lastName" className="mb-1 block text-sm font-medium">
              Last Name
            </label>
            <Input
              id="lastName"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="phone" className="mb-1 block text-sm font-medium">
            Phone Number
          </label>
          <div className="relative">
            <Phone className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="phone"
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="Optional"
              className="pl-10"
            />
          </div>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Button type="submit" disabled={updateProfile.isPending}>
          <Save className="mr-2 h-4 w-4" />
          {updateProfile.isPending ? 'Saving...' : 'Save Changes'}
        </Button>
        {saved && <span className="text-sm text-emerald-600">Profile updated!</span>}
        {updateProfile.isError && (
          <span className="text-sm text-destructive">Failed to update profile.</span>
        )}
      </div>
    </form>
  );
}

const PROVIDERS = [
  { id: 'google', name: 'Google', description: 'Sign in with your Google account' },
  { id: 'github', name: 'GitHub', description: 'Sign in with your GitHub account' },
  {
    id: 'spotify',
    name: 'Spotify',
    description: 'Connect for personalized concert recommendations based on your listening history',
  },
];

export function ProfilePage() {
  const user = useAuthStore((state) => state.user);
  useCurrentUser();
  const { data: linkedProviders } = useLinkedProviders();
  const unlinkProvider = useUnlinkProvider();

  if (!user) {
    return null;
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Profile</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Manage your account settings and connected services.
        </p>
      </div>

      <Separator className="mb-6" />

      {/* Key on user ID so form reinitializes when user data refreshes */}
      <ProfileForm key={user.id} user={user} />

      <Separator className="my-8" />

      {/* Connected Services */}
      <div>
        <h2 className="text-lg font-semibold">Connected Services</h2>
        <p className="mb-4 text-sm text-muted-foreground">
          Connect third-party accounts for personalized recommendations.
        </p>

        <div className="space-y-3">
          {PROVIDERS.map((provider) => {
            const isConnected = linkedProviders?.includes(provider.id) ?? false;
            return (
              <div
                key={provider.id}
                className="flex items-center justify-between rounded-lg border p-4"
              >
                <div>
                  <p className="font-medium">{provider.name}</p>
                  <p className="text-sm text-muted-foreground">{provider.description}</p>
                </div>
                {isConnected ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      if (window.confirm(`Disconnect ${provider.name}? You can reconnect later.`)) {
                        unlinkProvider.mutate(provider.id);
                      }
                    }}
                    disabled={unlinkProvider.isPending}
                  >
                    Disconnect
                  </Button>
                ) : (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      window.location.href = getOAuth2Url(provider.id);
                    }}
                  >
                    Connect
                  </Button>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
