module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      [
        'babel-preset-expo',
        {
          web: {
            // Metro web bundles are not executed as ES modules; untransformed
            // `import.meta` throws "Cannot use import.meta outside a module".
            unstable_transformImportMeta: true,
          },
        },
      ],
    ],
  };
};
