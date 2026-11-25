#!/usr/bin/env python3
import json, urllib.request

BASE = "http://127.0.0.1:23001"

def req(method, path, body=None):
    r = urllib.request.Request(BASE + path, method=method)
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        r.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(r, data=data, timeout=30) as resp:
        return resp.status, json.loads(resp.read().decode())

# 1. create thread
st, t = req("POST", "/api/threads", {"title": "原生界面测试", "model": "none"})
tid = t.get("id")
print("create:", st, "id:", tid, "title:", t.get("title"))

# 2. list threads
st, lst = req("GET", "/api/threads?limit=3")
print("threads:", st, "count:", len(lst))

# 3. messages (empty)
st, msgs = req("GET", "/api/threads/%s/messages" % tid)
print("messages empty:", st, "count:", len(msgs) if isinstance(msgs, list) else msgs)

# 4. send user message
st, r = req("POST", "/api/threads/%s/send-text" % tid, {"text": "你好，这是原生界面测试"})
print("send:", st, json.dumps(r, ensure_ascii=False)[:300])

# 5. messages after
st, msgs = req("GET", "/api/threads/%s/messages" % tid)
if isinstance(msgs, list):
    print("messages:", st, "count:", len(msgs))
    for m in msgs:
        print("  role=%s content=%s" % (m.get("role"), str(m.get("content"))[:80]))

# 6. websocket events endpoint metadata (subscribe requires ws; just check route exists)
import socket
sock = socket.create_connection(("127.0.0.1", 23001), timeout=5)
sock.close()
print("ws endpoint reachable")
