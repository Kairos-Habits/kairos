# Web — SvelteKit 5 Application

**Generated:** 2026-02-28 | **Commit:** 7dd282c

SvelteKit 5 web application for Kairos habit tracking. Online-first, minimal offline logic.

## Structure

```
web/
├── src/
│   ├── routes/           # SvelteKit file-based routing
│   │   └── +page.svelte  # Page components
│   ├── lib/              # Shared components, utilities
│   │   └── index.ts      # Library exports
│   └── app.html          # HTML template
├── e2e/                  # Playwright E2E tests
├── static/               # Static assets
├── package.json          # Dependencies (bun)
└── .prettierrc           # Tabs, single quotes, no trailing commas
```

## Where to Look

| Task | Location |
|------|----------|
| Pages | `src/routes/` — +page.svelte, +page.ts |
| API routes | `src/routes/api/` — +server.ts |
| Components | `src/lib/` — Shared UI |
| Styles | `src/routes/layout.css` — Tailwind entry |

## Commands

```bash
bun run dev          # Development server
bun run build        # Production build
bun run check        # Type checking
bun run test:unit    # Vitest unit tests
bun run test         # All tests (unit + e2e)
```

## Svelte 5 Conventions

### Runes (NOT legacy syntax)

```svelte
<script lang="ts">
    let count = $state(0);
    let doubled = $derived(count * 2);
    
    $effect(() => {
        console.log(`Count: ${count}`);
    });
</script>
```

### Props

```svelte
<script lang="ts">
    interface Props {
        task: Task;
        onComplete?: (id: string) => void;
    }
    let { task, onComplete }: Props = $props();
</script>
```

### Snippets over slots

```svelte
{#snippet item(task: Task)}
    <li>{task.name}</li>
{/snippet}

{@render item(task)}
```

## Formatting

- Tabs for indent
- Single quotes
- No trailing commas
- Print width: 100

## Anti-Patterns (THIS MODULE)

- **NEVER** use legacy `$:` reactive declarations
- **NEVER** use `export let` for props (use `$props()`)
- **NEVER** use slots (use snippets)
- **AVOID** `globals` package dependency (suspicious)
- **AVOID** duplicating Kotlin types (consume generated types)

## MCP Tools (Svelte Documentation)

When asked about Svelte/SvelteKit, use MCP tools:
1. `list-sections` — Discover available docs
2. `get-documentation` — Fetch specific sections
3. `svelte-autofixer` — Validate Svelte code

## Notes

- Web app is currently minimal (placeholder)
- TypeScript types should be generated from Kotlin contracts
- Uses Tailwind CSS with typography and forms plugins
