import { create } from 'zustand';

interface VideoState {
  activeVideoIndex: number;
  isPlaying: boolean;
  setActiveVideoIndex: (index: number) => void;
  setIsPlaying: (isPlaying: boolean) => void;
}

export const useVideoStore = create<VideoState>((set) => ({
  activeVideoIndex: 0,
  isPlaying: true,
  setActiveVideoIndex: (index) => set({ activeVideoIndex: index }),
  setIsPlaying: (isPlaying) => set({ isPlaying }),
}));
