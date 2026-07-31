declare module 'stompjs' {
  export interface Frame {
    body: string;
    command: string;
    headers: Record<string, string>;
  }

  export interface Subscription {
    unsubscribe(): void;
  }

  export interface Client {
    connect(
      headers: Record<string, string>,
      connectCallback: () => void,
      errorCallback: (error: string | Frame) => void
    ): void;
    disconnect(callback?: () => void): void;
    subscribe(destination: string, callback: (frame: Frame) => void): Subscription;
    send(destination: string, headers: Record<string, string>, body: string): void;
    connected: boolean;
    debug: (msg: string) => void;
  }

  function over(ws: any): Client;
}
