// Every component calls render("Name") in its body: one call is one render.
export const renders = new Map();
export function render(name) {
  renders.set(name, (renders.get(name) ?? 0) + 1);
}
export function resetRenders() {
  renders.clear();
}
