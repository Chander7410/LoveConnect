import { useEffect, useMemo, useRef, useState } from 'react';
import AgoraRTC from 'agora-rtc-sdk-ng';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Mic, MicOff, Phone, PhoneOff, Send, Video, VideoOff } from 'lucide-react';
import { API_ORIGIN, endpoints } from '../services/api.js';

export default function ChatPage({ currentUser, profiles, selected, onSelect, callsOnly = false }) {
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [callHistory, setCallHistory] = useState([]);
  const [activeCall, setActiveCall] = useState(null);
  const [callState, setCallState] = useState({ joined: false, mic: true, camera: true });
  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);
  const agoraRef = useRef({ client: null, tracks: [] });

  const conversationId = useMemo(() => {
    if (!selected) return null;
    return [currentUser.firebaseUid, selected.firebaseUid].sort().join('_');
  }, [currentUser.firebaseUid, selected]);

  useEffect(() => {
    if (!selected || callsOnly) return;
    endpoints.chatHistory(selected.firebaseUid).then((response) => setMessages(response.data));
  }, [selected, callsOnly]);

  useEffect(() => {
    if (!conversationId) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_ORIGIN}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/conversations/${conversationId}`, (frame) => {
          setMessages((previous) => [...previous, JSON.parse(frame.body)]);
        });
        client.subscribe(`/topic/calls/${currentUser.firebaseUid}`, (frame) => {
          const call = JSON.parse(frame.body);
          setCallHistory((previous) => [call, ...previous.filter((item) => item.id !== call.id)]);
        });
      }
    });
    client.activate();
    return () => client.deactivate();
  }, [conversationId, currentUser.firebaseUid]);

  useEffect(() => {
    endpoints.callHistory().then((response) => setCallHistory(response.data));
  }, []);

  async function sendMessage(event) {
    event.preventDefault();
    if (!text.trim() || !selected) return;
    await endpoints.sendMessage({ receiverUid: selected.firebaseUid, text });
    setText('');
  }

  async function startCall(type) {
    if (!selected) return;
    const response = await endpoints.startCall({ receiverUid: selected.firebaseUid, type });
    setActiveCall(response.data);
    setCallHistory((previous) => [response.data.call, ...previous]);
    await joinAgora(response.data, type);
  }

  async function joinAgora(callResponse, type) {
    if (!callResponse.appId) {
      return;
    }
    const client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
    await client.join(callResponse.appId, callResponse.channelName, callResponse.token || null, null);
    const tracks = type === 'VIDEO'
      ? await AgoraRTC.createMicrophoneAndCameraTracks()
      : [await AgoraRTC.createMicrophoneAudioTrack()];
    await client.publish(tracks);
    tracks.forEach((track) => {
      if (track.trackMediaType === 'video' && localVideoRef.current) {
        track.play(localVideoRef.current);
      }
    });
    client.on('user-published', async (user, mediaType) => {
      await client.subscribe(user, mediaType);
      if (mediaType === 'video' && remoteVideoRef.current) {
        user.videoTrack.play(remoteVideoRef.current);
      }
      if (mediaType === 'audio') {
        user.audioTrack.play();
      }
    });
    agoraRef.current = { client, tracks };
    setCallState((previous) => ({ ...previous, joined: true }));
  }

  async function endCall(status = 'ENDED') {
    if (activeCall?.call?.id) {
      await endpoints.endCall(activeCall.call.id, status);
    }
    agoraRef.current.tracks.forEach((track) => {
      track.stop();
      track.close();
    });
    await agoraRef.current.client?.leave();
    agoraRef.current = { client: null, tracks: [] };
    setActiveCall(null);
    setCallState({ joined: false, mic: true, camera: true });
  }

  async function toggleMic() {
    const audioTrack = agoraRef.current.tracks.find((track) => track.trackMediaType === 'audio');
    await audioTrack?.setEnabled(!callState.mic);
    setCallState((previous) => ({ ...previous, mic: !previous.mic }));
  }

  async function toggleCamera() {
    const videoTrack = agoraRef.current.tracks.find((track) => track.trackMediaType === 'video');
    await videoTrack?.setEnabled(!callState.camera);
    setCallState((previous) => ({ ...previous, camera: !previous.camera }));
  }

  return (
    <section className="chatLayout">
      <aside className="panel sidebar">
        <h2>Matches</h2>
        {profiles.map((profile) => (
          <button key={profile.firebaseUid} className={selected?.firebaseUid === profile.firebaseUid ? 'person selected' : 'person'} onClick={() => onSelect(profile)}>
            <img src={profile.photoUrl || `https://api.dicebear.com/9.x/lorelei/svg?seed=${profile.firebaseUid}`} alt="" />
            <span><strong>{profile.displayName || 'New Member'}</strong><small>{profile.phoneNumber}</small></span>
          </button>
        ))}
        <h3>Video call history</h3>
        <div className="history">
          {callHistory.map((call) => (
            <div key={call.id} className="historyItem">
              <span>{call.type}</span>
              <strong>{call.status}</strong>
            </div>
          ))}
        </div>
      </aside>

      <section className="chatPanel">
        <header className="chatHeader">
          <div>
            <p className="eyebrow">One-to-one room</p>
            <h2>{selected?.displayName || 'Select a user'}</h2>
          </div>
          <div className="callButtons">
            <button className="iconButton" onClick={() => startCall('AUDIO')} title="Audio call"><Phone /></button>
            <button className="iconButton" onClick={() => startCall('VIDEO')} title="Video call"><Video /></button>
          </div>
        </header>

        {activeCall && (
          <div className="callStage">
            <div className="videoTile" ref={localVideoRef}>Local preview</div>
            <div className="videoTile" ref={remoteVideoRef}>Remote user</div>
            <div className="callToolbar">
              <button className="iconButton" onClick={toggleMic}>{callState.mic ? <Mic /> : <MicOff />}</button>
              <button className="iconButton" onClick={toggleCamera}>{callState.camera ? <Video /> : <VideoOff />}</button>
              <button className="danger" onClick={() => endCall()}><PhoneOff size={18} /> End</button>
            </div>
            {activeCall.note && <p className="muted">{activeCall.note}</p>}
          </div>
        )}

        {!callsOnly && (
          <>
            <div className="messages">
              {messages.map((message) => (
                <div key={message.id} className={message.senderUid === currentUser.firebaseUid ? 'bubble mine' : 'bubble'}>
                  {message.text}
                </div>
              ))}
            </div>
            <form className="composer" onSubmit={sendMessage}>
              <input value={text} onChange={(event) => setText(event.target.value)} placeholder="Write a message" />
              <button className="primary" type="submit"><Send size={18} /> Send</button>
            </form>
          </>
        )}
      </section>
    </section>
  );
}
