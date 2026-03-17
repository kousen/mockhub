import { useCategories } from '@/hooks/use-events';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

interface CategoryNavProps {
  activeCategory?: string;
  onCategoryChange: (category: string | undefined) => void;
}

/**
 * Horizontal scrollable row of category chips/badges.
 * Highlights the active category. Clicking a category filters events.
 * Clicking the active category again deselects it.
 */
export function CategoryNav({ activeCategory, onCategoryChange }: CategoryNavProps) {
  const { data: categories, isLoading } = useCategories();

  if (isLoading) {
    return (
      <div className="flex gap-2 overflow-x-auto pb-2">
        {Array.from({ length: 6 }).map((_, index) => (
          <Skeleton key={index} className="h-7 w-20 shrink-0 rounded-full" />
        ))}
      </div>
    );
  }

  if (!categories || categories.length === 0) {
    return null;
  }

  return (
    <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-none">
      <Badge
        variant={activeCategory === undefined ? 'default' : 'outline'}
        className="shrink-0 cursor-pointer"
        onClick={() => onCategoryChange(undefined)}
      >
        All
      </Badge>
      {categories.map((category) => (
        <Badge
          key={category.id}
          variant={activeCategory === category.slug ? 'default' : 'outline'}
          className="shrink-0 cursor-pointer"
          onClick={() =>
            onCategoryChange(activeCategory === category.slug ? undefined : category.slug)
          }
        >
          {category.name}
        </Badge>
      ))}
    </div>
  );
}
