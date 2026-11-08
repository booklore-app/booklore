/**
 * Helpers for the "Published Year" filter.
 *
 * A selected value is stored as a small string token so it can travel through the URL and the
 * existing filter plumbing unchanged:
 *   "2020"       exactly the year 2020
 *   "1990-2000"  1990 up to and including 2000
 *   "1990-"      1990 and later
 *   "-2000"      2000 and earlier
 */

export interface YearRange {
  from: number | null;
  to: number | null;
}

/**
 * The publication year of a book. ISO dates start with the year, so it is read straight from the text;
 * going through `new Date(...)` would shift dates like "2020-01-01" into 2019 in time zones behind UTC.
 */
export function getBookPublishedYear(date: string | null | undefined): number | null {
  if (!date) return null;
  const text = date.trim();
  const leadingYear = /^(\d{4})/.exec(text);
  if (leadingYear) return Number(leadingYear[1]);
  const parsed = new Date(text);
  return isNaN(parsed.getTime()) ? null : parsed.getFullYear();
}

/** Reads one filter token. Returns null when the token is not a year or a year range. */
export function parseYearFilterValue(value: unknown): YearRange | null {
  const text = String(value ?? '').trim();

  const single = /^(\d{1,4})$/.exec(text);
  if (single) {
    const year = Number(single[1]);
    return {from: year, to: year};
  }

  const range = /^(\d{1,4})?-(\d{1,4})?$/.exec(text);
  if (range && (range[1] || range[2])) {
    return {
      from: range[1] ? Number(range[1]) : null,
      to: range[2] ? Number(range[2]) : null
    };
  }
  return null;
}

/** Builds the token for a range. Returns null when both ends are empty. Swapped ends are put in order. */
export function buildYearFilterValue(from: number | null, to: number | null): string | null {
  if (from == null && to == null) return null;
  if (from != null && to != null && from > to) {
    [from, to] = [to, from];
  }
  if (from != null && from === to) return String(from);
  return `${from ?? ''}-${to ?? ''}`;
}

/** True when the year falls inside the range or year described by the token. */
export function doesYearMatchFilterValue(year: number | null, value: unknown): boolean {
  if (year == null) return false;
  const range = parseYearFilterValue(value);
  if (!range) return false;
  return (range.from == null || year >= range.from) && (range.to == null || year <= range.to);
}

/**
 * Reads what a person typed into a year box.
 * Returns null for an empty box, undefined for something that is not a 1 to 4 digit year.
 */
export function parseYearInput(text: string | null | undefined): number | null | undefined {
  const trimmed = (text ?? '').trim();
  if (trimmed === '') return null;
  return /^\d{1,4}$/.test(trimmed) ? Number(trimmed) : undefined;
}
