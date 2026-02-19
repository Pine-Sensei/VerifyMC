import { sessionService } from '@/services/session'

const API_BASE = '/api'

export interface ApiResponse<T = any> {
  success: boolean
  message?: string
  data?: T
}

export interface ConfigResponse {
  login: {
    enable_email: boolean
    email_smtp: string
  }
  admin: any
  frontend: {
    theme: string
    logo_url: string
    announcement: string
    web_server_prefix: string
    username_regex: string
  }
  authme: {
    enabled: boolean
    require_password: boolean
    password_regex: string
  }
  captcha?: {
    enabled: boolean
    email_enabled: boolean
    type: string
  }
  discord?: {
    enabled: boolean
    required: boolean
  }
  questionnaire?: {
    enabled: boolean
    pass_score: number
    has_text_questions?: boolean
  }
  bedrock?: {
    enabled: boolean
    prefix: string
    username_regex: string
  }
}

export interface QuestionnaireAnswer {
  type: string
  selectedOptionIds: number[]
  textAnswer: string
}

export interface QuestionOption {
  id: number
  text: string
}

export interface QuestionInputMeta {
  min_selections?: number
  max_selections?: number
  min_length?: number
  max_length?: number
  multiline?: boolean
  placeholder?: string
}

export type QuestionType = 'single_choice' | 'multiple_choice' | 'text'

export interface Question {
  id: number
  question: string
  type: QuestionType
  required: boolean
  options?: QuestionOption[]
  input?: QuestionInputMeta
}

export interface SubmitQuestionnaireResponse {
  success: boolean
  passed: boolean
  score: number
  pass_score: number
  manual_review_required?: boolean
  token?: string
  submitted_at?: number
  expires_at?: number
  msg?: string
  message?: string
}

export interface QuestionnaireSubmission {
  passed: boolean
  score: number
  pass_score: number
  manual_review_required?: boolean
  answers: Record<string, QuestionnaireAnswer>
  token: string
  submitted_at: number
  expires_at: number
}

export interface CaptchaResponse {
  success: boolean
  token?: string
  image?: string
  msg?: string
}

export interface SendCodeRequest {
  email: string
  language: string
}

export interface SendCodeResponse {
  success: boolean
  msg: string
}

export interface RegisterRequest {
  email: string
  code?: string
  username: string
  password?: string
  captchaToken?: string
  captchaAnswer?: string
  language: string
  platform?: 'java' | 'bedrock'
  questionnaire?: QuestionnaireSubmission
}

export interface RegisterResponse {
  success: boolean
  msg: string
}

export interface AdminLoginRequest {
  password: string
  language: string
}

export interface AdminLoginResponse {
  success: boolean
  token: string
  message: string
}

export interface PendingUser {
  username: string
  email: string
  status: string
  regTime: string
  questionnaire_score?: number | null
  questionnaire_review_summary?: string | null
}

export interface PendingListResponse {
  success: boolean
  users: PendingUser[]
  message?: string
}

export interface ReviewRequest {
  username: string
  action: 'approve' | 'reject'
  reason?: string
  language: string
}

export interface ReviewResponse {
  success: boolean
  msg: string
}

export interface ChangePasswordRequest {
  username: string
  password: string
  language: string
}

class ApiService {
  private getAuthHeaders(): Record<string, string> {
    const token = sessionService.getToken()
    if (token) {
      return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
    return {
      'Content-Type': 'application/json'
    }
  }

  private getResponseMessage(payload: { msg?: string; message?: string } | null | undefined): string {
    return payload?.msg || payload?.message || ''
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE}${endpoint}`
    const response = await fetch(url, {
      headers: this.getAuthHeaders(),
      ...options,
    })

    if (response.status === 401 || response.status === 403) {
      sessionService.handleUnauthorized()
      throw new Error('Authentication required')
    }

    const data = await response.json()
    const responseMessage = this.getResponseMessage(data)
    if (data && data.success === false && responseMessage.includes('Authentication required')) {
      sessionService.handleUnauthorized()
      throw new Error('Authentication required')
    }
    return data
  }

  // 获取配置
  async getConfig(): Promise<ConfigResponse> {
    const response = await this.request<{ success: boolean; config: ConfigResponse }>('/config')
    return response.config || response as unknown as ConfigResponse
  }

  // 获取验证码
  async getCaptcha(): Promise<CaptchaResponse> {
    return this.request<CaptchaResponse>('/captcha')
  }

  // 发送验证码
  async sendCode(data: SendCodeRequest): Promise<SendCodeResponse> {
    return this.request<SendCodeResponse>('/verify/send', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // 注册用户
  async register(data: RegisterRequest): Promise<RegisterResponse> {
    return this.request<RegisterResponse>('/register', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // 获取问卷
  async getQuestionnaire(language: string): Promise<{
    success: boolean
    data?: {
      enabled: boolean
      questions: Question[]
    }
    msg?: string
    message?: string
  }> {
    return this.request(`/questionnaire/config?language=${encodeURIComponent(language)}`)
  }

  // 提交问卷
  async submitQuestionnaire(payload: {
    answers: Record<string, QuestionnaireAnswer>
    language: string
  }): Promise<SubmitQuestionnaireResponse> {
    return this.request<SubmitQuestionnaireResponse>('/questionnaire/submit', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  }

  // 管理员登录
  async adminLogin(data: AdminLoginRequest): Promise<AdminLoginResponse> {
    return this.request<AdminLoginResponse>('/admin/login', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // 获取待审核用户列表
  async getPendingList(language: string = 'zh'): Promise<PendingListResponse> {
    return this.request<PendingListResponse>(`/admin/users?language=${language}&status=pending`)
  }

  // 审核用户
  async reviewUser(data: ReviewRequest): Promise<ReviewResponse> {
    const endpoint = data.action === 'approve' ? '/admin/user/approve' : '/admin/user/reject'
    return this.request<ReviewResponse>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // 删除用户
  async deleteUser(username: string, language: string = 'zh'): Promise<ReviewResponse> {
    return this.request<ReviewResponse>('/admin/user/delete', {
      method: 'POST',
      body: JSON.stringify({ username, language }),
    })
  }

  // 封禁用户
  async banUser(username: string, language: string = 'zh'): Promise<ReviewResponse> {
    return this.request<ReviewResponse>('/admin/user/ban', {
      method: 'POST',
      body: JSON.stringify({ username, language }),
    })
  }

  // 解封用户
  async unbanUser(username: string, language: string = 'zh'): Promise<ReviewResponse> {
    return this.request<ReviewResponse>('/admin/user/unban', {
      method: 'POST',
      body: JSON.stringify({ username, language }),
    })
  }

  // 更新公告
  async updateAnnouncement(content: string, language: string = 'zh'): Promise<ReviewResponse> {
    return this.request<ReviewResponse>('/update-announcement', {
      method: 'POST',
      body: JSON.stringify({ content, language }),
    })
  }

  // 重载配置
  async reloadConfig(): Promise<{ success: boolean; message: string }> {
    return this.request<{ success: boolean; message: string }>('/reload-config', {
      method: 'POST',
    })
  }

  // 获取所有用户
  async getAllUsers(): Promise<{ success: boolean; users: PendingUser[]; message?: string }> {
    return this.request<{ success: boolean; users: PendingUser[]; message?: string }>('/admin/users')
  }

  // 获取分页用户列表
  async getUsersPaginated(page: number = 1, pageSize: number = 10, search: string = ''): Promise<{
    success: boolean;
    users: PendingUser[];
    pagination: {
      currentPage: number;
      pageSize: number;
      totalCount: number;
      totalPages: number;
      hasNext: boolean;
      hasPrev: boolean;
    };
    message?: string;
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: pageSize.toString(),
    });

    if (search.trim()) {
      params.append('search', search.trim());
    }

    return this.request(`/admin/users?${params.toString()}`);
  }

  // 获取待审核用户列表 (兼容方法)
  async getPendingUsers(): Promise<{ success: boolean; data: PendingUser[]; message?: string }> {
    const response = await this.getPendingList()
    return {
      success: response.success,
      data: response.users,
      message: response.message
    }
  }

  // 获取用户状态
  async getUserStatus(): Promise<{ success: boolean; data: { status: string; reason?: string }; message?: string }> {
    return this.request<{ success: boolean; data: { status: string; reason?: string }; message?: string }>('/user/status')
  }

  // 修改用户密码
  async changePassword(data: ChangePasswordRequest): Promise<ReviewResponse> {
    return this.request<ReviewResponse>('/admin/user/password', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // 检查认证状态
  isAuthenticated(): boolean {
    const token = sessionService.getToken()
    return token !== null
  }

  // 登出
  logout(): void {
    sessionService.clearToken()
  }

  // 版本检查
  async checkVersion(): Promise<{
    success: boolean;
    currentVersion?: string;
    latestVersion?: string;
    updateAvailable?: boolean;
    releasesUrl?: string;
    error?: string;
  }> {
    return this.request('/version')
  }

  // Discord OAuth - 获取授权 URL
  async getDiscordAuthUrl(username: string): Promise<{
    success: boolean;
    auth_url?: string;
    msg?: string;
  }> {
    return this.request(`/discord/auth?username=${encodeURIComponent(username)}`)
  }

  // Discord OAuth - 检查绑定状态
  async getDiscordStatus(username: string): Promise<{
    success: boolean;
    linked: boolean;
    user?: {
      id: string;
      username: string;
      discriminator: string;
      avatar?: string;
      global_name?: string;
    };
    msg?: string;
  }> {
    return this.request(`/discord/status?username=${encodeURIComponent(username)}`)
  }

  // AuthMe 同步
  async syncAuthme(language: string = 'en'): Promise<{ success: boolean; message?: string; msg?: string }> {
    return this.request<{ success: boolean; message?: string; msg?: string }>('/admin/sync', {
      method: 'POST',
      body: JSON.stringify({ language }),
    })
  }
}

export const apiService = new ApiService()
