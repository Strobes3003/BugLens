// A small, geometric ladybug mark — BugLens's brand icon.
// Deliberately simple (a shell, a center seam, a head, four spots) so it
// reads clearly at navbar size (~20px) as well as on the login screen.
export default function LadybugMark({ size = 24 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      aria-hidden="true"
    >
      <ellipse cx="20" cy="23" rx="15" ry="13.5" fill="var(--accent)" />
      <path
        d="M20 9.5 A15 13.5 0 0 1 20 36.5"
        stroke="var(--spot)"
        strokeWidth="1.6"
      />
      <circle cx="20" cy="9.5" r="6" fill="var(--spot)" />
      <circle cx="16" cy="7.5" r="1" fill="#fff" />
      <circle cx="12.5" cy="19" r="2.3" fill="var(--spot)" />
      <circle cx="27.5" cy="19" r="2.3" fill="var(--spot)" />
      <circle cx="14" cy="29" r="2.3" fill="var(--spot)" />
      <circle cx="26" cy="29" r="2.3" fill="var(--spot)" />
    </svg>
  )
}
