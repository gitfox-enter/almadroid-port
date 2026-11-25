import os, re

ROOT = r'<PROJECT_ROOT>\upgrade-417\alma-417\smali'

# 第二批：明确的界面文案（不含代码枚举/异常/协议字段）
MAP = {
    "Connection log": "连接日志",
    "New Chat": "新对话",
    "New chat": "新对话",
    "Send": "发送",
    "Rename": "重命名",
    "Upgrade": "升级",
    "Thinking": "思考中",
    "Ready": "就绪",
    "Working": "运行中",
    "Running": "运行中",
    "Stopped": "已停止",
    "Failed": "失败",
    "Canceled": "已取消",
    "Skipped": "已跳过",
    "Agents": "智能体",
    "Agent": "智能体",
    "Subagent": "子智能体",
    "Model": "模型",
    "Models": "模型",
    "Status": "状态",
    "Task": "任务",
    "Tasks": "任务",
    "Orientation": "方向",
    "Connection": "连接",
    "Enter": "回车",
    "Write": "写入",
    "Read": "读取",
    "Start": "开始",
    "Stop": "停止",
    "Pause": "暂停",
    "Clear": "清空",
    "Refresh": "刷新",
    "Reload": "重载",
    "Open": "打开",
    "Add": "添加",
    "Remove": "移除",
    "Rename Chat": "重命名对话",
    "Delete Chat": "删除对话",
    "Messages": "消息",
    "Message": "消息",
    "Type a message…": "输入消息…",
    "Type a message...": "输入消息…",
    "Message Alma": "给 Alma 发消息",
    "Send a message": "发送消息",
    "No messages yet": "还没有消息",
    "Nothing here yet": "这里还没有内容",
    "Connect": "连接",
    "Disconnect": "断开",
    "Connected": "已连接",
    "Connecting": "连接中",
    "Offline": "离线",
    "Online": "在线",
    "Loading…": "加载中…",
    "Loading...": "加载中…",
    "Please wait…": "请稍候…",
    "Please wait...": "请稍候…",
    "Something went wrong": "出错了",
    "Error": "错误",
    "Warning": "警告",
    "Notice": "提示",
    "Confirm deletion": "确认删除",
    "Are you sure?": "确定吗？",
    "Yes": "是",
    "No": "否",
    "OK": "好",
    "Okay": "好",
    "Later": "稍后",
    "Skip": "跳过",
    "Next": "下一步",
    "Previous": "上一步",
    "Finish": "完成",
    "Continue to app": "进入应用",
    "Get started": "开始使用",
}

SKIP = re.compile(r'(?i)(serialName|route|path|url|http|\.smali|permission|MIME|'
                  r'content://|file://|android\.|Ljava/|Lkotlin/|Log\.|'
                  r'sqlite|SELECT|INSERT|/api/)')

def is_codeish(line):
    return bool(SKIP.search(line))

def process(path):
    with open(path, encoding='utf-8') as f:
        src = f.read()
    out = []
    n = 0
    for line in src.split('\n'):
        if 'const-string' not in line or is_codeish(line):
            out.append(line)
            continue
        m = re.match(r'^(\s*const-string(?:/jumbo)?\s+[pv]\d+,\s*)"(.*)"(\s*)$', line)
        if not m:
            out.append(line)
            continue
        pre, val, post = m.groups()
        # 跳过含转义/占位符的复合串
        if '\\' in val or '{' in val:
            out.append(line)
            continue
        if val in MAP:
            out.append(f'{pre}"{MAP[val]}"{post}')
            n += 1
        else:
            out.append(line)
    if n:
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(out))
    return n

total = files = 0
for base, _, names in os.walk(ROOT):
    for name in names:
        if not name.endswith('.smali'):
            continue
        p = os.path.join(base, name)
        c = process(p)
        if c:
            files += 1
            total += c
            print(f'{c:4d}  {os.path.relpath(p, ROOT)}')
print(f'\nTOTAL {total} in {files} files')