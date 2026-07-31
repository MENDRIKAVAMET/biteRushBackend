import SockJS from 'sockjs-client';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';

export type WebSocketCallback = (message: unknown) => void;

interface SubscriptionInfo {
  destination: string;
  callback: WebSocketCallback;
  subscription: StompSubscription | null;
}

export class WebSocketService {
  private stompClient: Client | null = null;
  private subscriptions: Map<string, SubscriptionInfo> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private messageQueue: Array<{ destination: string; message: unknown }> = [];

  private baseUrl: string;
  private token: string | null;

  constructor(baseUrl: string, token: string | null) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.stompClient?.connected) {
        resolve();
        return;
      }

      try {
        this.stompClient = new Client({
          webSocketFactory: () => new SockJS(`${this.baseUrl}/ws`),

          reconnectDelay: 0, // on gère nous-même

          connectHeaders: this.token
            ? { Authorization: `Bearer ${this.token}` }
            : {},

          debug: () => {},

          onConnect: () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
            this.processMessageQueue();
            this.resubscribeAll();
            resolve();
          },

          onStompError: (frame) => {
            console.error('STOMP error:', frame);
            this.handleConnectionError(reject);
          },

          onWebSocketError: (event) => {
            console.error('WebSocket error:', event);
            this.handleConnectionError(reject);
          },
        });

        this.stompClient.activate();
      } catch (error) {
        console.error('Failed to create WebSocket connection:', error);
        this.handleConnectionError(reject);
      }
    });
  }

  private handleConnectionError(reject: (reason?: unknown) => void) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * this.reconnectAttempts;

      console.log(
        `🔄 Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`
      );

      setTimeout(() => this.connect().catch(reject), delay);
    } else {
      reject(new Error('WebSocket connection failed - max reconnect attempts exceeded'));
    }
  }

  subscribe(destination: string, callback: WebSocketCallback): string {
    if (!this.stompClient?.connected) {
      console.warn(`Not connected, queueing subscription ${destination}`);

      this.subscriptions.set(destination, {
        destination,
        callback,
        subscription: null,
      });

      return destination;
    }

    const subscription = this.stompClient.subscribe(
      destination,
      (message: IMessage) => {
        try {
          const body = JSON.parse(message.body);
          callback(body);
        } catch (err) {
          console.error('Failed to parse message:', err);
        }
      }
    );

    this.subscriptions.set(destination, {
      destination,
      callback,
      subscription,
    });

    console.log(`Subscribed to ${destination}`);
    return destination;
  }

  unsubscribe(destination: string): void {
    const info = this.subscriptions.get(destination);

    if (info?.subscription) {
      info.subscription.unsubscribe();
      console.log(`Unsubscribed from ${destination}`);
    }

    this.subscriptions.delete(destination);
  }

  send(destination: string, body: unknown, headers?: Record<string, string>): void {
    if (!this.stompClient?.connected) {
      console.warn(`Not connected, queueing message to ${destination}`);

      this.messageQueue.push({ destination, message: body });
      return;
    }

    try {
      this.stompClient.publish({
        destination,
        body: JSON.stringify(body),
        headers: headers || {},
      });

      console.log(`Message sent to ${destination}`);
    } catch (error) {
      console.error('Failed to send message:', error);
      this.messageQueue.push({ destination, message: body });
    }
  }

  private processMessageQueue(): void {
    while (this.messageQueue.length > 0) {
      const { destination, message } = this.messageQueue.shift()!;
      this.send(destination, message);
    }
  }

  private resubscribeAll(): void {
    const list = Array.from(this.subscriptions.values());
    this.subscriptions.clear();

    list.forEach(({ destination, callback }) => {
      this.subscribe(destination, callback);
    });
  }

  disconnect(): void {
    if (this.stompClient?.active) {
      this.stompClient.deactivate();
      console.log('WebSocket disconnected');
      this.subscriptions.clear();
      this.messageQueue = [];
    }
  }

  isConnected(): boolean {
    return this.stompClient?.connected ?? false;
  }

  reconnect(): Promise<void> {
    console.log('Manual reconnect');

    this.reconnectAttempts = 0;

    if (this.stompClient?.active) {
      this.stompClient.deactivate();
    }

    return this.connect().then(() => this.resubscribeAll());
  }

  setToken(token: string | null): void {
    this.token = token;

    if (this.stompClient) {
      this.stompClient.connectHeaders = token
        ? { Authorization: `Bearer ${token}` }
        : {};
    }
  }
}

// Singleton
let wsService: WebSocketService | null = null;

export const getWebSocketService = (
  baseUrl: string,
  token: string | null
): WebSocketService => {
  if (!wsService) {
    wsService = new WebSocketService(baseUrl, token);
  }
  return wsService;
};

export const resetWebSocketService = (): void => {
  if (wsService?.isConnected()) {
    wsService.disconnect();
  }
  wsService = null;
};