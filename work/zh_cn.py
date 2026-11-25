import os, re, json, sys

ROOT = r'<PROJECT_ROOT>\upgrade-417\alma-417\smali'

# 只替换明确的界面文案；键是英文原文（不含引号），值是中文。
# 不含：资源名、路由、错误码、类名、日志。
UI_MAP = {
    # 主界面 / 分段控件
    "Chat": "对话",
    "Chats": "对话",
    "Routines": "例程",
    "Recents": "最近",
    "Projects": "项目",
    "Settings": "设置",
    "Search": "搜索",
    "Show Sidebar": "显示侧栏",
    "About you": "关于你",
    "Chats and search": "对话与搜索",
    # 通用按钮
    "Cancel": "取消",
    "Done": "完成",
    "Close": "关闭",
    "Back": "返回",
    "Exit": "退出",
    "Try Again": "重试",
    "Save": "保存",
    "Delete": "删除",
    "Edit": "编辑",
    "Copy": "复制",
    "Share": "分享",
    "Retry": "重试",
    "Continue": "继续",
    "Confirm": "确认",
    "Off": "关",
    "On": "开",
    "Dark": "深色",
    "Light": "浅色",
    "System": "跟随系统",
    # 状态
    "Connecting\u2026": "连接中…",
    "Connecting...": "连接中…",
    "Booted": "已启动",
    "Empty folder": "空文件夹",
    "App Switcher": "应用切换",
    # 错误
    "Couldn't create the conversation.": "无法创建对话",
    "Couldn't start the conversation": "无法开始对话",
    "Couldn't load files": "无法加载文件",
    # 引导页
    "Your computer's agent, in your pocket.": "你的 AI 助手，就在手机里。",
    "Scan QR code": "扫码连接",
    "Paste a pairing link": "粘贴配对链接",
    "On your computer, open Alma \u2192 Settings \u2192 Remote Access.":
        "在本机服务已自动就绪，无需扫码。",
    # 输入区
    "按住 说话": "输入消息…",
    # 设置项
    "Line input": "逐行输入",
    "Read Alma's replies aloud": "朗读 Alma 的回复",
    "Models available": "可用模型",
    "Update to ": "更新到 ",
    "About": "关于",
    "Appearance": "外观",
    "Terminal": "终端",
    "Voice": "语音",
    "Same as Computer": "与电脑一致",
}

# 需要跳过的上下文（避免误伤）
SKIP_PAT = re.compile(r'(?i)(serialName|route|path|url|http|\.smali|permission|MIME|'
                      r'content://|file://|android\.|Ljava/|Lkotlin/|Log\.)')

def is_codeish(line):
    return bool(SKIP_PAT.search(line))

def process(path):
    with open(path, encoding='utf-8') as f:
        src = f.read()
    out_lines = []
    changes = 0
    for line in src.split('\n'):
        if 'const-string' not in line or is_codeish(line):
            out_lines.append(line)
            continue
        m = re.match(r'^(\s*const-string(?:/jumbo)?\s+[pv]\d+,\s*)"(.*)"(\s*)$', line)
        if not m:
            out_lines.append(line)
            continue
        pre, val, post = m.group(1), m.group(2), m.group(3)
        if val in UI_MAP:
            new = UI_MAP[val]
            out_lines.append(f'{pre}"{new}"{post}')
            changes += 1
        else:
            out_lines.append(line)
    if changes:
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(out_lines))
    return changes

total = 0
files = 0
for base, _, names in os.walk(ROOT):
    for n in names:
        if not n.endswith('.smali'):
            continue
        p = os.path.join(base, n)
        try:
            c = process(p)
        except Exception as e:
            print('ERR', n, e)
            continue
        if c:
            files += 1
            total += c
            print(f'{c:4d}  {os.path.relpath(p, ROOT)}')

print(f'\nTOTAL: {total} strings in {files} files')