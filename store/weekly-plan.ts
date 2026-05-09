import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { WEEKLY_PLAN_STORAGE_KEY, weeklyPlanStorage } from '@/store/persistence';

export const WEEKDAYS = [
  'mon',
  'tue',
  'wed',
  'thu',
  'fri',
  'sat',
  'sun',
] as const;

export type Weekday = (typeof WEEKDAYS)[number];

export const WEEKDAY_LABELS: Record<Weekday, string> = {
  mon: 'Monday',
  tue: 'Tuesday',
  wed: 'Wednesday',
  thu: 'Thursday',
  fri: 'Friday',
  sat: 'Saturday',
  sun: 'Sunday',
};

export type DayAssignments = Record<Weekday, string | null>;

const emptyByDay = (): DayAssignments => ({
  mon: null,
  tue: null,
  wed: null,
  thu: null,
  fri: null,
  sat: null,
  sun: null,
});

const isWeekday = (value: unknown): value is Weekday =>
  typeof value === 'string' && (WEEKDAYS as readonly string[]).includes(value);

type WeeklyPlanStore = {
  byDay: DayAssignments;
  setDayRecipe: (day: Weekday, recipeId: string | null) => void;
};

export const useWeeklyPlan = create<WeeklyPlanStore>()(
  persist(
    (set) => ({
      byDay: emptyByDay(),
      setDayRecipe: (day, recipeId) =>
        set((state) => ({
          byDay: { ...state.byDay, [day]: recipeId },
        })),
    }),
    {
      name: WEEKLY_PLAN_STORAGE_KEY,
      storage: weeklyPlanStorage,
      version: 1,
      partialize: (state) => ({
        byDay: state.byDay,
      }),
      migrate: (persistedState) => {
        const byDay = (persistedState as { byDay?: unknown } | undefined)?.byDay;
        if (!byDay || typeof byDay !== 'object') {
          return { byDay: emptyByDay() };
        }
        const next = emptyByDay();
        for (const key of Object.keys(byDay)) {
          if (!isWeekday(key)) {
            continue;
          }
          const v = (byDay as Record<string, unknown>)[key];
          next[key] = v === null || v === undefined ? null : typeof v === 'string' ? v : null;
        }
        return { byDay: next };
      },
    }
  )
);
