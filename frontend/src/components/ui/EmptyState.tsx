export function EmptyState({ mensaje }: { mensaje: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-1 py-10 text-center text-sub">
      <p className="text-base">{mensaje}</p>
    </div>
  )
}
