export type AdminActionKey =
  | 'reload'
  | 'approve'
  | 'reject'
  | 'delete'
  | 'ban'
  | 'unban'
  | 'list'
  | 'info'
  | 'audit'
  | 'sync'
  | 'password'
  | 'unlink'

export interface UserInfo {
  username: string
  email?: string
  isAdmin?: boolean
  adminActions?: AdminActionKey[]
}

export type UserStatusType = 'pending' | 'approved' | 'rejected'

export interface DiscordUser {
  id: string
  username: string
  discriminator: string
  avatar?: string
  globalName?: string
}

export interface DiscordStatus {
  success: boolean
  linked: boolean
  user?: DiscordUser
  message?: string
}

export interface PendingUser {
  username: string
  email: string
  status: string
  regTime: number
  questionnaireScore?: number | null
  questionnaireReviewSummary?: string | null
}
