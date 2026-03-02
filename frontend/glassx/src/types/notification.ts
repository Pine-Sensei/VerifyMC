export type NotificationType = 'success' | 'error' | 'warning' | 'info'

export interface NotificationInput {
  type: NotificationType
  title: string
  message?: string
  duration?: number
}

export interface Notification {
  id: string
  type: NotificationType
  title: string
  message?: string
  duration: number
  remaining: number
  paused: boolean
}

export interface ToastProps {
  type?: NotificationType
  title: string
  message?: string
  duration?: number
  autoClose?: boolean
}
