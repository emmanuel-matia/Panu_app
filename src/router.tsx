import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import HomePage from './pages/HomePage';
import StudioPage from './pages/StudioPage';
import VodPage from './pages/VodPage';
import LiveSportsPage from './pages/LiveSportsPage';
import ProfilePage from './pages/ProfilePage';
import LoginPage from './pages/LoginPage';
import VerifyCardPage from './pages/VerifyCardPage';
import PaymentPage from './pages/PaymentPage';

/**
 * Configuration des routes PANU (Français exclusif)
 * Les barres de navigation et l'entête sont gérés globalement dans App.tsx
 */
export interface PanuAppRouterProps {
  searchQuery?: string;
}

export const PanuAppRouter: React.FC<PanuAppRouterProps> = ({ searchQuery = '' }) => {
  return (
    <Routes>
      <Route path="/" element={<HomePage searchQuery={searchQuery} />} />
      <Route path="/home" element={<HomePage searchQuery={searchQuery} />} />
      <Route path="/studio" element={<StudioPage />} />
      <Route path="/vod" element={<VodPage />} />
      <Route path="/live" element={<LiveSportsPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/payment" element={<PaymentPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/verify/:cardNumber" element={<VerifyCardPage />} />
      <Route path="/verify" element={<Navigate to="/verify/PANU-FND-001" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default PanuAppRouter;
