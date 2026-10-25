import React, { useState } from 'react';
import { Camera, Upload, X } from 'lucide-react';

export const CreateModal = ({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) => {
  const [duration, setDuration] = useState('15s');
  
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4">
      <div className="relative w-full max-w-md rounded-2xl bg-gray-900 p-6 text-white">
        <button onClick={onClose} className="absolute right-4 top-4 text-gray-400 hover:text-white">
          <X />
        </button>
        
        <h2 className="mb-6 text-xl font-bold">Studio de Création</h2>

        <div className="mb-6 aspect-video w-full rounded-lg bg-gray-800 flex items-center justify-center">
            <Camera className="h-12 w-12 text-gray-600" />
            <p className="ml-2 text-gray-400">Viseur caméra</p>
        </div>

        <div className="mb-6 flex gap-2">
          {['15s', '60s', '3min'].map((d) => (
            <button
              key={d}
              onClick={() => setDuration(d)}
              className={`rounded-full px-4 py-1 text-sm ${duration === d ? 'bg-amber-500 text-black' : 'bg-gray-800'}`}
            >
              {d}
            </button>
          ))}
        </div>

        <div className="flex gap-4">
          <label className="flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-lg bg-gray-800 py-3 hover:bg-gray-700">
            <Upload size={20} />
            Importer
            <input type="file" accept="video/*,image/*" className="hidden" />
          </label>
          <button className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-amber-500 py-3 font-bold text-black">
            <Camera size={20} />
            Filmer
          </button>
        </div>
      </div>
    </div>
  );
};
