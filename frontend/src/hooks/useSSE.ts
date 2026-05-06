import { useEffect, useRef } from 'react';

export function useSSE(url: string, onMessage: (data: any) => void) {
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    const es = new EventSource(url);
    eventSourceRef.current = es;

    es.addEventListener('incident-update', (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (e) {
        console.warn('SSE parse error', e);
      }
    });

    es.onerror = () => {
      es.close();
      // Reconnect after 3 seconds
      setTimeout(() => {
        if (eventSourceRef.current === es) {
          eventSourceRef.current = new EventSource(url);
        }
      }, 3000);
    };

    return () => { es.close(); };
  }, [url]);
}

export function useInterval(callback: () => void, delay: number) {
  const savedCallback = useRef(callback);
  useEffect(() => { savedCallback.current = callback; }, [callback]);
  useEffect(() => {
    const id = setInterval(() => savedCallback.current(), delay);
    return () => clearInterval(id);
  }, [delay]);
}
