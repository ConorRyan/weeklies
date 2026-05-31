import { MAX_HTML_FOR_LLM } from '@/utils/recipe-import/constants';
import { preprocessRecipeHtml } from '@/utils/recipe-import/preprocess';

describe('preprocessRecipeHtml', () => {
  it('strips script and style tags from html snippet', () => {
    const html = `
      <html>
        <style>.hidden { display: none; }</style>
        <script>alert("x")</script>
        <body><p>Recipe body</p></body>
      </html>
    `;
    const result = preprocessRecipeHtml(html);
    expect(result.htmlSnippet).toContain('Recipe body');
    expect(result.htmlSnippet).not.toContain('alert');
    expect(result.htmlSnippet).not.toContain('.hidden');
  });

  it('extracts JSON-LD Recipe nodes from original html', () => {
    const html = `
      <html>
        <script type="application/ld+json">
          {"@type":"Recipe","name":"Pasta","recipeIngredient":["200g pasta"]}
        </script>
        <body>Pasta page</body>
      </html>
    `;
    const result = preprocessRecipeHtml(html);
    expect(result.jsonLdRecipes).toHaveLength(1);
    expect((result.jsonLdRecipes[0] as { name: string }).name).toBe('Pasta');
  });

  it('extracts Recipe nodes from @graph', () => {
    const html = `
      <script type="application/ld+json">
        {"@graph":[{"@type":"WebPage","name":"Site"},{"@type":"Recipe","name":"Soup"}]}
      </script>
    `;
    const result = preprocessRecipeHtml(html);
    expect(result.jsonLdRecipes).toHaveLength(1);
    expect((result.jsonLdRecipes[0] as { name: string }).name).toBe('Soup');
  });

  it('parses JSON-LD from original html even when script tags are stripped from snippet', () => {
    const html = `
      <script type="application/ld+json">
        {"@type":"Recipe","name":"From LD"}
      </script>
      <p>Visible text only</p>
    `;
    const result = preprocessRecipeHtml(html);
    expect(result.jsonLdRecipes).toHaveLength(1);
    expect(result.htmlSnippet).not.toContain('From LD');
    expect(result.htmlSnippet).toContain('Visible text only');
  });

  it('caps html snippet at MAX_HTML_FOR_LLM', () => {
    const html = `<p>${'x'.repeat(MAX_HTML_FOR_LLM + 500)}</p>`;
    const result = preprocessRecipeHtml(html);
    expect(result.htmlSnippet.length).toBeLessThanOrEqual(MAX_HTML_FOR_LLM);
  });

  it('skips invalid JSON-LD blocks', () => {
    const html = `
      <script type="application/ld+json">{not valid json</script>
      <p>content</p>
    `;
    const result = preprocessRecipeHtml(html);
    expect(result.jsonLdRecipes).toHaveLength(0);
  });
});
