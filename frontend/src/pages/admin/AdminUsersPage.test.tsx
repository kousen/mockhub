import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { AdminUsersPage } from './AdminUsersPage';

const mockUsers = {
  content: [
    {
      id: 1,
      email: 'admin@mockhub.com',
      firstName: 'Alice',
      lastName: 'Admin',
      roles: ['ROLE_USER', 'ROLE_ADMIN'],
      enabled: true,
      createdAt: '2026-01-01T00:00:00Z',
      orderCount: 5,
    },
    {
      id: 2,
      email: 'user@mockhub.com',
      firstName: 'Bob',
      lastName: 'User',
      roles: ['ROLE_USER'],
      enabled: false,
      createdAt: '2026-02-15T12:00:00Z',
      orderCount: 0,
    },
  ],
  totalPages: 1,
  number: 0,
  first: true,
  last: true,
};

const mockUpdateRolesMutate = vi.fn();
const mockUpdateStatusMutate = vi.fn();

let usersReturn = { data: mockUsers, isLoading: false };

vi.mock('@/hooks/use-admin', () => ({
  useAdminUsers: () => usersReturn,
  useUpdateUserRoles: () => ({
    mutate: mockUpdateRolesMutate,
  }),
  useUpdateUserStatus: () => ({
    mutate: mockUpdateStatusMutate,
  }),
}));

describe('AdminUsersPage', () => {
  it('renders loading skeletons when loading', () => {
    usersReturn = { data: undefined as never, isLoading: true };

    renderWithProviders(<AdminUsersPage />);

    expect(screen.getByText('Users')).toBeDefined();
  });

  it('renders users list with data', () => {
    usersReturn = { data: mockUsers, isLoading: false };

    renderWithProviders(<AdminUsersPage />);

    expect(screen.getByText('Alice Admin')).toBeDefined();
    expect(screen.getByText('admin@mockhub.com')).toBeDefined();
    // ADMIN text appears in both badge and select value
    const adminTexts = screen.getAllByText('ADMIN');
    expect(adminTexts.length).toBeGreaterThanOrEqual(1);

    expect(screen.getByText('Bob User')).toBeDefined();
    expect(screen.getByText('user@mockhub.com')).toBeDefined();
  });

  it('renders role badges for users', () => {
    usersReturn = { data: mockUsers, isLoading: false };

    renderWithProviders(<AdminUsersPage />);

    // Alice has both USER and ADMIN badges
    const userBadges = screen.getAllByText('USER');
    expect(userBadges.length).toBeGreaterThanOrEqual(2);
    // ADMIN appears in both badge and select, so use getAllByText
    const adminTexts = screen.getAllByText('ADMIN');
    expect(adminTexts.length).toBeGreaterThanOrEqual(1);
  });

  it('renders enable/disable switches with aria labels', () => {
    usersReturn = { data: mockUsers, isLoading: false };

    renderWithProviders(<AdminUsersPage />);

    expect(screen.getByLabelText('Disable Alice Admin')).toBeDefined();
    expect(screen.getByLabelText('Enable Bob User')).toBeDefined();
  });

  it('renders empty state when no users', () => {
    usersReturn = {
      data: { ...mockUsers, content: [] },
      isLoading: false,
    };

    renderWithProviders(<AdminUsersPage />);

    expect(screen.getByText('No users found.')).toBeDefined();
  });
});
