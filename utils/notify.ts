import Toast from 'react-native-toast-message';

const toastBase = {
  position: 'top' as const,
  visibilityTime: 4000,
};

/** Short notifications (web has no `Alert.alert`; native gets toasts for parity). */
export function notifyInfo(title: string, message?: string): void {
  Toast.show({
    type: 'info',
    text1: title,
    text2: message,
    ...toastBase,
  });
}

export function notifyError(title: string, message?: string): void {
  Toast.show({
    type: 'error',
    text1: title,
    text2: message,
    ...toastBase,
  });
}
