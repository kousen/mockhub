import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { ArrowUpDown } from 'lucide-react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination';
import { EventGrid } from '@/components/events/EventGrid';
import { EventSearch } from '@/components/events/EventSearch';
import { EventFilters } from '@/components/events/EventFilters';
import { CategoryNav } from '@/components/events/CategoryNav';
import { useEvents } from '@/hooks/use-events';
import type { EventSearchParams } from '@/types/event';

type SortOption = 'date' | 'price_asc' | 'price_desc' | 'name' | 'popularity';

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'date', label: 'Date (Soonest)' },
  { value: 'price_asc', label: 'Price (Low to High)' },
  { value: 'price_desc', label: 'Price (High to Low)' },
  { value: 'name', label: 'Name (A-Z)' },
  { value: 'popularity', label: 'Popularity' },
];

/**
 * Parses URL search params into an EventSearchParams object.
 */
function parseSearchParams(searchParams: URLSearchParams): EventSearchParams {
  const params: EventSearchParams = {};

  const q = searchParams.get('q');
  if (q) params.q = q;

  const category = searchParams.get('category');
  if (category) params.category = category;

  const tags = searchParams.get('tags');
  if (tags) params.tags = tags;

  const city = searchParams.get('city');
  if (city) params.city = city;

  const dateFrom = searchParams.get('dateFrom');
  if (dateFrom) params.dateFrom = dateFrom;

  const dateTo = searchParams.get('dateTo');
  if (dateTo) params.dateTo = dateTo;

  const minPrice = searchParams.get('minPrice');
  if (minPrice) params.minPrice = Number(minPrice);

  const maxPrice = searchParams.get('maxPrice');
  if (maxPrice) params.maxPrice = Number(maxPrice);

  const sort = searchParams.get('sort') as SortOption | null;
  if (sort) params.sort = sort;

  const page = searchParams.get('page');
  if (page) params.page = Number(page);

  const size = searchParams.get('size');
  if (size) params.size = Number(size);

  return params;
}

/**
 * Converts EventSearchParams to URLSearchParams, omitting empty values.
 */
function toSearchParams(params: EventSearchParams): URLSearchParams {
  const result = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      result.set(key, String(value));
    }
  }
  return result;
}

/**
 * Full-featured event browsing page with search, filters, sorting, and pagination.
 * Filter state is synced to URL params for shareable links.
 */
export function EventListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parseSearchParams(searchParams), [searchParams]);

  const { data, isLoading } = useEvents(filters);

  const setFilters = useCallback(
    (newFilters: EventSearchParams) => {
      setSearchParams(toSearchParams(newFilters));
    },
    [setSearchParams],
  );

  const handleSearchChange = useCallback(
    (q: string) => {
      const next = { ...filters };
      if (q) {
        next.q = q;
      } else {
        delete next.q;
      }
      delete next.page;
      setFilters(next);
    },
    [filters, setFilters],
  );

  const handleCategoryChange = useCallback(
    (category: string | undefined) => {
      const next = { ...filters };
      if (category) {
        next.category = category;
      } else {
        delete next.category;
      }
      delete next.page;
      setFilters(next);
    },
    [filters, setFilters],
  );

  const handleSortChange = useCallback(
    (sort: string) => {
      const next = { ...filters, sort: sort as SortOption };
      delete next.page;
      setFilters(next);
    },
    [filters, setFilters],
  );

  const handlePageChange = useCallback(
    (page: number) => {
      setFilters({ ...filters, page });
    },
    [filters, setFilters],
  );

  const currentPage = filters.page ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Generate page numbers to display
  const pageNumbers = useMemo(() => {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    const end = Math.min(totalPages, start + maxVisible);
    start = Math.max(0, end - maxVisible);
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }, [currentPage, totalPages]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Search Bar */}
      <div className="mb-6">
        <h1 className="mb-4 text-2xl font-bold tracking-tight sm:text-3xl">Browse Events</h1>
        <EventSearch value={filters.q ?? ''} onChange={handleSearchChange} />
      </div>

      {/* Category Navigation */}
      <div className="mb-6">
        <CategoryNav activeCategory={filters.category} onCategoryChange={handleCategoryChange} />
      </div>

      {/* Main Content: Filters + Grid */}
      <div className="flex gap-8">
        {/* Filters */}
        <EventFilters filters={filters} onFiltersChange={setFilters} />

        {/* Event Grid + Sort + Pagination */}
        <div className="min-w-0 flex-1">
          {/* Sort Bar */}
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-muted-foreground">
              {data?.totalElements !== undefined
                ? `${data.totalElements} event${data.totalElements !== 1 ? 's' : ''} found`
                : isLoading
                  ? 'Loading...'
                  : ''}
            </p>
            <div className="flex items-center gap-2">
              <ArrowUpDown className="h-4 w-4 shrink-0 text-muted-foreground" />
              <Select value={filters.sort ?? 'date'} onValueChange={handleSortChange}>
                <SelectTrigger className="w-full sm:w-[180px]" aria-label="Sort by">
                  <SelectValue placeholder="Sort by" />
                </SelectTrigger>
                <SelectContent>
                  {SORT_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Grid */}
          <EventGrid events={data?.content ?? []} isLoading={isLoading} />

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-8">
              <Pagination>
                <PaginationContent>
                  <PaginationItem>
                    <PaginationPrevious
                      onClick={() => handlePageChange(Math.max(0, currentPage - 1))}
                      aria-disabled={currentPage === 0}
                      className={
                        currentPage === 0
                          ? 'pointer-events-none text-muted-foreground'
                          : 'cursor-pointer'
                      }
                    />
                  </PaginationItem>
                  {pageNumbers.map((pageNum) => (
                    <PaginationItem key={pageNum}>
                      <PaginationLink
                        onClick={() => handlePageChange(pageNum)}
                        isActive={pageNum === currentPage}
                        className="cursor-pointer"
                      >
                        {pageNum + 1}
                      </PaginationLink>
                    </PaginationItem>
                  ))}
                  <PaginationItem>
                    <PaginationNext
                      onClick={() => handlePageChange(Math.min(totalPages - 1, currentPage + 1))}
                      aria-disabled={currentPage >= totalPages - 1}
                      className={
                        currentPage >= totalPages - 1
                          ? 'pointer-events-none text-muted-foreground'
                          : 'cursor-pointer'
                      }
                    />
                  </PaginationItem>
                </PaginationContent>
              </Pagination>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
