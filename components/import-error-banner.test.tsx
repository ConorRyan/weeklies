import { render, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { SettingsProvider } from '@/contexts/settings';

import { ImportErrorBanner } from './import-error-banner';

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

describe('ImportErrorBanner', () => {
  it('renders title, message, and hint', async () => {
    await render(
      <ImportErrorBanner
        error={{ title: 'Import failed', message: 'Could not read the page.', hint: 'Try another URL.' }}
      />,
      { wrapper }
    );

    expect(screen.getByText('Import failed')).toBeTruthy();
    expect(screen.getByText('Could not read the page.')).toBeTruthy();
    expect(screen.getByText('Try another URL.')).toBeTruthy();
  });

  it('omits the hint when not provided', async () => {
    await render(
      <ImportErrorBanner error={{ title: 'Import failed', message: 'Could not read the page.' }} />,
      { wrapper }
    );

    expect(screen.queryByText('Try another URL.')).toBeNull();
  });

  it('is exposed as an alert for screen readers', async () => {
    const { root } = await render(
      <ImportErrorBanner error={{ title: 'Import failed', message: 'Could not read the page.' }} />,
      { wrapper }
    );

    expect(root?.props.accessibilityRole).toBe('alert');
  });
});
