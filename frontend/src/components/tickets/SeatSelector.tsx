import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { formatCurrency } from '@/lib/formatters';
import type { SectionAvailability } from '@/types/ticket';

interface SeatSelectorProps {
  sections: SectionAvailability[];
  isLoading?: boolean;
  selectedSectionId?: number | null;
  onSectionSelect?: (sectionId: number | null) => void;
}

/**
 * Visual section selector showing cards for each venue section.
 * Each card displays section name, availability count, price range,
 * and a color indicator. Clicking a section selects it, which
 * can be used to filter the ticket list.
 */
export function SeatSelector({
  sections,
  isLoading,
  selectedSectionId,
  onSectionSelect,
}: SeatSelectorProps) {
  const [localSelected, setLocalSelected] = useState<number | null>(null);

  const selected = selectedSectionId !== undefined ? selectedSectionId : localSelected;
  const handleSelect = onSectionSelect ?? setLocalSelected;

  if (isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, index) => (
          <Skeleton key={index} className="h-24 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (sections.length === 0) {
    return (
      <div className="flex min-h-[200px] flex-col items-center justify-center rounded-lg border border-dashed p-8 text-center">
        <p className="text-lg font-medium text-muted-foreground">No sections available</p>
      </div>
    );
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {sections.map((section) => {
        const isSelected = selected === section.id;
        const isAvailable = section.availableCount > 0;

        return (
          <Card
            key={section.id}
            className={cn(
              'cursor-pointer transition-all',
              isSelected && 'ring-2 ring-primary',
              !isAvailable && 'opacity-50',
            )}
            onClick={() => {
              if (isAvailable) {
                handleSelect(isSelected ? null : section.id);
              }
            }}
          >
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                {section.colorHex && (
                  <div
                    className="h-3 w-3 rounded-full"
                    style={{ backgroundColor: section.colorHex }}
                  />
                )}
                <CardTitle className="text-sm">{section.name}</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between text-xs">
                <Badge variant={isAvailable ? 'secondary' : 'outline'} className="text-xs">
                  {section.availableCount} ticket{section.availableCount !== 1 ? 's' : ''}
                </Badge>
                {section.minPrice !== null ? (
                  <span className="font-medium text-sm">
                    {formatCurrency(section.minPrice)}
                    {section.maxPrice !== null && section.maxPrice !== section.minPrice ? `+` : ''}
                  </span>
                ) : (
                  <span className="text-muted-foreground">-</span>
                )}
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
