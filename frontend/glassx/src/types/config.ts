export interface AppConfig {
  webServerPrefix?: string
  discord?: {
    enabled: boolean
    clientId: string
    redirectUri: string
  }
}
