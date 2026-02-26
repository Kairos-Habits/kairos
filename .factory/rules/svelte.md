# Svelte 5 / SvelteKit Rules

Applies to: `web/` directory (SvelteKit web application)

## Svelte 5 Runes

### Use runes for reactivity
**Applies to**: All Svelte components
**Rule**: Use Svelte 5 runes (`$state`, `$derived`, `$effect`, `$props`) instead of legacy reactive declarations.

```svelte
<!-- ✅ Correct - Svelte 5 runes -->
<script lang="ts">
    let count = $state(0);
    let doubled = $derived(count * 2);

    $effect(() => {
        console.log(`Count changed to ${count}`);
    });
</script>

<!-- ❌ Avoid - legacy syntax -->
<script lang="ts">
    let count = 0;
    $: doubled = count * 2;
    $: console.log(`Count changed to ${count}`);
</script>
```

### $props for component props
**Applies to**: All components with props
**Rule**: Use `$props()` rune for declaring component props.

```svelte
<!-- ✅ Correct -->
<script lang="ts">
    interface Props {
        task: Task;
        onComplete?: (id: string) => void;
    }

    let { task, onComplete }: Props = $props();
</script>

<!-- ❌ Avoid -->
<script lang="ts">
    export let task: Task;
    export let onComplete: (id: string) => void;
</script>
```

### $bindable for two-way binding
**Applies to**: Props that need two-way binding
**Rule**: Use `$bindable()` for mutable props.

```svelte
<script lang="ts">
    interface Props {
        value: number;
    }

    let { value = $bindable() }: Props = $props();
</script>
```

## Component Structure

### Script setup with TypeScript
**Applies to**: All Svelte components
**Rule**: Use `<script lang="ts">` for all component logic.

### Snippets over slots
**Applies to**: Component composition
**Rule**: Use Svelte 5 snippets (`{#snippet}`) instead of slots for content projection.

```svelte
<!-- ✅ Correct - snippets -->
{#snippet item(task: Task)}
    <li>{task.name}</li>
{/snippet}

{@render item(task)}

<!-- ❌ Avoid - slots (legacy) -->
<slot name="item" {task} />
```

## File Organization

### Route structure follows SvelteKit conventions
**Applies to**: `src/routes/`
**Rule**: Use SvelteKit file-based routing conventions.

```
src/routes/
├── +page.svelte          # Page component
├── +page.ts              # Page load function
├── +layout.svelte        # Layout component
└── tasks/
    └── [id]/
        └── +page.svelte  # Dynamic route
```

### Server code in +server.ts
**Applies to**: API endpoints
**Rule**: Create API routes with `+server.ts` files.

### Load functions for data fetching
**Applies to**: Page data
**Rule**: Use `+page.ts` load functions, not onMount for initial data.

```typescript
// +page.ts
export async function load({ fetch }) {
    const response = await fetch('/api/tasks');
    return { tasks: await response.json() };
}
```

## TypeScript

### Define interfaces for props
**Applies to**: All components with props
**Rule**: Define a `Props` interface for component props.

```typescript
// ✅ Correct
interface Props {
    tasks: Task[];
    onSelect: (task: Task) => void;
}

let { tasks, onSelect }: Props = $props();
```

### Type generated from Kotlin contracts
**Applies to**: Domain types
**Rule**: Use TypeScript types generated from Kotlin contracts (when export pipeline established). Do not duplicate type definitions.

## Tailwind CSS

### Utility-first approach
**Applies to**: Styling
**Rule**: Use Tailwind utility classes. Avoid custom CSS unless necessary.

### Use @apply sparingly
**Applies to**: Repeated patterns
**Rule**: Extract to components rather than @apply for most cases.

## Testing

### Vitest for unit/component tests
**Applies to**: `*.test.ts`, `*.test.svelte`
**Rule**: Use Vitest with `vitest-browser-svelte` for component testing.

### Playwright for E2E
**Applies to**: `e2e/` directory
**Rule**: Use Playwright for end-to-end tests.
