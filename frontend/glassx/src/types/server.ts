export interface ServerStatusData {
  online: boolean
  players?: {
    online: number
    max: number
    list?: Array<{ name: string; uuid?: string }>
  }
  version?: string
  tps?: number
  memory?: {
    used: number
    max: number
  }
  motd?: string
}
