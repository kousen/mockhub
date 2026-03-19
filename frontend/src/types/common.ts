export interface ApiError {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  fieldErrors?: Record<string, string>;
  // Backwards compatibility alias
  message?: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
