import { markRaw } from 'vue'
import {
  User,
  Download,
  Activity,
  Users,
  FileText,
} from 'lucide-vue-next'
import { canAccessAdminSection } from '@/lib/adminAccess'
import type { AdminActionKey } from '@/types'

export const getPlayerMenuItems = (t: (key: string) => string) => [
  {
    id: 'profile',
    label: t('dashboard.menu.profile'),
    icon: markRaw(User),
  },
  {
    id: 'downloads',
    label: t('dashboard.menu.downloads'),
    icon: markRaw(Download),
  },
]

export const getAdminMenuItems = (t: (key: string) => string, adminActions: readonly AdminActionKey[] = []) => [
  {
    id: 'server-status',
    label: t('dashboard.menu.server_status'),
    icon: markRaw(Activity),
  },
  {
    id: 'user-management',
    label: t('dashboard.menu.user_management'),
    icon: markRaw(Users),
  },
  {
    id: 'audit-log',
    label: t('dashboard.menu.audit_log'),
    icon: markRaw(FileText),
  },
].filter((item) => canAccessAdminSection(item.id as 'server-status' | 'user-management' | 'audit-log', adminActions))
