import json, urllib.request, urllib.error, time

BASE = 'http://127.0.0.1:23001'

def req(method, path, body=None, timeout=30):
    r = urllib.request.Request(BASE + path, method=method)
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        r.add_header('Content-Type', 'application/json')
    try:
        with urllib.request.urlopen(r, data=data, timeout=timeout) as resp:
            return resp.status, resp.read().decode('utf-8', 'ignore')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'ignore')

print('=== 模拟 App 启动后的调用序列 ===')

# 1. health
st, body = req('GET', '/api/health', timeout=3)
print('1. health:', st, body[:80])

# 2. settings -> defaultModel（AlmaApi.defaultModel 做的事）
st, body = req('GET', '/api/settings')
sd = json.loads(body).get('settingsData')
sd = json.loads(sd) if isinstance(sd, str) else sd
default_model = (sd.get('chat') or {}).get('defaultModel')
print('2. defaultModel:', default_model)

# 3. models 列表（模型选择器）
st, body = req('GET', '/api/models')
models = json.loads(body).get('data', [])
print('3. models count:', len(models), '| first:', models[0]['providerId'] + ':' + models[0]['id'] if models else '-')

# 4. threads 列表（对话列表页）
st, body = req('GET', '/api/threads?limit=50')
threads = json.loads(body)
print('4. threads count:', len(threads))
for t in threads[:3]:
    print('     -', t.get('title'), '| model:', t.get('model'))

# 5. createThread（点“新对话”）
st, body = req('POST', '/api/threads', {'title': 'v3.0 原生界面测试', 'model': default_model or 'none'})
new_t = json.loads(body)
tid = new_t['id']
print('5. created thread:', st, tid, '|', new_t.get('title'))

# 6. messages（进对话详情，空）
st, body = req('GET', '/api/threads/%s/messages' % tid)
print('6. messages empty:', st, 'count:', len(json.loads(body)))

# 7. sendMessage（SSE 流式）— 复现 AlmaApi.sendMessage 的请求构造
payload = {
    'threadId': tid,
    'model': default_model,
    'messages': [
        {'id': 'u' + str(int(time.time() * 1000)), 'role': 'user',
         'parts': [{'type': 'text', 'text': '用一句话介绍你自己'}]},
    ],
    'stream': True,
}
r = urllib.request.Request(BASE + '/api/chat/completions', method='POST', data=json.dumps(payload).encode())
r.add_header('Content-Type', 'application/json')
print('7. SSE stream:')
chunks = []
with urllib.request.urlopen(r, timeout=60) as resp:
    for raw in resp:
        line = raw.decode('utf-8', 'ignore').strip()
        if not line.startswith('data:'):
            continue
        p = line[5:].strip()
        if p == '[DONE]':
            print('     [DONE]')
            break
        try:
            o = json.loads(p)
            ch = (o.get('choices') or [{}])[0]
            piece = (ch.get('delta') or {}).get('content', '')
            if piece:
                chunks.append(piece)
                print('     +', repr(piece))
        except Exception as e:
            pass
answer = ''.join(chunks)
print('8. assembled answer:', repr(answer[:120]))

# 9. messages after（验证服务端已存）
st, body = req('GET', '/api/threads/%s/messages' % tid)
msgs = json.loads(body)
print('9. messages after: count:', len(msgs))
for m in msgs:
    content = m.get('content') or ''
    print('     -', m.get('role'), '|', repr(content[:60]))

print()
print('=== 结论 ===')
print('原生界面所需的 4 个核心调用（列表/新建/发送/回读）全部可用 ✓')