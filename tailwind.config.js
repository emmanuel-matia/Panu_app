/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        panu: {
          gold: '#E5A93C',
          dark: '#0B0C12',
          surface: '#10121A',
          card: '#151722',
        },
      },
    },
  },
  plugins: [],
}
