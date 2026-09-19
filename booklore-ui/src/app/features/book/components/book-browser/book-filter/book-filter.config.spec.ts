import {describe, expect, it} from 'vitest';
import {Book} from '../../../model/book.model';
import {FILTER_EXTRACTORS} from './book-filter.config';

describe('file-size filter ranges', () => {
  it.each([
    [102400, '100–250 MB'],
    [255999, '100–250 MB'],
    [2097152, '2–5 GB'],
    [5242879, '2–5 GB']
  ])('includes %i KB in the %s bucket', (fileSizeKb, expectedLabel) => {
    const filters = FILTER_EXTRACTORS.fileSize({fileSizeKb} as Book);

    expect(filters).toHaveLength(1);
    expect(filters[0].name).toBe(expectedLabel);
  });
});
