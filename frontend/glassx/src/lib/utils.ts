import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatDate(dateValue: string | number): string {
  if (!dateValue) return '—'
  const date = typeof dateValue === 'number' ? new Date(dateValue) : new Date(dateValue)
  return date.toLocaleDateString()
}

export const statusColors = {
  pending: {
    bg: 'bg-yellow-500/20',
    text: 'text-yellow-400',
    border: 'border-yellow-500/30',
    cardBg: 'bg-yellow-500/10',
    cardBorder: 'border-yellow-500',
    iconBg: 'bg-yellow-500'
  },
  approved: {
    bg: 'bg-green-500/20',
    text: 'text-green-500',
    border: 'border-green-500/30',
    cardBg: 'bg-green-500/10',
    cardBorder: 'border-green-500',
    iconBg: 'bg-green-500'
  },
  rejected: {
    bg: 'bg-red-500/20',
    text: 'text-red-500',
    border: 'border-red-500/30',
    cardBg: 'bg-red-500/10',
    cardBorder: 'border-red-500',
    iconBg: 'bg-red-500'
  },
  banned: {
    bg: 'bg-red-500/20',
    text: 'text-red-500',
    border: 'border-red-500/30',
    cardBg: 'bg-red-500/10',
    cardBorder: 'border-red-500',
    iconBg: 'bg-red-500'
  },
  default: {
    bg: 'bg-white/5',
    text: 'text-white/70',
    border: 'border-white/10',
    cardBg: 'bg-white/10',
    cardBorder: 'border-white/10',
    iconBg: 'bg-white/20'
  }
}

export function getStatusColors(status: string) {
  const s = (status || '').toLowerCase() as keyof typeof statusColors
  return statusColors[s] || statusColors.default
}
