export async function runWithConcurrency<T>(
  items: readonly T[],
  limit: number,
  worker: (item: T, index: number) => Promise<void>,
  shouldContinue: () => boolean = () => true,
): Promise<void> {
  if (items.length === 0) return
  let cursor = 0
  const workerCount = Math.min(Math.max(1, Math.floor(limit)), items.length)

  const runWorker = async () => {
    while (shouldContinue()) {
      const index = cursor++
      if (index >= items.length) return
      await worker(items[index], index)
    }
  }

  await Promise.all(Array.from({ length: workerCount }, runWorker))
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}
