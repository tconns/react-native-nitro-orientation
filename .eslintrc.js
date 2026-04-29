module.exports = {
  root: true,
  extends: ['@react-native', 'prettier'],
  plugins: ['prettier'],
  rules: {
    'prettier/prettier': [
      'warn',
      {
        quoteProps: 'consistent',
        singleQuote: true,
        tabWidth: 2,
        trailingComma: 'es5',
        useTabs: false,
      },
    ],
  },
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: true,
  },
}
