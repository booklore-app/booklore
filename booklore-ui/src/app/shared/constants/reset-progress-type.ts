export const ResetProgressTypes = {
  KOREADER: 'KOREADER',
  BOOKLORE: 'BOOKLORE'
} as const;

export type ResetProgressType = keyof typeof ResetProgressTypes;
