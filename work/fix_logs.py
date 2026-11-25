p = r'<PROJECT_ROOT>\almadroid\server-java\io\almadroid\server\LocalService.java'
s = open(p, encoding='utf-8').read()

old = '''        File tmp = new File(getFilesDir(), "tmp");
        File home = new File(getFilesDir(), "home");
        File data = new File(getFilesDir(), "data");
        for (File f : new File[]{tmp, home, data}) f.mkdirs();'''
new = '''        File tmp = new File(getFilesDir(), "tmp");
        File home = new File(getFilesDir(), "home");
        File data = new File(getFilesDir(), "data");
        File logs = new File(getFilesDir(), "logs");
        for (File f : new File[]{tmp, home, data, logs}) f.mkdirs();'''

if old in s:
    s = s.replace(old, new, 1)
    print('logs declared at 167-170 block')
else:
    print('pattern not found; showing context:')
    i = s.find('File tmp = new File(getFilesDir(), "tmp")')
    print(s[i - 100:i + 500])
open(p, 'w', encoding='utf-8').write(s)