import React, { useState, useEffect } from 'react';
import { LiveKitRoom, VideoConference, RoomAudioRenderer } from '@livekit/components-react';
import '@livekit/components-styles';
import { X, Radio, Users, MessageSquare } from 'lucide-react';

export interface VideoRoomProps {
  roomName: string;
  userName: string;
  onLeave: () => void;
}

export const VideoRoom: React.FC<VideoRoomProps> = ({ roomName, userName, onLeave }) => {
  const livekitUrl = import.meta.env.VITE_LIVEKIT_URL || 'wss://panu-live-demo.livekit.cloud';
  const [token] = useState<string>('');
  const [simulationMode, setSimulationMode] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setSimulationMode(true);
    }, 800);
    return () => clearTimeout(timer);
  }, []);

  if (simulationMode) {
    return (
      <div className="fixed inset-0 z-[250] bg-[#0B0C12] text-white flex flex-col justify-between p-4 animate-in fade-in duration-300">
        {/* Top Header */}
        <div className="flex items-center justify-between pb-3 border-b border-white/10">
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-red-600 text-white font-black text-xs animate-pulse">
              <Radio className="w-3.5 h-3.5" /> LIVE ROOM (LiveKit)
            </span>
            <span className="font-bold text-sm">{roomName}</span>
          </div>
          <button
            onClick={onLeave}
            className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Center Live Stream & Chat */}
        <div className="flex-1 grid grid-cols-1 md:grid-cols-3 gap-4 py-4 overflow-hidden">
          <div className="md:col-span-2 relative bg-black rounded-2xl overflow-hidden border border-white/10 flex items-center justify-center">
            <video
              src="https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
              autoPlay
              loop
              muted
              playsInline
              className="w-full h-full object-cover"
            />
            <div className="absolute top-3 left-3 bg-black/60 backdrop-blur-md px-3 py-1 rounded-full text-xs font-bold text-amber-400 flex items-center gap-1.5">
              <Users className="w-3.5 h-3.5" /> 1,420 spectateurs en direct
            </div>
          </div>
          <div className="bg-[#131622] rounded-2xl border border-white/10 p-4 flex flex-col justify-between">
            <h3 className="font-extrabold text-sm text-amber-400 mb-2 flex items-center gap-1.5">
              <MessageSquare className="w-4 h-4" /> Chat en Direct (LiveKit)
            </h3>
            <div className="flex-1 overflow-y-auto space-y-2 pr-1 text-xs text-white/90">
              <div className="bg-white/5 p-2 rounded-lg">
                <span className="font-bold text-amber-400">Aminata :</span> Incroyable ce live ! 🔥
              </div>
              <div className="bg-white/5 p-2 rounded-lg">
                <span className="font-bold text-amber-400">Kofi :</span> Quel est le prochain sujet ?
              </div>
              <div className="bg-white/5 p-2 rounded-lg">
                <span className="font-bold text-amber-400">{userName} :</span> Bonjour tout le monde ! 🚀
              </div>
            </div>
            <div className="pt-2 border-t border-white/10 flex gap-2 mt-2">
              <input
                type="text"
                placeholder="Envoyer un message..."
                className="flex-1 bg-white/10 border border-white/15 rounded-full px-3 py-2 text-xs text-white placeholder-white/40 outline-none focus:border-amber-500"
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && (e.target as HTMLInputElement).value.trim()) {
                    alert(`Message envoyé : ${(e.target as HTMLInputElement).value}`);
                    (e.target as HTMLInputElement).value = '';
                  }
                }}
              />
            </div>
          </div>
        </div>

        {/* Bottom controls */}
        <div className="flex items-center justify-center gap-4 pb-2">
          <button
            onClick={onLeave}
            className="px-6 py-2.5 rounded-full bg-red-600 hover:bg-red-500 text-white font-extrabold text-xs shadow-lg shadow-red-600/30"
          >
            Quitter le Live
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[250] bg-black">
      <LiveKitRoom
        video={true}
        audio={true}
        token={token}
        serverUrl={livekitUrl}
        data-lk-theme="default"
        style={{ height: '100vh', width: '100vw' }}
      >
        <VideoConference />
        <RoomAudioRenderer />
        <div className="absolute top-4 right-4 z-50">
          <button
            onClick={onLeave}
            className="p-2 rounded-full bg-red-600 text-white shadow-xl hover:bg-red-500"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </LiveKitRoom>
    </div>
  );
};

export default VideoRoom;
