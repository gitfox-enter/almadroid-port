import os, re

ROOT = r'<PROJECT_ROOT>\upgrade-417\alma-417\smali'
LIB_DIRS = ('org' + os.sep, 'okhttp3' + os.sep, 'kotlinx' + os.sep,
            'A6' + os.sep, 'x6' + os.sep, 'v6' + os.sep, 'I6' + os.sep,
            'B' + os.sep, 'L0' + os.sep)

# 沿用 zh_cn3 的 UI 包范围
UI_DIRS_PREFIX = ('i6' + os.sep, 'q6' + os.sep, 'l6' + os.sep, 'm6' + os.sep,
                  'k6' + os.sep, 's6' + os.sep, 'u6' + os.sep, 'j6' + os.sep,
                  't6' + os.sep, 'p6' + os.sep, 'n6' + os.sep, 'w5' + os.sep,
                  'z5' + os.sep, 't5' + os.sep, 'x5' + os.sep, 'y5' + os.sep,
                  'f0' + os.sep, 'S5' + os.sep, 'A1' + os.sep, 'H0' + os.sep,
                  'B2' + os.sep, 'C6' + os.sep, 'l0' + os.sep, 'm6' + os.sep)

MAP = {
    # 工作区（智能体页）
    "No workspaces": "暂无工作区",
    "New Workspace": "新建工作区",
    "Workspace": "工作区",
    "Workspaces": "工作区",
    "Open a folder on your computer to start an agent in it.": "打开一个文件夹，即可在其中启动智能体。",
    "No project": "无项目",
    "Add a Device": "添加设备",
    # 会话/输入
    "Start a new conversation": "开始新对话",
    "Start a new session": "开始新会话",
    "Type something": "输入内容",
    "Choose the model": "选择模型",
    "Switch model for this session": "为本次会话切换模型",
    "Summarize the conversation to free up context": "总结对话以释放上下文",
    "Review the current changes": "查看当前改动",
    "Compacted context": "已压缩上下文",
    "Not connected to the computer.": "未连接到电脑。",
    "Not connected to the computer": "未连接到电脑",
    # 状态词
    "Completed": "已完成",
    "Missing": "缺失",
    "Active": "进行中",
    "Ended": "已结束",
    "Edited": "已编辑",
    "Collapsed": "已折叠",
    "Expanded": "已展开",
    "Already resumed": "已在运行",
    "The task was rejected": "任务被拒绝",
    "Background command": "后台命令",
    "Recap": "摘要",
    "Nothing": "无内容",
    "Unsupported": "不支持",
    "Other": "其他",
    "Unknown": "未知",
    "Invalid": "无效",
    "Unspecified": "未指定",
    "Failed": "失败",
    "Running": "运行中",
    "Pending": "等待中",
    "Ready": "就绪",
    # 电脑/窗口
    "Control Computer": "控制电脑",
    "Window": "窗口",
    "Display": "显示",
    "Simulators": "模拟器",
    "Monitor": "显示器",
    "Command": "命令",
    "Control": "控制",
    "Browser": "浏览器",
    "Favorites": "收藏",
    "Forget": "遗忘",
    "Reply": "回复",
    "New Tab": "新标签页",
    "Hide Keyboard": "收起键盘",
    "Dismiss": "关闭",
    "Image": "图片",
    "Files": "文件",
    "Not a readable image.": "不是可读取的图片。",
    "Local network": "局域网",
    "Update Alma on your computer to send files.": "请把电脑上的 Alma 更新到新版后即可发送文件。",
    "Allow Alma to use the microphone and speech recognition in Settings.": "请在系统设置中允许 Alma 使用麦克风与语音识别。",
    "The computer answered with HTTP ": "电脑返回 HTTP ",
    "HTTP ": "HTTP ",
}

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
