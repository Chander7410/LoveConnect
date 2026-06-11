import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_ORIGIN } from './api.js';

export const rtcConfig = {
  iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
};

export const createPeer = () => new RTCPeerConnection(rtcConfig);

export const createSignalClient = ({ token, uid, onSignal }) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${API_ORIGIN}/ws`),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe(`/topic/signaling/${uid}`, (message) => {
        onSignal(JSON.parse(message.body));
      });
    }
  });
  client.activate();
  return client;
};

export const publishSignal = (client, type, body) => {
  if (!client?.connected) return false;
  client.publish({
    destination: `/app/signal/${type}`,
    body: JSON.stringify(body)
  });
  return true;
};

export const appUidFromToken = (token) => {
  if (!token?.startsWith('app:')) return '';
  const encoded = token.split(':')[1];
  if (!encoded) return '';
  const normalized = encoded.replace(/-/g, '+').replace(/_/g, '/');
  return atob(normalized);
};
