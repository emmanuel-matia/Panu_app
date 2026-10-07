import React, { useEffect, useState } from 'react';
import { joinOrCreateLiveKitRoom, LiveKitRoomSession } from '../../services/livekitCloudService';

export const LiveKitStudio: React.FC = () => {
  const [session, setSession] = useState<LiveKitRoomSession | null>(null);
  const [isLive, setIsLive] = useState(false);

  const startLive = async () => {
    const roomSession = await joinOrCreateLiveKitRoom({
      roomName: 'studio-live-' + Date.now(),
      canPublish: true,
    });
    setSession(roomSession);
    setIsLive(true);
  };

  return (
    <div style={{ padding: 20, backgroundColor: '#15161E', borderRadius: 16 }}>
      <h2 style={{ color: '#E5A93C' }}>Diffusion en direct</h2>
      {!isLive ? (
        <button onClick={startLive} className="btn btn-gold">
          Démarrer le direct
        </button>
      ) : (
        <div>
          <p>En direct dans : {session?.roomName}</p>
          <div style={{ width: '100%', height: 300, backgroundColor: 'black', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            [Flux LiveKit]
          </div>
          <button onClick={() => setIsLive(false)} className="btn btn-outline" style={{ marginTop: 10 }}>
            Arrêter le direct
          </button>
        </div>
      )}
    </div>
  );
};
