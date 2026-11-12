export function createAnswerRenderer(MarkdownIt: any): {
  render(text: string): string;
  blocks(text: string): { table: boolean; html: string }[];
};
