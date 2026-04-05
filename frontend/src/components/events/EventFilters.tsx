import { useCallback } from 'react';
import { Filter, X } from 'lucide-react';
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
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { useCategories, useTags } from '@/hooks/use-events';
import type { EventSearchParams } from '@/types/event';

interface EventFiltersProps {
  filters: EventSearchParams;
  onFiltersChange: (filters: EventSearchParams) => void;
}

function FilterControls({ filters, onFiltersChange }: Readonly<EventFiltersProps>) {
  const { data: categories } = useCategories();
  const { data: tags } = useTags();

  const updateFilter = useCallback(
    (key: keyof EventSearchParams, value: string | number | undefined) => {
      const next = { ...filters };
      if (value === undefined || value === '' || value === 'all') {
        delete next[key];
      } else {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (next as Record<string, any>)[key] = value;
      }
      // Reset page when filters change
      delete next.page;
      onFiltersChange(next);
    },
    [filters, onFiltersChange],
  );

  const clearAll = useCallback(() => {
    onFiltersChange({
      sort: filters.sort,
      size: filters.size,
    });
  }, [filters.sort, filters.size, onFiltersChange]);

  const activeFilterCount = [
    filters.category,
    filters.tags,
    filters.city,
    filters.dateFrom,
    filters.dateTo,
    filters.minPrice,
    filters.maxPrice,
  ].filter(Boolean).length;

  const handleTagToggle = useCallback(
    (tagSlug: string) => {
      const currentTags = filters.tags ? filters.tags.split(',') : [];
      const newTags = currentTags.includes(tagSlug)
        ? currentTags.filter((t) => t !== tagSlug)
        : [...currentTags, tagSlug];
      updateFilter('tags', newTags.length > 0 ? newTags.join(',') : undefined);
    },
    [filters.tags, updateFilter],
  );

  const selectedTags = filters.tags ? filters.tags.split(',') : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">Filters</h3>
        {activeFilterCount > 0 && (
          <Button variant="ghost" size="xs" onClick={clearAll}>
            Clear All ({activeFilterCount})
          </Button>
        )}
      </div>

      <Separator />

      {/* Category */}
      <div className="space-y-2">
        <Label htmlFor="category-filter">Category</Label>
        <Select
          value={filters.category ?? 'all'}
          onValueChange={(value) => updateFilter('category', value)}
        >
          <SelectTrigger id="category-filter">
            <SelectValue placeholder="All Categories" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Categories</SelectItem>
            {categories?.map((category) => (
              <SelectItem key={category.id} value={category.slug}>
                {category.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Date Range */}
      <div className="space-y-2">
        <Label>Date Range</Label>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <Label htmlFor="date-from" className="text-xs text-muted-foreground">
              From
            </Label>
            <Input
              id="date-from"
              type="date"
              value={filters.dateFrom ?? ''}
              onChange={(e) => updateFilter('dateFrom', e.target.value || undefined)}
            />
          </div>
          <div>
            <Label htmlFor="date-to" className="text-xs text-muted-foreground">
              To
            </Label>
            <Input
              id="date-to"
              type="date"
              value={filters.dateTo ?? ''}
              onChange={(e) => updateFilter('dateTo', e.target.value || undefined)}
            />
          </div>
        </div>
      </div>

      {/* Price Range */}
      <div className="space-y-2">
        <Label>Price Range</Label>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <Label htmlFor="min-price" className="text-xs text-muted-foreground">
              Min ($)
            </Label>
            <Input
              id="min-price"
              type="number"
              min={0}
              placeholder="0"
              value={filters.minPrice ?? ''}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                updateFilter('minPrice', val);
              }}
            />
          </div>
          <div>
            <Label htmlFor="max-price" className="text-xs text-muted-foreground">
              Max ($)
            </Label>
            <Input
              id="max-price"
              type="number"
              min={0}
              placeholder="Any"
              value={filters.maxPrice ?? ''}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                updateFilter('maxPrice', val);
              }}
            />
          </div>
        </div>
      </div>

      {/* City */}
      <div className="space-y-2">
        <Label htmlFor="city-filter">City</Label>
        <Input
          id="city-filter"
          type="text"
          placeholder="Any city"
          value={filters.city ?? ''}
          onChange={(e) => updateFilter('city', e.target.value || undefined)}
        />
      </div>

      {/* Tags */}
      {tags && tags.length > 0 && (
        <div className="space-y-2">
          <Label>Tags</Label>
          <div className="flex flex-wrap gap-1.5">
            {tags.map((tag) => {
              const isSelected = selectedTags.includes(tag.slug);
              return (
                <Badge
                  key={tag.id}
                  variant={isSelected ? 'default' : 'outline'}
                  className="cursor-pointer"
                  onClick={() => handleTagToggle(tag.slug)}
                >
                  {tag.name}
                  {isSelected && <X className="ml-1 h-3 w-3" />}
                </Badge>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Event filters component.
 * On mobile: rendered inside a Sheet (slide-out drawer).
 * On desktop (lg+): rendered as a sidebar panel.
 */
export function EventFilters({ filters, onFiltersChange }: Readonly<EventFiltersProps>) {
  const activeFilterCount = [
    filters.category,
    filters.tags,
    filters.city,
    filters.dateFrom,
    filters.dateTo,
    filters.minPrice,
    filters.maxPrice,
  ].filter(Boolean).length;

  return (
    <>
      {/* Mobile: Sheet trigger */}
      <div className="lg:hidden">
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="outline" size="sm">
              <Filter className="mr-2 h-4 w-4" />
              Filters
              {activeFilterCount > 0 && (
                <Badge variant="secondary" className="ml-1">
                  {activeFilterCount}
                </Badge>
              )}
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-80 overflow-y-auto">
            <SheetHeader>
              <SheetTitle>Filter Events</SheetTitle>
              <SheetDescription>Narrow down your event search.</SheetDescription>
            </SheetHeader>
            <div className="p-4">
              <FilterControls filters={filters} onFiltersChange={onFiltersChange} />
            </div>
          </SheetContent>
        </Sheet>
      </div>

      {/* Desktop: Sidebar panel */}
      <div className="hidden w-64 shrink-0 lg:block">
        <div className="sticky top-20 rounded-lg border p-4">
          <FilterControls filters={filters} onFiltersChange={onFiltersChange} />
        </div>
      </div>
    </>
  );
}
