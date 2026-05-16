import { useEffect, useState } from 'react';
import type { EdgeInsets } from 'react-native-safe-area-context';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const ZERO: EdgeInsets = { top: 0, right: 0, bottom: 0, left: 0 };

/**
 * Safe-area values can differ between static HTML export and the browser on first paint.
 * Zeros match the server render; real insets apply after mount (avoids React hydration #418).
 */
export function useDeferredSafeAreaInsets(): EdgeInsets {
  const insets = useSafeAreaInsets();
  const [deferred, setDeferred] = useState<EdgeInsets>(ZERO);

  useEffect(() => {
    setDeferred(insets);
  }, [insets.top, insets.right, insets.bottom, insets.left]);

  return deferred;
}
