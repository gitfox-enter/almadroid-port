package io.almadroid.server;

import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Minimal streaming tar.gz extractor: ustar, GNU long names, PAX headers. */
public final class TarGz {
    private static final String TAG = "TarGz";

    public interface Progress {
        /** bytesRead = 已消费的压缩字节；totalCompressed = 压缩总字节（<=0 表示未知）。 */
        void onBytes(long bytesRead, long totalCompressed);
    }

    /** 带压缩总大小信息的解包，用于展示真实进度。 */
    public static void extract(InputStream gzStream, long totalCompressed, File destDir, Progress progress) throws IOException {
        CountingInputStream cis = new CountingInputStream(gzStream, progress, totalCompressed);
        extract(cis, destDir);
    }

    public static void extract(InputStream gzStream, File destDir, Progress progress) throws IOException {
        extract(gzStream, -1, destDir, progress);
    }

    /** 计数包装：解包过程天然顺序读取，消费的压缩字节 ≈ 进度。 */
    static final class CountingInputStream extends java.io.FilterInputStream {
        private final Progress p;
        private final long total;
        private long read;
        private long lastEmit;

        CountingInputStream(InputStream in, Progress p, long total) {
            super(in);
            this.p = p;
            this.total = total;
        }

        @Override public int read() throws IOException {
            int b = super.read();
            if (b >= 0) bump(1);
            return b;
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) bump(n);
            return n;
        }

        @Override public long skip(long n) throws IOException {
            long s = super.skip(n);
            if (s > 0) bump(s);
            return s;
        }

        private void bump(long n) {
            read += n;
            if (read - lastEmit >= 262144 || read == total) { // 每 256KB 上报一次
                lastEmit = read;
                if (p != null) p.onBytes(read, total);
            }
        }
    }

    private static void extract(InputStream gzStream, File destDir) throws IOException {
        InputStream in = new GZIPInputStream(gzStream, 64 * 1024);
        byte[] header = new byte[512];
        Map<String, String> pax = new HashMap<>();
        String gnuLongName = null;
        long index = 0;
        while (true) {
            if (!readFully(in, header)) break;
            if (isZeroBlock(header)) break; // single trailing zero block is enough for us
            String name = readStr(header, 0, 100);
            int mode = (int) parseOctal(readStr(header, 100, 8), 0);
            long size = parseOctal(readStr(header, 124, 12), 0);
            int typeflag = header[156] & 0xff;
            String linkName = readStr(header, 157, 100);
            String prefix = "";
            if (readStr(header, 257, 6).startsWith("ustar")) {
                String p = readStr(header, 345, 155);
                if (!p.isEmpty()) prefix = p + "/";
            }

            String entryName = prefix + name;
            if (typeflag == 'L') { // GNU long name
                byte[] buf = new byte[(int) size];
                if (!readFully(in, buf)) break;
                gnuLongName = readNullTerminated(buf);
                skipPad(in, size);
                continue;
            }
            if (typeflag == 'x' || typeflag == 'g') { // PAX header
                byte[] buf = new byte[(int) size];
                if (!readFully(in, buf)) break;
                parsePax(buf, pax, typeflag == 'g');
                skipPad(in, size);
                continue;
            }
            if (typeflag == 'K') { // GNU long link name
                byte[] buf = new byte[(int) size];
                if (!readFully(in, buf)) break;
                linkName = readNullTerminated(buf);
                skipPad(in, size);
                continue;
            }
            String paxPath = pax.get("path");
            if (paxPath != null) entryName = paxPath;
            String paxLink = pax.get("linkpath");
            if (paxLink != null) linkName = paxLink;
            String paxSize = pax.get("size");
            if (paxSize != null) {
                try { size = Long.parseLong(paxSize.trim()); } catch (NumberFormatException ignored) {}
            }
            if (gnuLongName != null) {
                entryName = gnuLongName;
                gnuLongName = null;
            }
            pax.clear();

            // sanitize: strip leading ./'s, forbid traversal
            String rel = entryName.replace('\\', '/');
            while (rel.startsWith("./")) rel = rel.substring(2);
            while (rel.startsWith("/")) rel = rel.substring(1);
            if (rel.contains("../") || rel.equals("..")) {
                Log.w(TAG, "skip suspicious entry " + entryName);
                skipData(in, size);
                index++;
                continue;
            }

            File outFile = new File(destDir, rel);
            File parent = outFile.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();

            switch (typeflag) {
                case '0': case 0: { // regular file
                    extractFile(in, outFile, size);
                    applyMode(outFile, mode);
                    break;
                }
                case '5': { // dir
                    outFile.mkdirs();
                    applyMode(outFile, mode);
                    skipData(in, size);
                    break;
                }
                case '2': { // symlink
                    skipData(in, size);
                    try {
                        if (outFile.exists()) outFile.delete();
                        Os.symlink(linkName, outFile.getAbsolutePath());
                    } catch (Throwable t) {
                        Log.w(TAG, "symlink failed " + rel + " -> " + linkName + ": " + t.getMessage());
                    }
                    break;
                }
                case '1': { // hardlink
                    skipData(in, size);
                    File src = new File(destDir, normalizeLink(destDir, linkName));
                    try {
                        if (src.exists()) Os.link(src.getAbsolutePath(), outFile.getAbsolutePath());
                    } catch (Throwable t) {
                        Log.w(TAG, "hardlink failed " + rel + ": " + t.getMessage());
                        if (src.exists()) copyFile(src, outFile);
                    }
                    break;
                }
                default:
                    skipData(in, size);
            }
            index++;
        }
        Log.i(TAG, "extracted " + index + " entries to " + destDir);
    }

    private static String normalizeLink(File destDir, String link) {
        String rel = link.replace('\\', '/');
        while (rel.startsWith("./")) rel = rel.substring(2);
        while (rel.startsWith("/")) rel = rel.substring(1);
        return rel;
    }

    private static void extractFile(InputStream in, File out, long size) throws IOException {
        try (OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[64 * 1024];
            long remaining = size;
            while (remaining > 0) {
                int want = (int) Math.min(buf.length, remaining);
                int n = in.read(buf, 0, want);
                if (n < 0) throw new IOException("unexpected EOF in tar data");
                os.write(buf, 0, n);
                remaining -= n;
            }
        }
        skipPad(in, size);
    }

    private static void skipData(InputStream in, long size) throws IOException {
        long pad = (512 - (size % 512)) % 512;
        long total = size + pad;
        long skipped = 0;
        while (skipped < total) {
            long n = in.skip(total - skipped);
            if (n <= 0) {
                if (in.read() < 0) return;
                n = 1;
            }
            skipped += n;
        }
    }

    private static void skipPad(InputStream in, long size) throws IOException {
        long pad = (512 - (size % 512)) % 512;
        while (pad > 0) {
            long n = in.skip(pad);
            if (n <= 0) { if (in.read() < 0) return; n = 1; }
            pad -= n;
        }
    }

    private static void applyMode(File f, int mode) {
        if ((mode & 0111) != 0) f.setExecutable(true, false);
        if ((mode & 0200) == 0) f.setWritable(true); // keep writable for app
    }

    private static void parsePax(byte[] buf, Map<String, String> out, boolean global) {
        int pos = 0;
        while (pos < buf.length) {
            int sp = pos;
            while (sp < buf.length && buf[sp] != ' ') sp++;
            if (sp >= buf.length) break;
            int len;
            try { len = Integer.parseInt(new String(buf, pos, sp - pos)); } catch (NumberFormatException e) { break; }
            if (len <= 0 || pos + len > buf.length) break;
            String rec = new String(buf, sp + 1, pos + len - sp - 1);
            int eq = rec.indexOf('=');
            if (eq > 0) out.put(rec.substring(0, eq), rec.substring(eq + 1));
            pos += len;
        }
    }

    private static String readNullTerminated(byte[] buf) {
        int end = 0;
        while (end < buf.length && buf[end] != 0) end++;
        return new String(buf, 0, end);
    }

    private static String readStr(byte[] h, int off, int len) {
        int end = off;
        int max = off + len;
        while (end < max && h[end] != 0 && h[end] != ' ') end++;
        return new String(h, off, end - off);
    }

    private static long parseOctal(String s, long def) {
        s = s.trim();
        if (s.isEmpty()) return def;
        try {
            if ((s.charAt(0) & 0x80) != 0) return def; // base-256, rare; skip
            return Long.parseLong(s, 8);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean isZeroBlock(byte[] h) {
        for (byte b : h) if (b != 0) return false;
        return true;
    }

    private static boolean readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) return false;
            off += n;
        }
        return true;
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream is = new java.io.FileInputStream(src); OutputStream os = new FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        }
    }
}
