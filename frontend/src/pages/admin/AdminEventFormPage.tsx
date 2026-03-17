import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  useAdminEvent,
  useCreateEvent,
  useUpdateEvent,
} from '@/hooks/use-admin';
import { useCategories } from '@/hooks/use-events';
import { ROUTES } from '@/lib/constants';
import type { CreateEventRequest, UpdateEventRequest } from '@/types/admin';

interface FormState {
  name: string;
  artistName: string;
  venueId: string;
  categoryId: string;
  eventDate: string;
  doorsOpenAt: string;
  basePrice: string;
  description: string;
}

const emptyForm: FormState = {
  name: '',
  artistName: '',
  venueId: '',
  categoryId: '',
  eventDate: '',
  doorsOpenAt: '',
  basePrice: '',
  description: '',
};

/**
 * Form for creating or editing an event. When an :id param is present,
 * the form loads existing data for editing.
 */
export function AdminEventFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = Boolean(id);
  const eventId = id ? parseInt(id, 10) : 0;

  const { data: existingEvent, isLoading: eventLoading } =
    useAdminEvent(eventId);
  const { data: categories, isLoading: categoriesLoading } = useCategories();
  const createEvent = useCreateEvent();
  const updateEvent = useUpdateEvent(eventId);

  const [form, setForm] = useState<FormState>(emptyForm);

  useEffect(() => {
    if (isEditing && existingEvent) {
      setForm({
        name: existingEvent.name,
        artistName: existingEvent.artistName ?? '',
        venueId: existingEvent.venue.id.toString(),
        categoryId: existingEvent.category.id.toString(),
        eventDate: existingEvent.eventDate.slice(0, 16),
        doorsOpenAt: existingEvent.doorsOpenAt
          ? existingEvent.doorsOpenAt.slice(0, 16)
          : '',
        basePrice: existingEvent.basePrice.toString(),
        description: existingEvent.description ?? '',
      });
    }
  }, [isEditing, existingEvent]);

  const updateField = (field: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (isEditing) {
      const data: UpdateEventRequest = {
        name: form.name,
        artistName: form.artistName || null,
        venueId: parseInt(form.venueId, 10),
        categoryId: parseInt(form.categoryId, 10),
        eventDate: form.eventDate,
        doorsOpenAt: form.doorsOpenAt || null,
        basePrice: parseFloat(form.basePrice),
        description: form.description || null,
      };
      updateEvent.mutate(data, {
        onSuccess: () => navigate(ROUTES.ADMIN_EVENTS),
      });
    } else {
      const data: CreateEventRequest = {
        name: form.name,
        artistName: form.artistName || null,
        venueId: parseInt(form.venueId, 10),
        categoryId: parseInt(form.categoryId, 10),
        eventDate: form.eventDate,
        doorsOpenAt: form.doorsOpenAt || null,
        basePrice: parseFloat(form.basePrice),
        description: form.description || null,
      };
      createEvent.mutate(data, {
        onSuccess: () => navigate(ROUTES.ADMIN_EVENTS),
      });
    }
  };

  const isSubmitting = createEvent.isPending || updateEvent.isPending;

  if (isEditing && eventLoading) {
    return <Skeleton className="h-96 rounded-lg" />;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">
        {isEditing ? 'Edit Event' : 'Create Event'}
      </h1>

      <Card>
        <CardHeader>
          <CardTitle>Event Details</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid gap-6 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="name">Event Name *</Label>
                <Input
                  id="name"
                  value={form.name}
                  onChange={(e) => updateField('name', e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="artistName">Artist Name</Label>
                <Input
                  id="artistName"
                  value={form.artistName}
                  onChange={(e) => updateField('artistName', e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="venueId">Venue *</Label>
                <Input
                  id="venueId"
                  type="number"
                  placeholder="Venue ID"
                  value={form.venueId}
                  onChange={(e) => updateField('venueId', e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="categoryId">Category *</Label>
                {categoriesLoading ? (
                  <Skeleton className="h-10 w-full" />
                ) : (
                  <Select
                    value={form.categoryId}
                    onValueChange={(val) => updateField('categoryId', val)}
                  >
                    <SelectTrigger id="categoryId">
                      <SelectValue placeholder="Select a category" />
                    </SelectTrigger>
                    <SelectContent>
                      {categories?.map((cat) => (
                        <SelectItem key={cat.id} value={cat.id.toString()}>
                          {cat.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="eventDate">Event Date & Time *</Label>
                <Input
                  id="eventDate"
                  type="datetime-local"
                  value={form.eventDate}
                  onChange={(e) => updateField('eventDate', e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="doorsOpenAt">Doors Open At</Label>
                <Input
                  id="doorsOpenAt"
                  type="datetime-local"
                  value={form.doorsOpenAt}
                  onChange={(e) => updateField('doorsOpenAt', e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="basePrice">Base Price (USD) *</Label>
                <Input
                  id="basePrice"
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.basePrice}
                  onChange={(e) => updateField('basePrice', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                rows={4}
                value={form.description}
                onChange={(e) => updateField('description', e.target.value)}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>

            <div className="flex gap-3">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? 'Saving...'
                  : isEditing
                    ? 'Update Event'
                    : 'Create Event'}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(ROUTES.ADMIN_EVENTS)}
              >
                Cancel
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
