p = r'<PROJECT_ROOT>\work\repack\alma-final\smali\me\yetone\alma\app\LocalDeepLinker.smali'
s = open(p, encoding='utf-8').read()

old = '''.method public run()V
    .locals 6

    # 等 45 秒让服务器完全就绪
    const-wide/16 v0, 0x2ee0

    :try_start
    invoke-static {v0, v1}, Ljava/lang/Thread;->sleep(J)V
    :try_end
    .catch Ljava/lang/InterruptedException; {:try_start .. :try_end} :catch

    iget-object v2, p0, Lme/yetone/alma/app/LocalDeepLinker;->ctx:Landroid/content/Context;

    invoke-static {v2}, Lme/yetone/alma/app/LocalBootstrap;->fireDeepLink(Landroid/content/Context;)V

    return-void

    :catch
    return-void
.end method'''

new = '''.method public run()V
    .locals 6

    # 循环重试 20 次 x 30 秒（覆盖首次解包 3 分钟 + 服务启动），直到 App 进入主界面
    const/16 v5, 0x14

    :loop
    # sleep 30s
    const-wide/16 v0, 0x7530

    :try_start_s
    invoke-static {v0, v1}, Ljava/lang/Thread;->sleep(J)V
    :try_end_s
    .catch Ljava/lang/InterruptedException; {:try_start_s .. :try_end_s} :catch_s

    iget-object v2, p0, Lme/yetone/alma/app/LocalDeepLinker;->ctx:Landroid/content/Context;

    invoke-static {v2}, Lme/yetone/alma/app/LocalBootstrap;->fireDeepLink(Landroid/content/Context;)V

    add-int/lit8 v5, v5, -0x1

    if-lez v5, :done

    goto :loop

    :catch_s
    return-void

    :done
    return-void
.end method'''

assert old in s, 'old run() not found'
s = s.replace(old, new)
open(p, 'w', encoding='utf-8').write(s)
print('LocalDeepLinker: retry loop added (20 x 30s)')