import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';

interface PaginationState {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  isFirst: boolean;
  isLast: boolean;
  goToPage: (page: number) => void;
  nextPage: () => void;
  prevPage: () => void;
  setPageSize: (size: number) => void;
}

/**
 * Custom hook for managing pagination state synchronized with URL search params.
 * This ensures that pagination state is shareable via URL.
 */
export function usePagination(
  totalPages: number = 0,
  totalElements: number = 0,
  defaultSize: number = 20,
): PaginationState {
  const [searchParams, setSearchParams] = useSearchParams();

  const page = useMemo(() => {
    const pageParam = searchParams.get('page');
    const parsed = pageParam ? Number.parseInt(pageParam, 10) : 0;
    return Number.isNaN(parsed) || parsed < 0 ? 0 : parsed;
  }, [searchParams]);

  const size = useMemo(() => {
    const sizeParam = searchParams.get('size');
    const parsed = sizeParam ? Number.parseInt(sizeParam, 10) : defaultSize;
    return Number.isNaN(parsed) || parsed < 1 ? defaultSize : parsed;
  }, [searchParams, defaultSize]);

  const goToPage = useCallback(
    (newPage: number) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (newPage === 0) {
          next.delete('page');
        } else {
          next.set('page', String(newPage));
        }
        return next;
      });
    },
    [setSearchParams],
  );

  const nextPage = useCallback(() => {
    if (page < totalPages - 1) {
      goToPage(page + 1);
    }
  }, [page, totalPages, goToPage]);

  const prevPage = useCallback(() => {
    if (page > 0) {
      goToPage(page - 1);
    }
  }, [page, goToPage]);

  const setPageSize = useCallback(
    (newSize: number) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set('size', String(newSize));
        next.delete('page'); // Reset to first page when changing size
        return next;
      });
    },
    [setSearchParams],
  );

  return {
    page,
    size,
    totalPages,
    totalElements,
    isFirst: page === 0,
    isLast: page >= totalPages - 1,
    goToPage,
    nextPage,
    prevPage,
    setPageSize,
  };
}
