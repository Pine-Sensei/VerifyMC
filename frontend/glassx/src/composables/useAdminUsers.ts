import { onScopeDispose, ref, type Ref, watch } from 'vue'
import { apiService, type PendingUser } from '@/services/api'

interface PaginationState {
  currentPage: number
  totalCount: number
  totalPages: number
  hasNext: boolean
  hasPrev: boolean
}

interface UsersPaginatedResponse {
  success: boolean
  users?: PendingUser[]
  pagination?: PaginationState
  message?: string
}

interface UseAdminUsersOptions {
  locale: Ref<string>
  t: (key: string) => string
  notification: {
    error: (title: string, message?: string) => void
  }
}

export const applySearch = (users: PendingUser[], query: string): PendingUser[] => {
  const keyword = query.trim().toLowerCase()
  if (!keyword) {
    return users
  }

  return users.filter((user) => {
    return user.username?.toLowerCase().includes(keyword) || user.email?.toLowerCase().includes(keyword)
  })
}

export const paginate = (users: PendingUser[], page: number, pageSize: number): PendingUser[] => {
  const startIndex = (page - 1) * pageSize
  return users.slice(startIndex, startIndex + pageSize)
}

export const applyPaginationState = (result: Partial<PaginationState>): PaginationState => {
  return {
    currentPage: result.currentPage ?? 1,
    totalCount: result.totalCount ?? 0,
    totalPages: result.totalPages ?? 0,
    hasNext: result.hasNext ?? false,
    hasPrev: result.hasPrev ?? false,
  }
}

const getManualPagination = (totalCount: number, page: number, pageSize: number): PaginationState => {
  const totalPages = Math.ceil(totalCount / pageSize)
  return {
    currentPage: page,
    totalCount,
    totalPages,
    hasNext: page < totalPages,
    hasPrev: page > 1,
  }
}

export const useAdminUsers = ({ locale, t, notification }: UseAdminUsersOptions) => {
  const loading = ref(false)
  const allUsers = ref<PendingUser[]>([])
  const searchQuery = ref('')

  const currentPage = ref(1)
  const pageSize = ref(10)
  const totalCount = ref(0)
  const totalPages = ref(0)
  const hasNext = ref(false)
  const hasPrev = ref(false)

  let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

  const notifyLoadError = (message?: string, fallbackKey: string = 'admin.users.messages.load_error') => {
    if (message && message.includes('Authentication')) {
      notification.error(t('common.error'), message)
      return
    }
    notification.error(t('common.error'), message || t(fallbackKey))
  }

  const setPagination = (pagination: Partial<PaginationState>) => {
    const state = applyPaginationState(pagination)
    currentPage.value = state.currentPage
    totalCount.value = state.totalCount
    totalPages.value = state.totalPages
    hasNext.value = state.hasNext
    hasPrev.value = state.hasPrev
  }

  const applyManualUsers = (users: PendingUser[]) => {
    const filteredData = applySearch(users, searchQuery.value)
    allUsers.value = paginate(filteredData, currentPage.value, pageSize.value)
    setPagination(getManualPagination(filteredData.length, currentPage.value, pageSize.value))
  }

  const loadAllUsers = async () => {
    loading.value = true

    try {
      await apiService.syncAuthme(locale.value)
      const response: UsersPaginatedResponse = await apiService.getUsersPaginated(
        currentPage.value,
        pageSize.value,
        searchQuery.value,
      )

      if (response.success && response.users && response.users.length > 0) {
        allUsers.value = response.users
        setPagination(response.pagination ?? {})
        return
      }

      if (response.success) {
        try {
          const pendingResponse = await apiService.getPendingList(locale.value)
          if (pendingResponse.success && pendingResponse.users?.length) {
            applyManualUsers(pendingResponse.users)
            return
          }
        } catch {
          // continue to all-users fallback
        }

        const fallbackResponse = await apiService.getAllUsers()
        if (fallbackResponse.success && fallbackResponse.users) {
          applyManualUsers(fallbackResponse.users)
          return
        }
      }

      allUsers.value = []
      setPagination({})
      notifyLoadError(response.message)
    } catch (error) {
      allUsers.value = []
      setPagination({})
      notifyLoadError(undefined, 'errors.network')
    } finally {
      loading.value = false
    }
  }

  const handlePageChange = (page: number) => {
    currentPage.value = page
    loadAllUsers()
  }

  const handlePageSizeChange = (newPageSize: number) => {
    pageSize.value = newPageSize
    currentPage.value = 1
    loadAllUsers()
  }

  watch(searchQuery, () => {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer)
    }

    searchDebounceTimer = setTimeout(() => {
      currentPage.value = 1
      loadAllUsers()
    }, 500)
  })


  onScopeDispose(() => {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer)
    }
  })
  const resetUsersPagination = () => {
    currentPage.value = 1
  }

  return {
    loading,
    allUsers,
    searchQuery,
    currentPage,
    pageSize,
    totalCount,
    totalPages,
    hasNext,
    hasPrev,
    loadAllUsers,
    handlePageChange,
    handlePageSizeChange,
    resetUsersPagination,
  }
}
