'use client';

import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';

export type ThemeName = 'dark' | 'state-color-check';

interface ThemeContextValue {
  theme: ThemeName;
  setTheme: (t: ThemeName) => void;
}

const ThemeContext = createContext<ThemeContextValue>({
  theme: 'dark',
  setTheme: () => {},
});

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeName>(() => {
    if (typeof window !== 'undefined') {
      return (localStorage.getItem('wiom_theme') as ThemeName) || 'dark';
    }
    return 'dark';
  });

  const setTheme = useCallback((newTheme: ThemeName) => {
    setThemeState(newTheme);
    if (typeof window !== 'undefined') {
      localStorage.setItem('wiom_theme', newTheme);
    }
    // Persist to server so the Android app picks it up
    fetch('/api/theme', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ theme: newTheme }),
    }).catch(() => {});
  }, []);

  // Apply data-theme attribute on <html>
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'state-color-check') {
      root.setAttribute('data-theme', 'state-color-check');
    } else {
      root.removeAttribute('data-theme');
    }
  }, [theme]);

  // Also listen for localStorage changes from other tabs
  useEffect(() => {
    const handler = (e: StorageEvent) => {
      if (e.key === 'wiom_theme' && e.newValue) {
        setThemeState(e.newValue as ThemeName);
      }
    };
    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
