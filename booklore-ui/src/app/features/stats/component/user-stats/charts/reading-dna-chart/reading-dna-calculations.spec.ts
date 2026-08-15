import {describe, expect, it} from 'vitest';
import {isHighPersonalRating} from './reading-dna-calculations';

describe('Reading DNA calculations', () => {
  describe('isHighPersonalRating', () => {
    it('uses the high-rating threshold for the ten-point scale', () => {
      expect(isHighPersonalRating(7)).toBe(false);
      expect(isHighPersonalRating(8)).toBe(true);
      expect(isHighPersonalRating(10)).toBe(true);
    });
  });
});
