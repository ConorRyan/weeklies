import { MAX_HTML_FOR_LLM } from '@/utils/recipe-import/constants';

export type PreprocessedRecipePage = {
  jsonLdRecipes: unknown[];
  htmlSnippet: string;
};

function stripScriptAndStyle(html: string): string {
  return html
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '');
}

function extractJsonLdRecipes(html: string): unknown[] {
  const recipes: unknown[] = [];
  const re = /<script[^>]*type\s*=\s*["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi;
  let match: RegExpExecArray | null;
  while ((match = re.exec(html)) !== null) {
    const raw = match[1]?.trim();
    if (!raw) {
      continue;
    }
    try {
      const parsed = JSON.parse(raw) as unknown;
      collectRecipeNodes(parsed, recipes);
    } catch {
      /* skip invalid JSON-LD block */
    }
  }
  return recipes;
}

function isRecipeType(typeValue: unknown): boolean {
  if (typeof typeValue === 'string') {
    return typeValue === 'Recipe' || typeValue.endsWith('Recipe');
  }
  if (Array.isArray(typeValue)) {
    return typeValue.some((t) => typeof t === 'string' && (t === 'Recipe' || t.endsWith('Recipe')));
  }
  return false;
}

function collectRecipeNodes(node: unknown, out: unknown[]): void {
  if (node == null) {
    return;
  }
  if (Array.isArray(node)) {
    for (const item of node) {
      collectRecipeNodes(item, out);
    }
    return;
  }
  if (typeof node !== 'object') {
    return;
  }
  const o = node as Record<string, unknown>;
  if ('@graph' in o) {
    collectRecipeNodes(o['@graph'], out);
  }
  if (isRecipeType(o['@type'])) {
    out.push(node);
  }
}

function capText(text: string, max: number): string {
  if (text.length <= max) {
    return text;
  }
  return text.slice(0, max);
}

export function preprocessRecipeHtml(html: string): PreprocessedRecipePage {
  const stripped = stripScriptAndStyle(html);
  const jsonLdRecipes = extractJsonLdRecipes(html);
  const htmlSnippet = capText(stripped.replace(/\s+/g, ' ').trim(), MAX_HTML_FOR_LLM);
  return { jsonLdRecipes, htmlSnippet };
}
