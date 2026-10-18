const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const { autoUpdater } = require('electron-updater');

let mainWindow;

function sendUpdateStatus(type, payload = {}) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('update-status', { type, ...payload });
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 980,
    height: 720,
    minWidth: 760,
    minHeight: 560,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.loadFile('index.html');
}

function configureUpdater() {
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;

  autoUpdater.on('checking-for-update', () => sendUpdateStatus('checking'));
  autoUpdater.on('update-available', info => sendUpdateStatus('available', { version: info.version }));
  autoUpdater.on('update-not-available', info => sendUpdateStatus('current', { version: info.version }));
  autoUpdater.on('download-progress', progress => {
    sendUpdateStatus('downloading', { percent: Math.round(progress.percent || 0) });
  });
  autoUpdater.on('update-downloaded', info => {
    sendUpdateStatus('ready', { version: info.version });
  });
  autoUpdater.on('error', error => {
    sendUpdateStatus('error', { message: error?.message || 'Erro ao procurar atualização.' });
  });

  if (app.isPackaged) {
    setTimeout(() => autoUpdater.checkForUpdatesAndNotify(), 5000);
    setInterval(() => autoUpdater.checkForUpdatesAndNotify(), 6 * 60 * 60 * 1000);
  }
}

app.whenReady().then(() => {
  createWindow();
  configureUpdater();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

ipcMain.handle('app-version', () => app.getVersion());
ipcMain.handle('check-for-updates', async () => {
  if (!app.isPackaged) return { ok: false, message: 'Atualizações automáticas só funcionam na versão instalada.' };
  const result = await autoUpdater.checkForUpdatesAndNotify();
  return { ok: true, version: result?.updateInfo?.version || null };
});

ipcMain.handle('install-update', () => {
  if (app.isPackaged) {
    autoUpdater.quitAndInstall(false, true);
    return { ok: true };
  }
  return { ok: false };
});
