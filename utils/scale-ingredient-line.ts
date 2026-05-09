/**
 * Multiplies every numeric token in an ingredient line by `factor`.
 * Handles integers, decimals, and fractions like `1/2` or `1 / 2`.
 */
const NUMERIC_TOKEN =
  /\d+\s*\/\s*\d+|\d+\.\d+|\d+/g;

function parseNumericToken(raw: string): number | null {
  const t = raw.trim();
  if (t.includes('/')) {
    const parts = t.split(/\s*\/\s*/);
    if (parts.length !== 2) {
      return null;
    }
    const a = Number(parts[0]);
    const b = Number(parts[1]);
    if (!Number.isFinite(a) || !Number.isFinite(b) || b === 0) {
      return null;
    }
    return a / b;
  }
  const n = Number(t);
  return Number.isFinite(n) ? n : null;
}

function formatScaled(n: number): string {
  if (!Number.isFinite(n)) {
    return String(n);
  }
  const rounded = Math.round(n * 10000) / 10000;
  if (Math.abs(rounded - Math.round(rounded)) < 1e-9) {
    return String(Math.round(rounded));
  }
  return String(rounded);
}

export function scaleIngredientLine(line: string, factor: number): string {
  if (!Number.isFinite(factor) || factor === 1) {
    return line;
  }
  return line.replace(NUMERIC_TOKEN, (match) => {
    const value = parseNumericToken(match);
    if (value === null) {
      return match;
    }
    return formatScaled(value * factor);
  });
}
