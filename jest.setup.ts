jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock')
);

// Components focus inputs via requestAnimationFrame; the jest environment has no rAF.
if (typeof global.requestAnimationFrame === 'undefined') {
  global.requestAnimationFrame = ((callback: (time: number) => void): number =>
    setTimeout(() => callback(Date.now()), 0) as unknown as number) as typeof global.requestAnimationFrame;
  global.cancelAnimationFrame = ((id: number): void => {
    clearTimeout(id as unknown as ReturnType<typeof setTimeout>);
  }) as unknown as typeof global.cancelAnimationFrame;
}
