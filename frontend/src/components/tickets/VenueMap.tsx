import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { formatCurrency } from '@/lib/formatters';
import type { SectionAvailability } from '@/types/ticket';
import { SeatSelector } from './SeatSelector';

interface VenueMapProps {
  sections: SectionAvailability[];
  selectedSectionId: number | null;
  onSectionSelect: (sectionId: number | null) => void;
  isLoading?: boolean;
}

export function VenueMap({
  sections,
  selectedSectionId,
  onSectionSelect,
  isLoading = false,
}: VenueMapProps) {
  if (isLoading) {
    return (
      <div className="mx-auto w-full max-w-2xl">
        <Skeleton className="aspect-[3/2] w-full rounded-lg" />
      </div>
    );
  }

  const hasSvgData = sections.some((section) => section.svgX !== null);

  if (!hasSvgData) {
    return (
      <SeatSelector
        sections={sections}
        selectedSectionId={selectedSectionId}
        onSectionSelect={onSectionSelect}
      />
    );
  }

  function handleSectionClick(section: SectionAvailability): void {
    if (section.availableTickets === 0) return;
    onSectionSelect(selectedSectionId === section.sectionId ? null : section.sectionId);
  }

  function handleKeyDown(event: React.KeyboardEvent, section: SectionAvailability): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleSectionClick(section);
    }
  }

  function buildAriaLabel(section: SectionAvailability): string {
    const parts = [`${section.sectionName} section`];
    parts.push(
      `${section.availableTickets} ticket${section.availableTickets !== 1 ? 's' : ''} available`,
    );
    if (section.minPrice !== null) {
      parts.push(`from ${formatCurrency(section.minPrice)}`);
    }
    return parts.join(', ');
  }

  function buildSubLabel(section: SectionAvailability): string {
    const count = `${section.availableTickets} ticket${section.availableTickets !== 1 ? 's' : ''}`;
    if (section.minPrice !== null) {
      return `${count} from ${formatCurrency(section.minPrice)}`;
    }
    return count;
  }

  return (
    <div className="mx-auto w-full max-w-2xl">
      <svg viewBox="0 0 600 400" role="img" aria-label="Venue seating map" className="w-full">
        {/* Stage indicator */}
        <rect x={200} y={5} width={200} height={30} rx={4} fill="#374151" />
        <text x={300} y={25} textAnchor="middle" fill="#e5e7eb" fontSize={14} fontWeight="bold">
          STAGE
        </text>

        {/* Section rectangles */}
        {sections.map((section) => {
          if (
            section.svgX === null ||
            section.svgY === null ||
            section.svgWidth === null ||
            section.svgHeight === null
          ) {
            return null;
          }

          const isAvailable = section.availableTickets > 0;
          const isSelected = selectedSectionId === section.sectionId;
          const fillColor = section.colorHex ?? '#6B7280';
          const centerX = section.svgX + section.svgWidth / 2;
          const centerY = section.svgY + section.svgHeight / 2;

          return (
            <g
              key={section.sectionId}
              role="button"
              tabIndex={isAvailable ? 0 : -1}
              aria-label={buildAriaLabel(section)}
              onClick={() => handleSectionClick(section)}
              onKeyDown={(event) => handleKeyDown(event, section)}
              className={cn(
                isAvailable ? 'cursor-pointer' : 'cursor-default',
                isAvailable && 'hover:opacity-90',
              )}
            >
              <rect
                x={section.svgX}
                y={section.svgY}
                width={section.svgWidth}
                height={section.svgHeight}
                rx={6}
                fill={fillColor}
                opacity={isAvailable ? 0.85 : 0.3}
                stroke={isSelected ? '#2563eb' : 'none'}
                strokeWidth={isSelected ? 3 : 0}
              />
              <text
                x={centerX}
                y={centerY - 8}
                textAnchor="middle"
                dominantBaseline="middle"
                fill="#1a1a1a"
                fontSize={14}
                fontWeight="bold"
              >
                {section.sectionName}
              </text>
              <text
                x={centerX}
                y={centerY + 10}
                textAnchor="middle"
                dominantBaseline="middle"
                fill="#1a1a1a"
                fontSize={11}
              >
                {buildSubLabel(section)}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}
