const HIGH_RATING_THRESHOLD = 8;

export function isHighPersonalRating(rating: number): boolean {
  return rating >= HIGH_RATING_THRESHOLD;
}
