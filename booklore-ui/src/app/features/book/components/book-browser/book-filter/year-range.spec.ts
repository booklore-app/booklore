import {describe, expect, it} from 'vitest';
import {
  buildYearFilterValue,
  doesYearMatchFilterValue,
  getBookPublishedYear,
  parseYearFilterValue,
  parseYearInput
} from './year-range';

describe('getBookPublishedYear', () => {
  it.each([
    ['2020-01-01', 2020],
    ['2020-12-31', 2020],
    ['1999', 1999],
    ['2005-06-15T10:00:00Z', 2005]
  ])('reads the year of %s without a time zone shift', (date, expected) => {
    expect(getBookPublishedYear(date)).toBe(expected);
  });

  it('returns null for missing or unreadable dates', () => {
    expect(getBookPublishedYear(undefined)).toBeNull();
    expect(getBookPublishedYear(null)).toBeNull();
    expect(getBookPublishedYear('')).toBeNull();
    expect(getBookPublishedYear('not a date')).toBeNull();
  });
});

describe('parseYearFilterValue', () => {
  it.each([
    ['2020', {from: 2020, to: 2020}],
    [2020, {from: 2020, to: 2020}],
    ['1990-2000', {from: 1990, to: 2000}],
    ['1990-', {from: 1990, to: null}],
    ['-2000', {from: null, to: 2000}]
  ])('parses %s', (value, expected) => {
    expect(parseYearFilterValue(value)).toEqual(expected);
  });

  it.each(['', '-', 'abc', '1990-2000-2010', '12345', '1990 2000'])('rejects %j', value => {
    expect(parseYearFilterValue(value)).toBeNull();
  });
});

describe('buildYearFilterValue', () => {
  it('builds single years, closed ranges and open ranges', () => {
    expect(buildYearFilterValue(2020, 2020)).toBe('2020');
    expect(buildYearFilterValue(1990, 2000)).toBe('1990-2000');
    expect(buildYearFilterValue(1990, null)).toBe('1990-');
    expect(buildYearFilterValue(null, 2000)).toBe('-2000');
  });

  it('puts swapped ends in order', () => {
    expect(buildYearFilterValue(2000, 1990)).toBe('1990-2000');
  });

  it('returns null when both ends are empty', () => {
    expect(buildYearFilterValue(null, null)).toBeNull();
  });

  it('round-trips through parseYearFilterValue', () => {
    for (const [from, to] of [[1990, 2000], [1990, null], [null, 2000], [2020, 2020]] as const) {
      const token = buildYearFilterValue(from, to)!;
      expect(parseYearFilterValue(token)).toEqual({from, to});
    }
  });
});

describe('doesYearMatchFilterValue', () => {
  it('includes both ends of a range', () => {
    expect(doesYearMatchFilterValue(1990, '1990-2000')).toBe(true);
    expect(doesYearMatchFilterValue(2000, '1990-2000')).toBe(true);
    expect(doesYearMatchFilterValue(1989, '1990-2000')).toBe(false);
    expect(doesYearMatchFilterValue(2001, '1990-2000')).toBe(false);
  });

  it('supports open-ended ranges', () => {
    expect(doesYearMatchFilterValue(2024, '2000-')).toBe(true);
    expect(doesYearMatchFilterValue(1999, '2000-')).toBe(false);
    expect(doesYearMatchFilterValue(1850, '-1900')).toBe(true);
    expect(doesYearMatchFilterValue(1901, '-1900')).toBe(false);
  });

  it('keeps matching a plain year, as strings or numbers', () => {
    expect(doesYearMatchFilterValue(2020, '2020')).toBe(true);
    expect(doesYearMatchFilterValue(2020, 2020)).toBe(true);
    expect(doesYearMatchFilterValue(2021, '2020')).toBe(false);
  });

  it('never matches a book without a year, or a garbage token', () => {
    expect(doesYearMatchFilterValue(null, '1990-2000')).toBe(false);
    expect(doesYearMatchFilterValue(2000, 'abc')).toBe(false);
  });
});

describe('parseYearInput', () => {
  it('distinguishes empty from invalid', () => {
    expect(parseYearInput('')).toBeNull();
    expect(parseYearInput('   ')).toBeNull();
    expect(parseYearInput(' 1999 ')).toBe(1999);
    expect(parseYearInput('abc')).toBeUndefined();
    expect(parseYearInput('12345')).toBeUndefined();
    expect(parseYearInput('-5')).toBeUndefined();
  });
});
