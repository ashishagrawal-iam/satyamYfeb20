// Shared server-side theme store
// Persists to disk so theme survives Next.js hot-reloads

import { readFileSync, writeFileSync, existsSync } from 'fs';
import { join } from 'path';

const THEME_FILE = join(process.cwd(), '.theme-state.json');

function readFromDisk(): string {
  try {
    if (existsSync(THEME_FILE)) {
      const data = JSON.parse(readFileSync(THEME_FILE, 'utf-8'));
      return data.theme || 'dark';
    }
  } catch {
    // File corrupted or missing, fall back to default
  }
  return 'dark';
}

function writeToDisk(theme: string): void {
  try {
    writeFileSync(THEME_FILE, JSON.stringify({ theme }), 'utf-8');
  } catch {
    // Disk write failed, theme will reset on next reload
  }
}

export function getTheme(): string {
  return readFromDisk();
}

export function setTheme(theme: string): void {
  writeToDisk(theme);
}
