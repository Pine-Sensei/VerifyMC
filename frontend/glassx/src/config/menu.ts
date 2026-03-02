import { markRaw } from 'vue'
import {
  User,
  Download,
  Activity,
  Users,
  FileText,
} from 'lucide-vue-next'

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

export const getAdminMenuItems = (t: (key: string) => string) => [
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
]
