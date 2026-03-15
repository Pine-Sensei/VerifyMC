import type { AdminActionKey } from '@/types'

export type AdminSectionId = 'server-status' | 'user-management' | 'audit-log'

const USER_MANAGEMENT_SECTION_ACTIONS: AdminActionKey[] = [
  'list',
  'approve',
  'reject',
  'delete',
  'ban',
  'unban',
  'password',
]

const ADMIN_SECTION_REQUIREMENTS: Record<AdminSectionId, AdminActionKey[]> = {
  'server-status': ['list'],
  'user-management': USER_MANAGEMENT_SECTION_ACTIONS,
  'audit-log': ['audit'],
}

export function hasAdminAction(adminActions: readonly AdminActionKey[] | undefined, action: AdminActionKey): boolean {
  return (adminActions ?? []).includes(action)
}

export function hasAnyAdminAction(
  adminActions: readonly AdminActionKey[] | undefined,
  requiredActions: readonly AdminActionKey[],
): boolean {
  return requiredActions.some((action) => hasAdminAction(adminActions, action))
}

export function canAccessAdminSection(
  section: AdminSectionId,
  adminActions: readonly AdminActionKey[] | undefined,
): boolean {
  return hasAnyAdminAction(adminActions, ADMIN_SECTION_REQUIREMENTS[section])
}

export function getAccessibleAdminSections(adminActions: readonly AdminActionKey[] | undefined): AdminSectionId[] {
  return (Object.keys(ADMIN_SECTION_REQUIREMENTS) as AdminSectionId[]).filter((section) =>
    canAccessAdminSection(section, adminActions),
  )
}

export function canAccessUserManagementList(adminActions: readonly AdminActionKey[] | undefined): boolean {
  return hasAdminAction(adminActions, 'list')
}

export function canAccessPendingReviews(adminActions: readonly AdminActionKey[] | undefined): boolean {
  return hasAnyAdminAction(adminActions, ['approve', 'reject'])
}
