import { useWeeklyPlan } from '@/store/weekly-plan';

describe('useWeeklyPlan actions', () => {
  beforeEach(() => {
    useWeeklyPlan.setState({
      byDay: {
        mon: null,
        tue: null,
        wed: null,
        thu: null,
        fri: null,
        sat: null,
        sun: null,
      },
    });
  });

  it('assigns recipe to a weekday', () => {
    useWeeklyPlan.getState().setDayRecipe('mon', 'recipe-1');
    expect(useWeeklyPlan.getState().byDay.mon).toBe('recipe-1');
  });

  it('clears a weekday with null', () => {
    useWeeklyPlan.setState({
      byDay: {
        mon: 'recipe-1',
        tue: null,
        wed: null,
        thu: null,
        fri: null,
        sat: null,
        sun: null,
      },
    });
    useWeeklyPlan.getState().setDayRecipe('mon', null);
    expect(useWeeklyPlan.getState().byDay.mon).toBeNull();
  });
});
