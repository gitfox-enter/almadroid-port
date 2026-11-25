import re

p = r'<PROJECT_ROOT>\work\repack\alma-final\smali\me\yetone\alma\app\LocalBootstrap.smali'
s = open(p, encoding='utf-8').read()

# 1) remove the invalid getBytes line
bad = '    invoke-virtual {v2}, Ljava/lang/String;->getBytes()Ljava/lang/ByteArray; 2>/dev/null || nop\n'
if bad in s:
    s = s.replace(bad, '')
    print('removed invalid line')

# 2) add trigger thread start at end of start()
old_start_tail = '''    # 2) 拉起 proot + alma-server
    invoke-static {p0}, Lme/yetone/alma/app/LocalBootstrap;->startServer(Landroid/content/Context;)V

    :try_end'''
new_start_tail = '''    # 2) 拉起 proot + alma-server
    invoke-static {p0}, Lme/yetone/alma/app/LocalBootstrap;->startServer(Landroid/content/Context;)V

    # 3) 延时触发 deep link（等服务就绪）
    new-instance v1, Lme/yetone/alma/app/LocalDeepLinker;
    invoke-direct {v1, p0}, Lme/yetone/alma/app/LocalDeepLinker;-><init>(Landroid/content/Context;)V
    invoke-virtual {v1}, Lme/yetone/alma/app/LocalDeepLinker;->start()V

    :try_end'''
if old_start_tail in s:
    s = s.replace(old_start_tail, new_start_tail)
    print('deep link trigger added to start()')

open(p, 'w', encoding='utf-8').write(s)