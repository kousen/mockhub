import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { formatCurrency } from '@/lib/formatters';
import type { SectionAvailability } from '@/types/ticket';

interface TicketGridViewProps {
  sections: SectionAvailability[];
  isLoading?: boolean;
  onSectionClick?: (sectionId: number) => void;
}

/**
 * Grid of section cards showing section name, type, availability count,
 * and price range. Grouped visually by section type.
 */
export function TicketGridView({
  sections,
  isLoading,
  onSectionClick,
}: Readonly<TicketGridViewProps>) {
  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, index) => (
          <Skeleton key={`skeleton-${index}`} className="h-32 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (sections.length === 0) {
    return (
      <div className="flex min-h-[200px] flex-col items-center justify-center rounded-lg border border-dashed p-8 text-center">
        <p className="text-lg font-medium text-muted-foreground">No section info available</p>
      </div>
    );
  }

  // Group sections by sectionType
  const grouped = sections.reduce<Record<string, SectionAvailability[]>>((acc, section) => {
    const key = section.sectionType;
    if (!acc[key]) {
      acc[key] = [];
    }
    acc[key].push(section);
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      {Object.entries(grouped).map(([sectionType, sectionList]) => (
        <div key={sectionType}>
          <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
            {sectionType}
          </h3>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {sectionList.map((section) => (
              <Card
                key={section.sectionId}
                className={`transition-shadow hover:shadow-md ${onSectionClick ? 'cursor-pointer' : ''}`}
                onClick={() => onSectionClick?.(section.sectionId)}
              >
                <CardHeader className="pb-2">
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-base">{section.sectionName}</CardTitle>
                    {section.colorHex && (
                      <div
                        className="h-4 w-4 rounded-full border"
                        style={{ backgroundColor: section.colorHex }}
                        aria-label={`Section color: ${section.colorHex}`}
                      />
                    )}
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <Badge
                      variant={section.availableTickets > 0 ? 'secondary' : 'outline'}
                      className="text-xs"
                    >
                      {section.availableTickets} available
                    </Badge>
                    <div className="text-right text-sm">
                      {(() => {
                        if (section.minPrice === null || section.maxPrice === null) {
                          return <span className="text-muted-foreground">-</span>;
                        }
                        if (section.minPrice === section.maxPrice) {
                          return (
                            <span className="font-medium">{formatCurrency(section.minPrice)}</span>
                          );
                        }
                        return (
                          <span className="font-medium">
                            {formatCurrency(section.minPrice)} - {formatCurrency(section.maxPrice)}
                          </span>
                        );
                      })()}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
