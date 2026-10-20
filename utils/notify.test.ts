import Toast from 'react-native-toast-message';

import { notifyError, notifyInfo } from '@/utils/notify';

jest.mock('react-native-toast-message', () => ({
  __esModule: true,
  default: { show: jest.fn() },
}));

const show = Toast.show as unknown as jest.Mock;

describe('notifyInfo', () => {
  beforeEach(() => {
    show.mockClear();
  });

  it('shows an info toast with title and message', () => {
    notifyInfo('Saved', 'Recipe added');

    expect(show).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'info', text1: 'Saved', text2: 'Recipe added' })
    );
  });

  it('shows an info toast with only a title', () => {
    notifyInfo('Saved');

    expect(show).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'info', text1: 'Saved', text2: undefined })
    );
  });
});

describe('notifyError', () => {
  beforeEach(() => {
    show.mockClear();
  });

  it('shows an error toast with title and message', () => {
    notifyError('Import failed', 'Could not read the page.');

    expect(show).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'error',
        text1: 'Import failed',
        text2: 'Could not read the page.',
      })
    );
  });
});

describe('toast base options', () => {
  it('positions at the top with 4s visibility', () => {
    notifyInfo('Saved');

    expect(show).toHaveBeenCalledWith(
      expect.objectContaining({ position: 'top', visibilityTime: 4000 })
    );
  });
});
