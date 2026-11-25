import os, re

ROOT = r'<PROJECT_ROOT>\upgrade-417\alma-417\smali'

# 协议/库包绝对不动（上次误伤 Upgrade/Connection 头的教训）
LIB_DIRS = ('org' + os.sep, 'okhttp3' + os.sep, 'kotlinx' + os.sep,
            'A6' + os.sep, 'x6' + os.sep, 'v6' + os.sep, 'I6' + os.sep,
            'B' + os.sep, 'L0' + os.sep)

MAP = {
    # 设置页高频项
    "General": "通用",
    "Appearance": "外观",
    "Terminal": "终端",
    "Voice": "语音",
    "About": "关于",
    "Version": "版本",
    "Models": "模型",
    "Models available": "可用模型",
    "Remote Access": "远程访问",
    "Notifications": "通知",
    "Language": "语言",
    "Theme": "主题",
    "Font": "字体",
    "Font size": "字号",
    "Color scheme": "配色",
    "Sound": "声音",
    "Haptics": "震动",
    "Privacy": "隐私",
    "Security": "安全",
    "Storage": "存储",
    "Network": "网络",
    "Proxy": "代理",
    "Account": "账户",
    "Sign out": "退出登录",
    "Sign in": "登录",
    "Manage": "管理",
    "Devices": "设备",
    "Computer": "电脑",
    "This computer": "本机",
    "Screen": "屏幕",
    "Screen sharing": "屏幕共享",
    "Unavailable": "不可用",
    "Not available": "不可用",
    "Unavailable on this device": "本机不可用",
    "Screen not available": "屏幕不可用",
    "Screen sharing is not available": "屏幕共享不可用",
    "Computer screen unavailable": "本机无桌面可共享",
    "Same as Computer": "与本机一致",
    "Line input": "逐行输入",
    "Read Alma's replies aloud": "朗读回复",
    "When you hold to talk, she answers out loud": "按住说话时，将语音回复",
    "Update to ": "检查更新（已停用）",
    "Restart to Update": "重启以更新（已停用）",
    "Check for updates": "检查更新",
    "Auto-update": "自动更新",
    "Advanced": "高级",
    "Developer": "开发者",
    "Diagnostics": "诊断",
    "Connection log": "连接日志",
    "Clear log": "清空日志",
    "Threads": "对话",
    "New Chat": "新对话",
    "New chat": "新对话",
    "Send": "发送",
    "Type a message": "输入消息",
    "Hold to talk": "按住说话",
    "Release to send": "松手发送",
    "Attachments": "附件",
    "Camera": "相机",
    "Photo": "照片",
    "File": "文件",
    "Mic": "麦克风",
    "Microphone": "麦克风",
    "Permission": "权限",
    "Permissions": "权限",
    "Allow": "允许",
    "Deny": "拒绝",
    "Routines": "例程",
    "Routine": "例程",
    "Schedule": "计划",
    "Projects": "项目",
    "Recents": "最近",
    "Chats": "对话",
    "Search": "搜索",
    "No results": "无结果",
    "Nothing found": "没有找到",
    "Today": "今天",
    "Yesterday": "昨天",
    "Earlier": "更早",
    "Untitled": "未命名",
    "Untitled chat": "未命名对话",
    "Delete chat": "删除对话",
    "Rename chat": "重命名对话",
    "Pin": "置顶",
    "Unpin": "取消置顶",
    "Archive": "归档",
    "Unarchive": "取消归档",
    "Share": "分享",
    "Export": "导出",
    "Import": "导入",
    "Copy": "复制",
    "Cut": "剪切",
    "Paste": "粘贴",
    "Select all": "全选",
    "Done": "完成",
    "Save": "保存",
    "Cancel": "取消",
    "Close": "关闭",
    "Back": "返回",
    "Confirm": "确认",
    "Continue": "继续",
    "Retry": "重试",
    "Try Again": "重试",
    "Loading": "加载中",
    "Sending": "发送中",
    "Thinking": "思考中",
    "Working": "运行中",
    "Running": "运行中",
    "Ready": "就绪",
    "Stopped": "已停止",
    "Failed": "失败",
    "Connected": "已连接",
    "Connecting": "连接中",
    "Disconnected": "已断开",
    "Reconnecting": "重连中",
    "Local": "本机",
    "Desktop": "桌面",
    "Mobile": "手机",
    "On": "开",
    "Off": "关",
    "Enabled": "已启用",
    "Disabled": "已禁用",
    "Default": "默认",
    "Custom": "自定义",
    "Automatic": "自动",
    "Manual": "手动",
    "Learn more": "了解更多",
    "Feedback": "反馈",
    "Help": "帮助",
    "Docs": "文档",
    "Terms": "条款",
    "Licenses": "许可",
    "Open source": "开源",
}

# 只在这些（Compose UI）包里替换
UI_DIRS_PREFIX = ('i6' + os.sep, 'q6' + os.sep, 'l6' + os.sep, 'm6' + os.sep,
                  'p6' + os.sep, 'u6' + os.sep, 's6' + os.sep, 'j6' + os.sep,
                  'r6' + os.sep, 'h6' + os.sep, 'k6' + os.sep, 't6' + os.sep,
                  'g6' + os.sep, 'n6' + os.sep, 'z6' + os.sep, 'x2.1' + os.sep)

STRIP_RE = re.compile(r'^\s*const-string(?:/jumbo)?\s+[pv]\d+,\s*"(.*)"\s*$')

def in_ui(relpath):
    return relpath.startswith(UI_DIRS_PREFIX)

def main():
    total = files = 0
    for base, _, names in os.walk(ROOT):
        rel_dir = os.path.relpath(base, ROOT)
        if not in_ui(rel_dir + os.sep) and rel_dir != '.':
            continue
        for n in names:
            if not n.endswith('.smali'):
                continue
            p = os.path.join(base, n)
            rel = os.path.relpath(p, ROOT)
            if any(rel.startswith(d) for d in LIB_DIRS):
                continue
            try:
                src = open(p, encoding='utf-8').read()
            except Exception:
                continue
            out, changed = [], 0
            for line in src.split('\n'):
                m = STRIP_RE.match(line)
                if m and m.group(1) in MAP and '\\' not in m.group(1):
                    out.append(line[:m.start(1)] + MAP[m.group(1)] + line[m.end(1):])
                    changed += 1
                else:
                    out.append(line)
            if changed:
                open(p, 'w', encoding='utf-8').write('\n'.join(out))
                files += 1
                total += changed
    print('TOTAL', total, 'in', files, 'files')

main()