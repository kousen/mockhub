import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import type { ReactNode } from 'react';
import { usePagination } from './use-pagination';

function createWrapper(route: string = '/') {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>;
  };
}

describe('usePagination', () => {
  it('returns default page 0 and default size when no URL params', () => {
    const { result } = renderHook(() => usePagination(5, 100, 20), {
      wrapper: createWrapper('/'),
    });
    expect(result.current.page).toBe(0);
    expect(result.current.size).toBe(20);
    expect(result.current.totalPages).toBe(5);
    expect(result.current.totalElements).toBe(100);
    expect(result.current.isFirst).toBe(true);
    expect(result.current.isLast).toBe(false);
  });

  it('parses page from URL search params', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/?page=2'),
    });
    expect(result.current.page).toBe(2);
    expect(result.current.isFirst).toBe(false);
  });

  it('parses size from URL search params', () => {
    const { result } = renderHook(() => usePagination(5, 100, 20), {
      wrapper: createWrapper('/?size=10'),
    });
    expect(result.current.size).toBe(10);
  });

  it('defaults page to 0 for invalid page param', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/?page=abc'),
    });
    expect(result.current.page).toBe(0);
  });

  it('defaults page to 0 for negative page param', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/?page=-1'),
    });
    expect(result.current.page).toBe(0);
  });

  it('defaults size to defaultSize for invalid size param', () => {
    const { result } = renderHook(() => usePagination(5, 100, 25), {
      wrapper: createWrapper('/?size=abc'),
    });
    expect(result.current.size).toBe(25);
  });

  it('defaults size to defaultSize for zero size param', () => {
    const { result } = renderHook(() => usePagination(5, 100, 20), {
      wrapper: createWrapper('/?size=0'),
    });
    expect(result.current.size).toBe(20);
  });

  it('isLast is true when on last page', () => {
    const { result } = renderHook(() => usePagination(3, 60), {
      wrapper: createWrapper('/?page=2'),
    });
    expect(result.current.isLast).toBe(true);
  });

  it('goToPage updates page', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/'),
    });

    act(() => {
      result.current.goToPage(3);
    });
    expect(result.current.page).toBe(3);
  });

  it('nextPage increments page', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/?page=1'),
    });

    act(() => {
      result.current.nextPage();
    });
    expect(result.current.page).toBe(2);
  });

  it('nextPage does not go past last page', () => {
    const { result } = renderHook(() => usePagination(3, 60), {
      wrapper: createWrapper('/?page=2'),
    });

    act(() => {
      result.current.nextPage();
    });
    expect(result.current.page).toBe(2);
  });

  it('prevPage decrements page', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/?page=2'),
    });

    act(() => {
      result.current.prevPage();
    });
    expect(result.current.page).toBe(1);
  });

  it('prevPage does not go below 0', () => {
    const { result } = renderHook(() => usePagination(5, 100), {
      wrapper: createWrapper('/'),
    });

    act(() => {
      result.current.prevPage();
    });
    expect(result.current.page).toBe(0);
  });

  it('setPageSize updates size and resets page to 0', () => {
    const { result } = renderHook(() => usePagination(5, 100, 20), {
      wrapper: createWrapper('/?page=3&size=20'),
    });

    act(() => {
      result.current.setPageSize(50);
    });
    expect(result.current.size).toBe(50);
    expect(result.current.page).toBe(0);
  });
});
