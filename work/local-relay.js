#!/usr/bin/env node
/**
 * AlmaDroid local relay shim.
 *
 * 官方 App 的连接路径固定是：  ws://<host>:<port>/c/<routingHash>/__alma_e2e__
 * 而 alma-server 的 E2E 监听器只认：  ws://127.0.0.1:23019/__alma_e2e__
 *
 * 本脚本在本机扮演「中继」的角色：把任意 /c/.../__alma_e2e__ 的 WebSocket
 * 原样桥接到 23019 的 /__alma_e2e__，让官方 App 直连本机、不经过云端中继。
 *
 * 鉴权不依赖 URL：E2E 隧道在握手协议内用配对码派生密钥，路径里的 hash 只是
 * relay 用来选路的标识，本机只有一个服务，可以直接忽略。
 *
 * 用法：node local-relay.js [port]   (默认 23017)
 */
const http = require('http');
const net = require('net');
const path = require('path');

function loadWs() {
  // 优先用 alma-server 自带的 ws，避免额外依赖
  const candidates = [
    path.join(__dirname, 'ws'),
    path.join(__dirname, 'node_modules', 'ws'),
    'ws',
  ];
  for (const c of candidates) {
    try {
      return require(c);
    } catch (_) { /* try next */ }
  }
  throw new Error('ws module not found');
}

const WebSocket = loadWs();
const WebSocketServer = WebSocket.WebSocketServer || WebSocket.Server;

const LISTEN_PORT = parseInt(process.argv[2] || process.env.ALMA_LOCAL_RELAY_PORT || '23017', 10);
const TARGET_PORT = parseInt(process.env.ALMA_E2E_PORT || '23019', 10);
const TARGET_PATH = '/__alma_e2e__';

const log = (...a) => console.log('[local-relay]', ...a);

// ---- 1. 普通 HTTP：把 /api/* 等直接反代到 23001，方便官方 App 的 REST 调用 ----
const API_PORT = parseInt(process.env.ALMA_API_PORT || '23001', 10);

const server = http.createServer((req, res) => {
  const upstream = http.request({
    host: '127.0.0.1',
    port: API_PORT,
    path: req.url,
    method: req.method,
    headers: { ...req.headers, host: `127.0.0.1:${API_PORT}` },
  }, (up) => {
    res.writeHead(up.statusCode || 502, up.headers);
    up.pipe(res);
  });
  upstream.on('error', (e) => {
    log('http proxy error:', e.message);
    if (!res.headersSent) res.writeHead(502, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ error: 'local relay upstream failed: ' + e.message }));
  });
  req.pipe(upstream);
});

// ---- 2. WebSocket：把 /c/*/__alma_e2e__ 桥接到 23019 的 /__alma_e2e__ ----
const wss = new WebSocketServer({ noServer: true, maxPayload: 64 * 1024 * 1024 });

server.on('upgrade', (req, socket, head) => {
  const url = (req.url || '').split('?')[0];
  const isE2E = url.includes('__alma_e2e__');
  if (!isE2E) {
    log('upgrade rejected (not e2e):', url);
    socket.destroy();
    return;
  }
  wss.handleUpgrade(req, socket, head, (client) => {
    log('client attached:', url);
    const upstream = new WebSocket(`ws://127.0.0.1:${TARGET_PORT}${TARGET_PATH}`);

    const pending = [];
    let upstreamOpen = false;

    upstream.on('open', () => {
      upstreamOpen = true;
      log('upstream connected');
      for (const msg of pending.splice(0)) {
        upstream.send(msg.data, { binary: msg.binary });
      }
    });

    client.on('message', (data, isBinary) => {
      if (upstreamOpen) {
        upstream.send(data, { binary: isBinary });
      } else {
        pending.push({ data, binary: isBinary });
      }
    });

    upstream.on('message', (data, isBinary) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data, { binary: isBinary });
      }
    });

    const closeBoth = (why) => {
      log('closing:', why);
      try { client.close(); } catch (_) {}
      try { upstream.close(); } catch (_) {}
    };

    client.on('close', () => closeBoth('client closed'));
    client.on('error', (e) => closeBoth('client error: ' + e.message));
    upstream.on('close', () => closeBoth('upstream closed'));
    upstream.on('error', (e) => closeBoth('upstream error: ' + e.message));
  });
});

server.listen(LISTEN_PORT, '0.0.0.0', () => {
  log(`listening on 0.0.0.0:${LISTEN_PORT}`);
  log(`  REST  -> 127.0.0.1:${API_PORT}`);
  log(`  WS    -> 127.0.0.1:${TARGET_PORT}${TARGET_PATH}`);
});

// 健康自检：确认 23019 是否在听
function probeE2E() {
  const s = net.connect(TARGET_PORT, '127.0.0.1');
  s.on('connect', () => { log('E2E listener 23019 is UP'); s.destroy(); });
  s.on('error', (e) => log('E2E listener 23019 is DOWN:', e.code));
}
setTimeout(probeE2E, 1500);
setInterval(probeE2E, 30000).unref();