import { ref, onMounted, onUnmounted, watch } from 'vue'
import { sessionService } from '@/services/session'

interface UseWebSocketOptions {
  immediate?: boolean
  autoReconnect?: boolean
  reconnectInterval?: number
  maxReconnectAttempts?: number
  onConnected?: (ws: WebSocket) => void
  onDisconnected?: (ws: WebSocket | null, event: CloseEvent) => void
  onError?: (ws: WebSocket | null, event: Event) => void
  onMessage?: (ws: WebSocket, event: MessageEvent) => void
}

export function useWebSocket(
  urlOrFactory: string | (() => string),
  options: UseWebSocketOptions = {}
) {
  const {
    immediate = true,
    autoReconnect = true,
    reconnectInterval = 3000,
    maxReconnectAttempts = 5,
    onConnected,
    onDisconnected,
    onError,
    onMessage
  } = options

  const isConnected = ref(false)
  const lastMessage = ref<MessageEvent | null>(null)
  const error = ref<Event | null>(null)
  
  let ws: WebSocket | null = null
  let reconnectAttempts = 0
  let reconnectTimer: number | undefined
  let shouldReconnect = true

  const scheduleReconnect = () => {
    if (!shouldReconnect || !autoReconnect || reconnectAttempts >= maxReconnectAttempts) {
      return
    }

    reconnectTimer = window.setTimeout(() => {
      reconnectAttempts++
      connect()
    }, reconnectInterval)
  }

  const getUrl = () => {
    let url = typeof urlOrFactory === 'function' ? urlOrFactory() : urlOrFactory
    
    // Handle authentication token automatically
    const token = sessionService.getToken()
    if (token) {
      // Check if token is already in the URL to avoid duplication
      if (!url.includes('token=')) {
        const separator = url.includes('?') ? '&' : '/?'
        // If URL ends with slash and we use /?, it might be //?.
        // UserManagement used `/?token=...`.
        // Let's be smart about it.
        // If url has query params (contains ?), use &.
        // If not, append /? or ? depending on if it ends with /
        if (url.includes('?')) {
            url = `${url}&token=${encodeURIComponent(token)}`
        } else {
            // Ensure we don't double slash if url ends with /
            const prefix = url.endsWith('/') ? '?' : '/?'
            url = `${url}${prefix}token=${encodeURIComponent(token)}`
        }
      }
    }
    
    return url
  }

  const connect = () => {
    shouldReconnect = true

    // Check for existing connection
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    const url = getUrl()

    try {
      ws = new WebSocket(url)
      
      ws.onopen = () => {
        isConnected.value = true
        error.value = null
        reconnectAttempts = 0
        if (onConnected && ws) onConnected(ws)
      }

      ws.onmessage = (event) => {
        lastMessage.value = event
        if (onMessage && ws) onMessage(ws, event)
      }

      ws.onerror = (e) => {
        error.value = e
        console.warn('WebSocket error:', e)
        if (onError) onError(ws, e)
      }

      ws.onclose = (event) => {
        isConnected.value = false
        if (onDisconnected) onDisconnected(ws, event)
        ws = null

        scheduleReconnect()
      }
    } catch (e) {
      console.error('WebSocket connection failed:', e)
      // Trigger error handling
      if (onError) onError(null, e as Event)

      scheduleReconnect()
    }
  }

  const disconnect = () => {
    shouldReconnect = false

    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = undefined
    }
    if (ws) {
      ws.close()
      ws = null
    }
    isConnected.value = false
  }

  const send = (data: string | ArrayBufferLike | Blob | ArrayBufferView) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(data)
    } else {
      console.warn('WebSocket is not connected')
    }
  }

  onMounted(() => {
    if (immediate) {
      connect()
    }
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    lastMessage,
    error,
    connect,
    disconnect,
    send
  }
}
