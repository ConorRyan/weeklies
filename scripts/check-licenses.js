#!/usr/bin/env node
/**
 * Dependency license audit.
 *
 * Scans every package under node_modules (including nested copies), reads the
 * declared license, and fails if anything is not free software. The FOSS SPDX
 * allowlist below mirrors what common FOSS policies accept.
 *
 * Run: node scripts/check-licenses.js
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..');
const MODULES = path.join(ROOT, 'node_modules');

// SPDX identifiers accepted as free software.
const FOSS = new Set([
  '0BSD',
  'Apache-1.1',
  'Apache-2.0',
  'Artistic-1.0',
  'Artistic-2.0',
  'BSD-2-Clause',
  'BSD-3-Clause',
  'BSD-3-Clause-Clear',
  'BSD-4-Clause',
  'BSD-Source-Code',
  'BlueOak-1.0.0',
  'CC0-1.0',
  'CC-BY-1.0',
  'CC-BY-2.0',
  'CC-BY-2.5',
  'CC-BY-3.0',
  'CC-BY-4.0',
  'CDDL-1.0',
  'CDDL-1.1',
  'EPL-1.0',
  'EPL-2.0',
  'GPL-1.0-only',
  'GPL-1.0-or-later',
  'GPL-2.0-only',
  'GPL-2.0-or-later',
  'GPL-3.0-only',
  'GPL-3.0-or-later',
  'ISC',
  'LGPL-2.0-only',
  'LGPL-2.0-or-later',
  'LGPL-2.1-only',
  'LGPL-2.1-or-later',
  'LGPL-3.0-only',
  'LGPL-3.0-or-later',
  'MIT',
  'MIT-0',
  'MPL-1.1',
  'MPL-2.0',
  'Python-2.0',
  'Unlicense',
  'WTFPL',
  'Zlib',
]);

// Packages whose license field needs a manual look (e.g. an object with
// multiple licenses, or a name that is FOSS but spelled differently). Keep
// this list SHORT and review each entry when adding to it.
const REVIEWED = new Set([]);

function normalize(license) {
  if (typeof license === 'string') {
    return license.trim();
  }
  if (Array.isArray(license)) {
    return license.map(normalize);
  }
  if (license && typeof license === 'object') {
    if (typeof license.type === 'string') return license.type.trim();
    if (Array.isArray(license.types)) return license.types.map(normalize);
  }
  return null;
}

// SPDX expressions like "(MIT OR Apache-2.0)" or "MIT AND Apache-2.0":
// OR means the user may pick any side (free if any side is), AND requires
// every side to be free. Anything else fails closed.
function licensesOk(value) {
  if (value === null) return false;
  if (Array.isArray(value)) return value.every((entry) => licensesOk(entry));
  if (typeof value !== 'string') return false;
  const tokens = value.match(/\(|\)|[^\s()]+/g);
  if (tokens === null) return false;
  let index = 0;
  const peek = () => tokens[index];

  function parseExpr() {
    const terms = [parseTerm()];
    while (peek() === 'OR') {
      index += 1;
      terms.push(parseTerm());
    }
    return terms.some(Boolean); // OR: any free side makes it free
  }

  function parseTerm() {
    const factors = [parseFactor()];
    while (peek() === 'AND') {
      index += 1;
      factors.push(parseFactor());
    }
    return factors.every(Boolean); // AND: all sides must be free
  }

  function parseFactor() {
    if (peek() === '(') {
      index += 1;
      const result = parseExpr();
      if (peek() === ')') index += 1;
      return result;
    }
    const token = tokens[index];
    index += 1;
    return FOSS.has(token);
  }

  return parseExpr() && index === tokens.length;
}

const problems = [];
const unknown = [];
let scanned = 0;

function scan(dir) {
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch {
    return;
  }
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === '.bin' || entry.name.startsWith('.')) continue;
      // npm scope directories: recurse into them.
      if (entry.name.startsWith('@')) {
        scan(full);
        continue;
      }
      audit(path.join(full, 'package.json'), full);
      // Nested dependency copies (not hoisted).
      const nested = path.join(full, 'node_modules');
      if (fs.existsSync(nested)) scan(nested);
    }
  }
}

function audit(pkgPath, dir) {
  let pkg;
  try {
    pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
  } catch {
    return; // not a package (e.g. .DS_Store debris); skip
  }
  if (!pkg.name || pkg.private === true) return;
  scanned += 1;
  const license = normalize(pkg.license ?? pkg.licenses);
  const id = `${pkg.name}@${pkg.version ?? '?'}`;
  if (licensesOk(license)) return;
  if (REVIEWED.has(id)) return;
  const declared = license === null ? '(none declared)' : JSON.stringify(license);
  if (declared === '(none declared)') {
    unknown.push(`${id} — no license field`);
  } else {
    problems.push(`${id} — ${declared}`);
  }
}

scan(MODULES);

console.log(`Scanned ${scanned} packages under node_modules.`);
if (unknown.length > 0) {
  console.log(`\nNo license declared (${unknown.length}):`);
  for (const line of unknown) console.log(`  ${line}`);
}
if (problems.length > 0) {
  console.log(`\nNon-allowlisted license declarations (${problems.length}):`);
  for (const line of problems) console.log(`  ${line}`);
}
if (problems.length > 0 || unknown.length > 0) {
  console.log(
    '\nReview each entry: add genuinely free ones to the REVIEWED set (or FOSS allowlist),\n' +
      'and remove or replace anything proprietary.'
  );
  process.exitCode = 1;
} else {
  console.log('All dependencies declare allowlisted FOSS licenses.');
}
