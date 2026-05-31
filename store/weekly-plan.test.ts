import { WEEKDAYS, emptyByDay, weekdayFromDate } from '@/store/weekly-plan';

describe('weekdayFromDate', () => {
  it('maps all JS getDay values to app weekdays', () => {
    const cases: Array<[number, (typeof WEEKDAYS)[number]]> = [
      [0, 'sun'],
      [1, 'mon'],
      [2, 'tue'],
      [3, 'wed'],
      [4, 'thu'],
      [5, 'fri'],
      [6, 'sat'],
    ];
    for (const [jsDay, expected] of cases) {
      const d = new Date(2024, 0, 7 + jsDay);
      expect(weekdayFromDate(d)).toBe(expected);
    }
  });
});

describe('emptyByDay', () => {
  it('returns all weekdays mapped to null', () => {
    const byDay = emptyByDay();
    for (const day of WEEKDAYS) {
      expect(byDay[day]).toBeNull();
    }
  });
});
