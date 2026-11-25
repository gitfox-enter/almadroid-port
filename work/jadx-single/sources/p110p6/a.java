package p110p6;

import A.AbstractC0008e;
import B.n;
import G1.k;
import H0.f;
import H5.l;
import L0.h;
import L0.m;
import L0.p;
import O4.e;
import P5.AbstractC0139a;
import S0.r;
import android.content.Context;
import androidx.compose.foundation.layout.FillElement;
import androidx.compose.foundation.layout.LayoutWeightElement;
import androidx.compose.ui.platform.AndroidCompositionLocals_androidKt;
import androidx.lifecycle.InterfaceC0366t;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.jvm.functions.Function1;
import me.yetone.alma.R;
import org.java_websocket.extensions.ExtensionRequestData;
import org.scilab.forge.jlatexmath.FontInfo;
import p018d.i;
import p034f0.AbstractC0515c;
import p034f0.AbstractC0522j;
import p034f0.AbstractC0528p;
import p034f0.AbstractC0532u;
import p034f0.C0533v;
import p034f0.y0;
import p054h5.b;
import p057i1.H;
import p061i6.C0570a0;
import p061i6.C0619g5;
import p061i6.C0664n1;
import p061i6.C0698s1;
import p061i6.U1;
import p061i6.g6;
import p066j3.D;
import p069j6.E;
import p072k1.C0819h;
import p072k1.C0820i;
import p072k1.C0821j;
import p072k1.InterfaceC0822k;
import p076k6.C0876g3;
import p082l5.c;
import p082l5.d;
import p082l5.g;
import p083l6.C1108h4;
import p083l6.D0;
import p083l6.F0;
import p088m4.A5;
import p088m4.AbstractC1456v6;
import p088m4.AbstractC1464w5;
import p088m4.Q7;
import p088m4.V4;
import p088m4.e8;
import p090m6.AbstractC1502e;
import p090m6.AbstractC1505f0;
import p090m6.AbstractC1523o0;
import p090m6.AbstractC1529s;
import p090m6.C1496b;
import p090m6.EnumC1494a;
import p090m6.G;
import p090m6.Y0;
import p090m6.Z0;
import p090m6.e1;
import p090m6.f1;
import p090m6.h1;
import p095n4.D2;
import p129s5.y;
import p135t5.t;
import p144v1.M;
import p169z0.C2136b;
import p169z0.C2154k;
import p169z0.C2159n;
import p169z0.InterfaceC2149h0;
import p169z0.Q;
import p169z0.V;
import p169z0.m0;
import p170z1.s;
import r6.AbstractC1922j;
import t6.AbstractC1966d;
import t6.F;
import t6.q;
import t6.x;

/* JADX INFO: compiled from: r8-map-id-08551124316a47cb96f8f852e55ceed0adba1d058007b632e698b1aa183a4ca9 */
/* JADX INFO: loaded from: classes.dex */
public abstract class a {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public static final f f20732a = new f(new C0619g5(25), false, 1571890767);

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static final void a(Function1 function1, C2159n c2159n, int i) {
        Function1 function2;
        C2159n c2159n2;
        c2159n.a0(2069213491);
        int i7 = (c2159n.j(function1) ? 4 : 2) | i;
        if (c2159n.Q(i7 & 1, (i7 & 3) != 2)) {
            InterfaceC0366t interfaceC0366t = (InterfaceC0366t) c2159n.l(p086m2.a.f17346a);
            Object objN = c2159n.N();
            Q q7 = C2154k.f26087a;
            if (objN == q7) {
                objN = new AtomicBoolean(false);
                c2159n.j0(objN);
            }
            AtomicBoolean atomicBoolean = (AtomicBoolean) objN;
            Object objN2 = c2159n.N();
            if (objN2 == q7) {
                objN2 = Executors.newSingleThreadExecutor();
                c2159n.j0(objN2);
            }
            ExecutorService executorService = (ExecutorService) objN2;
            Object objN3 = c2159n.N();
            if (objN3 == q7) {
                b bVar = new b(FontInfo.NUMBER_OF_CHAR_CODES);
                c cVar = (c) p039f5.f.c().a(c.class);
                d dVar = new d(bVar, (g) cVar.f15125a.b(bVar), (Executor) cVar.f15126b.f9249a.get(), e8.d(true != p082l5.b.c() ? "play-services-mlkit-barcode-scanning" : "barcode-scanning"));
                c2159n.j0(dVar);
                objN3 = dVar;
            }
            p054h5.a aVar = (p054h5.a) objN3;
            l.b(aVar);
            Object objN4 = c2159n.N();
            if (objN4 == q7) {
                objN4 = C2136b.u(null);
                c2159n.j0(objN4);
            }
            V v7 = (V) objN4;
            boolean zJ = c2159n.j(aVar) | c2159n.j(executorService);
            Object objN5 = c2159n.N();
            if (zJ || objN5 == q7) {
                objN5 = new C0698s1(aVar, executorService, v7, 17);
                c2159n.j0(objN5);
            }
            C2136b.d(y.f22805a, (Function1) objN5, c2159n);
            boolean zJ2 = c2159n.j(executorService) | c2159n.j(atomicBoolean) | c2159n.j(aVar) | ((i7 & 14) == 4) | c2159n.j(interfaceC0366t);
            Object objN6 = c2159n.N();
            if (zJ2 || objN6 == q7) {
                function2 = function1;
                g gVar = new g(executorService, v7, atomicBoolean, aVar, function2, interfaceC0366t);
                c2159n.j0(gVar);
                objN6 = gVar;
            } else {
                function2 = function1;
            }
            c2159n2 = c2159n;
            androidx.compose.ui.viewinterop.a.b((Function1) objN6, androidx.compose.foundation.layout.d.f7039c, null, c2159n2, 48, 4);
        } else {
            function2 = function1;
            c2159n2 = c2159n;
            c2159n2.T();
        }
        m0 m0VarV = c2159n2.v();
        if (m0VarV != null) {
            m0VarV.f26106d = new D0(function2, i, 1);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX WARN: Code duplicated, block: B:100:0x0388  */
    /* JADX WARN: Code duplicated, block: B:101:0x038a  */
    /* JADX WARN: Code duplicated, block: B:107:0x0398  */
    /* JADX WARN: Code duplicated, block: B:110:0x03cd  */
    /* JADX WARN: Code duplicated, block: B:111:0x03d1  */
    /* JADX WARN: Code duplicated, block: B:116:0x03ee  */
    /* JADX WARN: Code duplicated, block: B:121:0x041b  */
    /* JADX WARN: Code duplicated, block: B:124:0x043c  */
    /* JADX WARN: Code duplicated, block: B:125:0x0449  */
    /* JADX WARN: Code duplicated, block: B:128:0x046e  */
    /* JADX WARN: Code duplicated, block: B:130:0x047c  */
    /* JADX WARN: Code duplicated, block: B:131:0x0489  */
    /* JADX WARN: Code duplicated, block: B:134:0x049c  */
    /* JADX WARN: Code duplicated, block: B:135:0x049e  */
    /* JADX WARN: Code duplicated, block: B:141:0x04b2  */
    /* JADX WARN: Code duplicated, block: B:144:0x04df  */
    /* JADX WARN: Code duplicated, block: B:147:0x04fa  */
    /* JADX WARN: Code duplicated, block: B:150:0x051c  */
    /* JADX WARN: Code duplicated, block: B:153:0x0533  */
    /* JADX WARN: Code duplicated, block: B:155:0x053d  */
    /* JADX WARN: Code duplicated, block: B:157:0x0549  */
    /* JADX WARN: Code duplicated, block: B:160:0x0565  */
    /* JADX WARN: Code duplicated, block: B:48:0x011c  */
    /* JADX WARN: Code duplicated, block: B:49:0x0120  */
    /* JADX WARN: Code duplicated, block: B:54:0x013b  */
    /* JADX WARN: Code duplicated, block: B:58:0x014d  */
    /* JADX WARN: Code duplicated, block: B:61:0x0167  */
    /* JADX WARN: Code duplicated, block: B:63:0x016c  */
    /* JADX WARN: Code duplicated, block: B:66:0x01b3  */
    /* JADX WARN: Code duplicated, block: B:67:0x01b6  */
    /* JADX WARN: Code duplicated, block: B:70:0x01f8  */
    /* JADX WARN: Code duplicated, block: B:71:0x01fb  */
    /* JADX WARN: Code duplicated, block: B:75:0x0220  */
    /* JADX WARN: Code duplicated, block: B:78:0x0252  */
    /* JADX WARN: Code duplicated, block: B:79:0x0256  */
    /* JADX WARN: Code duplicated, block: B:84:0x0273  */
    /* JADX WARN: Code duplicated, block: B:90:0x0296  */
    /* JADX WARN: Code duplicated, block: B:91:0x02a2  */
    /* JADX WARN: Code duplicated, block: B:94:0x02e4  */
    /* JADX WARN: Code duplicated, block: B:95:0x02f1  */
    /* JADX WARN: Code duplicated, block: B:98:0x034f  */
    public static final void b(E e7, G5.a aVar, C2159n c2159n, int i, int i7) {
        G5.a aVar2;
        int i8;
        E e8;
        int i9;
        C0819h c0819h;
        int i10;
        G5.a aVar3;
        int i11;
        m mVar;
        String str;
        n nVar;
        String str2;
        int i12;
        C0819h c0819h2;
        Object objN;
        V v7;
        C0819h c0819h3;
        V v8;
        Q q7;
        Object objN2;
        V v9;
        V v10;
        G5.a aVar4;
        boolean z7;
        int i13;
        int i14;
        boolean z8;
        Object objN3;
        V v11;
        V v12;
        G5.a aVar5;
        String str3;
        Object objN4;
        Object objN5;
        Object objN6;
        V v13;
        boolean z9;
        boolean z10;
        Object objN7;
        V v14;
        Object objN8;
        Object objN9;
        boolean z11;
        Object objN10;
        int i15;
        C2159n c2159n2 = c2159n;
        l.e("app", e7);
        c2159n2.a0(-347358753);
        int i16 = i | (c2159n2.j(e7) ? 4 : 2);
        int i17 = i7 & 2;
        if (i17 != 0) {
            i8 = i16 | 48;
            aVar2 = aVar;
        } else {
            aVar2 = aVar;
            i8 = i16 | (c2159n2.j(aVar2) ? 32 : 16);
        }
        if (c2159n2.Q(i8 & 1, (i8 & 19) != 18)) {
            G5.a aVar6 = i17 != 0 ? null : aVar2;
            Object objN11 = c2159n2.N();
            Q q8 = C2154k.f26087a;
            if (objN11 == q8) {
                objN11 = C2136b.u(Boolean.FALSE);
                c2159n2.j0(objN11);
            }
            V v15 = (V) objN11;
            Object objN12 = c2159n2.N();
            if (objN12 == q8) {
                objN12 = C2136b.u(Boolean.FALSE);
                c2159n2.j0(objN12);
            }
            V v16 = (V) objN12;
            Object objN13 = c2159n2.N();
            if (objN13 == q8) {
                objN13 = C2136b.u(ExtensionRequestData.EMPTY_VALUE);
                c2159n2.j0(objN13);
            }
            V v17 = (V) objN13;
            Object objN14 = c2159n2.N();
            if (objN14 == q8) {
                objN14 = C2136b.u(null);
                c2159n2.j0(objN14);
            }
            V v18 = (V) objN14;
            FillElement fillElement = androidx.compose.foundation.layout.d.f7039c;
            n nVar2 = e1.f18883a;
            long jA = Z0.f18811a.a(c2159n2);
            e eVar = S0.n.f4026a;
            p pVarB = androidx.compose.foundation.a.b(fillElement, jA, eVar);
            int i18 = 3;
            p pVarA = L0.a.a(pVarB, new y0(i18, i18));
            H hD = AbstractC0528p.d(L0.c.f2776c, false);
            int i19 = c2159n2.f26125P;
            InterfaceC2149h0 interfaceC2149h0N = c2159n2.n();
            p pVarC = L0.a.c(pVarA, c2159n2);
            InterfaceC0822k.f13042U.getClass();
            C0820i c0820i = C0821j.f13033b;
            c2159n2.c0();
            if (c2159n2.f26124O) {
                c2159n2.m(c0820i);
            } else {
                c2159n2.m0();
            }
            C0819h c0819h4 = C0821j.f13037f;
            C2136b.y(c0819h4, hD, c2159n2);
            C0819h c0819h5 = C0821j.f13036e;
            C2136b.y(c0819h5, interfaceC2149h0N, c2159n2);
            C0819h c0819h6 = C0821j.f13038g;
            if (c2159n2.f26124O) {
                i9 = i8;
            } else {
                i9 = i8;
                if (!l.a(c2159n2.N(), Integer.valueOf(i19))) {
                }
                c0819h = C0821j.f13035d;
                C2136b.y(c0819h, pVarC, c2159n2);
                L0.f fVar = L0.c.f2773Z;
                C0533v c0533vA = AbstractC0532u.a(AbstractC0522j.f9118c, fVar, c2159n2, 48);
                i10 = c2159n2.f26125P;
                aVar3 = aVar6;
                InterfaceC2149h0 interfaceC2149h0N2 = c2159n2.n();
                p pVarC2 = L0.a.c(fillElement, c2159n2);
                c2159n2.c0();
                if (c2159n2.f26124O) {
                    c2159n2.m(c0820i);
                } else {
                    c2159n2.m0();
                }
                C2136b.y(c0819h4, c0533vA, c2159n2);
                C2136b.y(c0819h5, interfaceC2149h0N2, c2159n2);
                if (c2159n2.f26124O || !l.a(c2159n2.N(), Integer.valueOf(i10))) {
                    AbstractC0008e.q(i10, c2159n2, i10, c0819h6);
                }
                C2136b.y(c0819h, pVarC2, c2159n2);
                if (1.0f <= 0.0d) {
                    p042g0.a.a("invalid weight; must be greater than zero");
                }
                AbstractC0515c.d(new LayoutWeightElement(1.0f, true), c2159n2);
                if (((Boolean) c2159n2.l(f1.f18891a)).booleanValue()) {
                    i11 = R.drawable.app_icon_display_dark;
                } else {
                    i11 = R.drawable.app_icon_display;
                }
                X0.b bVarA = Q7.a(i11, c2159n2, 0);
                mVar = m.f2794c;
                A5.a(bVarA, null, V4.a(androidx.compose.foundation.layout.d.j(mVar, 88), p078l0.e.a(22)), null, null, 0.0f, null, c2159n, 48, 120);
                AbstractC0515c.d(androidx.compose.foundation.layout.d.e(mVar, 28), c2159n);
                if (aVar3 == null) {
                    str = "Alma";
                } else {
                    str = "Add a Device";
                }
                M mG = h1.g(34, 4, s.f26336x);
                nVar = Y0.f18788a;
                AbstractC1505f0.C(str, mG, nVar.a(c2159n), null, 0, null, 0, c2159n, 0, 120);
                float f7 = 6;
                AbstractC0515c.d(androidx.compose.foundation.layout.d.e(mVar, f7), c2159n);
                if (aVar3 == null) {
                    str2 = "Your computer's agent, in your pocket.";
                } else {
                    str2 = "Switch between your devices in Settings.";
                }
                AbstractC1505f0.C(str2, AbstractC1529s.f19034e, Y0.a(c2159n), null, 0, null, 0, c2159n, 48, 120);
                if (1.0f <= 0.0d) {
                    p042g0.a.a("invalid weight; must be greater than zero");
                }
                AbstractC0515c.d(new LayoutWeightElement(1.0f, true), c2159n);
                float f8 = 24;
                p pVarK = androidx.compose.foundation.layout.a.k(mVar, f8, 0.0f, 2);
                C0533v c0533vA2 = AbstractC0532u.a(AbstractC0522j.g(12), fVar, c2159n, 54);
                i12 = c2159n.f26125P;
                InterfaceC2149h0 interfaceC2149h0N3 = c2159n.n();
                p pVarC3 = L0.a.c(pVarK, c2159n);
                c2159n.c0();
                if (c2159n.f26124O) {
                    c2159n.m(c0820i);
                } else {
                    c2159n.m0();
                }
                C2136b.y(c0819h4, c0533vA2, c2159n);
                C2136b.y(c0819h5, interfaceC2149h0N3, c2159n);
                if (c2159n.f26124O && l.a(c2159n.N(), Integer.valueOf(i12))) {
                    c0819h2 = c0819h6;
                } else {
                    c0819h2 = c0819h6;
                    AbstractC0008e.q(i12, c2159n, i12, c0819h2);
                }
                C2136b.y(c0819h, pVarC3, c2159n);
                p pVarC4 = androidx.compose.foundation.layout.d.c(mVar, 1.0f);
                float f9 = 14;
                float f10 = 64;
                objN = c2159n.N();
                if (objN == q8) {
                    v7 = v15;
                    objN = new C1108h4(4, v7);
                    c2159n.j0(objN);
                } else {
                    v7 = v15;
                }
                c0819h3 = c0819h2;
                v8 = v7;
                q7 = q8;
                AbstractC1505f0.A(pVarC4, f9, f10, false, 0L, 0.0f, (G5.a) objN, f20732a, c2159n, 14156214, 56);
                c2159n2 = c2159n;
                M mB = h1.b(AbstractC1529s.f19036g);
                long jA2 = Y0.a(c2159n2);
                objN2 = c2159n2.N();
                if (objN2 == q7) {
                    v9 = v16;
                    objN2 = new C1108h4(8, v9);
                    c2159n2.j0(objN2);
                } else {
                    v9 = v16;
                }
                v10 = v9;
                AbstractC1505f0.C("Paste a pairing link", mB, jA2, androidx.compose.foundation.layout.a.k(AbstractC1523o0.g(mVar, false, 0.0f, 0.0f, null, (G5.a) objN2, 15), 0.0f, f7, 1), 0, null, 0, c2159n2, 6, 112);
                c2159n2.q(true);
                AbstractC1505f0.C("On your computer, open Alma → Settings → Remote Access.", AbstractC1529s.f19037h, Y0.f18789b.a(c2159n2), androidx.compose.foundation.layout.a.m(mVar, 0.0f, 18, 0.0f, f8, 5), 0, new k(3), 0, c2159n2, 3126, 80);
                c2159n2.q(true);
                if (aVar3 != null) {
                    c2159n2.Z(1942871209);
                    p pVarB2 = androidx.compose.foundation.a.b(V4.a(androidx.compose.foundation.layout.d.j(androidx.compose.foundation.layout.a.i(androidx.compose.foundation.layout.b.f7034a.a(mVar, L0.c.f2778h), 16), 30), p078l0.e.f14650a), r.c(0.07f, nVar.a(c2159n2)), eVar);
                    i13 = 32;
                    if ((i9 & 112) == 32) {
                        z11 = true;
                    } else {
                        z11 = false;
                    }
                    objN10 = c2159n2.N();
                    if (!z11 || objN10 == q7) {
                        aVar4 = aVar3;
                        i14 = 2;
                        objN10 = new G(aVar4, i14);
                        c2159n2.j0(objN10);
                    } else {
                        aVar4 = aVar3;
                        i14 = 2;
                    }
                    p pVarG = AbstractC1523o0.g(pVarB2, false, 0.0f, 0.0f, null, (G5.a) objN10, 15);
                    H hD2 = AbstractC0528p.d(L0.c.f2780r, false);
                    i15 = c2159n2.f26125P;
                    InterfaceC2149h0 interfaceC2149h0N4 = c2159n2.n();
                    p pVarC5 = L0.a.c(pVarG, c2159n2);
                    c2159n2.c0();
                    if (c2159n2.f26124O) {
                        c2159n2.m(c0820i);
                    } else {
                        c2159n2.m0();
                    }
                    C2136b.y(c0819h4, hD2, c2159n2);
                    C2136b.y(c0819h5, interfaceC2149h0N4, c2159n2);
                    if (c2159n2.f26124O || !l.a(c2159n2.N(), Integer.valueOf(i15))) {
                        AbstractC0008e.q(i15, c2159n2, i15, c0819h3);
                    }
                    C2136b.y(c0819h, pVarC5, c2159n2);
                    z8 = false;
                    AbstractC1502e.j("xmark", 13, Y0.a(c2159n2), null, "Close", c2159n, 24630, 8);
                    c2159n2 = c2159n;
                    z7 = true;
                    c2159n2.q(true);
                } else {
                    aVar4 = aVar3;
                    z7 = true;
                    i13 = 32;
                    i14 = 2;
                    z8 = false;
                    c2159n2.Z(1939272233);
                }
                c2159n2.q(z8);
                c2159n2.q(z7);
                boolean zBooleanValue = ((Boolean) v8.getValue()).booleanValue();
                objN3 = c2159n2.N();
                if (objN3 == q7) {
                    v11 = v8;
                    objN3 = new C1108h4(9, v11);
                    c2159n2.j0(objN3);
                } else {
                    v11 = v8;
                }
                AbstractC1505f0.h(zBooleanValue, (G5.a) objN3, H0.l.d(-1324629424, new C0570a0(e7, aVar4, v11, v18), c2159n2), c2159n2, 432);
                if (((Boolean) v10.getValue()).booleanValue()) {
                    c2159n2.Z(-1585141913);
                    EnumC1494a enumC1494a = EnumC1494a.f18823d;
                    objN6 = c2159n2.N();
                    if (objN6 == q7) {
                        v13 = v17;
                        objN6 = new C1108h4(10, v13);
                        c2159n2.j0(objN6);
                    } else {
                        v13 = v17;
                    }
                    C1496b c1496b = new C1496b("Cancel", enumC1494a, (G5.a) objN6);
                    boolean zJ = c2159n2.j(e7);
                    if ((i9 & 112) == i13) {
                        z9 = z7;
                    } else {
                        z9 = z8;
                    }
                    z10 = zJ | z9;
                    objN7 = c2159n2.N();
                    if (!z10 || objN7 == q7) {
                        G5.a aVar7 = aVar4;
                        v14 = v13;
                        C0664n1 c0664n1 = new C0664n1((Object) v14, (Object) e7, (Object) aVar7, (Object) v18, 12);
                        e8 = e7;
                        aVar5 = aVar7;
                        v12 = v18;
                        c2159n2.j0(c0664n1);
                        objN7 = c0664n1;
                    } else {
                        e8 = e7;
                        v12 = v18;
                        aVar5 = aVar4;
                        v14 = v13;
                    }
                    List listG = D2.g(c1496b, new C1496b("Connect", (G5.a) objN7, i14));
                    objN8 = c2159n2.N();
                    if (objN8 == q7) {
                        objN8 = new C1108h4(11, v10);
                        c2159n2.j0(objN8);
                    }
                    G5.a aVar8 = (G5.a) objN8;
                    String str4 = (String) v14.getValue();
                    objN9 = c2159n2.N();
                    if (objN9 == q7) {
                        objN9 = new C0876g3(15, v14);
                        c2159n2.j0(objN9);
                    }
                    AbstractC1505f0.j("Paste pairing link", null, listG, aVar8, new D("alma://connect?…", str4, (Function1) objN9), c2159n2, 3078, 2);
                } else {
                    e8 = e7;
                    v12 = v18;
                    aVar5 = aVar4;
                    i7 = i7;
                    q7 = q7;
                    c2159n2.Z(-1589282397);
                }
                c2159n2.q(z8);
                str3 = (String) v12.getValue();
                if (str3 == null) {
                    c2159n2.Z(-1584730017);
                } else {
                    c2159n2.Z(-1584730016);
                    objN4 = c2159n2.N();
                    if (objN4 == q7) {
                        objN4 = new C1108h4(5, v12);
                        c2159n2.j0(objN4);
                    }
                    List listF = D2.f(new C1496b("OK", (G5.a) objN4, i14));
                    objN5 = c2159n2.N();
                    if (objN5 == q7) {
                        objN5 = new C1108h4(7, v12);
                        c2159n2.j0(objN5);
                    }
                    AbstractC1505f0.j("Couldn't pair", str3, listF, (G5.a) objN5, null, c2159n2, 3078, 16);
                }
                c2159n2.q(z8);
                aVar2 = aVar5;
            }
            AbstractC0008e.q(i19, c2159n2, i19, c0819h6);
            c0819h = C0821j.f13035d;
            C2136b.y(c0819h, pVarC, c2159n2);
            L0.f fVar2 = L0.c.f2773Z;
            C0533v c0533vA3 = AbstractC0532u.a(AbstractC0522j.f9118c, fVar2, c2159n2, 48);
            i10 = c2159n2.f26125P;
            aVar3 = aVar6;
            InterfaceC2149h0 interfaceC2149h0N5 = c2159n2.n();
            p pVarC6 = L0.a.c(fillElement, c2159n2);
            c2159n2.c0();
            if (c2159n2.f26124O) {
                c2159n2.m(c0820i);
            } else {
                c2159n2.m0();
            }
            C2136b.y(c0819h4, c0533vA3, c2159n2);
            C2136b.y(c0819h5, interfaceC2149h0N5, c2159n2);
            if (c2159n2.f26124O) {
                AbstractC0008e.q(i10, c2159n2, i10, c0819h6);
            } else {
                AbstractC0008e.q(i10, c2159n2, i10, c0819h6);
            }
            C2136b.y(c0819h, pVarC6, c2159n2);
            if (1.0f <= 0.0d) {
                p042g0.a.a("invalid weight; must be greater than zero");
            }
            AbstractC0515c.d(new LayoutWeightElement(1.0f, true), c2159n2);
            if (((Boolean) c2159n2.l(f1.f18891a)).booleanValue()) {
                i11 = R.drawable.app_icon_display_dark;
            } else {
                i11 = R.drawable.app_icon_display;
            }
            X0.b bVarA2 = Q7.a(i11, c2159n2, 0);
            mVar = m.f2794c;
            A5.a(bVarA2, null, V4.a(androidx.compose.foundation.layout.d.j(mVar, 88), p078l0.e.a(22)), null, null, 0.0f, null, c2159n, 48, 120);
            AbstractC0515c.d(androidx.compose.foundation.layout.d.e(mVar, 28), c2159n);
            if (aVar3 == null) {
                str = "Alma";
            } else {
                str = "Add a Device";
            }
            M mG2 = h1.g(34, 4, s.f26336x);
            nVar = Y0.f18788a;
            AbstractC1505f0.C(str, mG2, nVar.a(c2159n), null, 0, null, 0, c2159n, 0, 120);
            float f11 = 6;
            AbstractC0515c.d(androidx.compose.foundation.layout.d.e(mVar, f11), c2159n);
            if (aVar3 == null) {
                str2 = "Your computer's agent, in your pocket.";
            } else {
                str2 = "Switch between your devices in Settings.";
            }
            AbstractC1505f0.C(str2, AbstractC1529s.f19034e, Y0.a(c2159n), null, 0, null, 0, c2159n, 48, 120);
            if (1.0f <= 0.0d) {
                p042g0.a.a("invalid weight; must be greater than zero");
            }
            AbstractC0515c.d(new LayoutWeightElement(1.0f, true), c2159n);
            float f12 = 24;
            p pVarK2 = androidx.compose.foundation.layout.a.k(mVar, f12, 0.0f, 2);
            C0533v c0533vA4 = AbstractC0532u.a(AbstractC0522j.g(12), fVar2, c2159n, 54);
            i12 = c2159n.f26125P;
            InterfaceC2149h0 interfaceC2149h0N6 = c2159n.n();
            p pVarC7 = L0.a.c(pVarK2, c2159n);
            c2159n.c0();
            if (c2159n.f26124O) {
                c2159n.m(c0820i);
            } else {
                c2159n.m0();
            }
            C2136b.y(c0819h4, c0533vA4, c2159n);
            C2136b.y(c0819h5, interfaceC2149h0N6, c2159n);
            if (c2159n.f26124O) {
                c0819h2 = c0819h6;
                AbstractC0008e.q(i12, c2159n, i12, c0819h2);
            } else {
                c0819h2 = c0819h6;
                AbstractC0008e.q(i12, c2159n, i12, c0819h2);
            }
            C2136b.y(c0819h, pVarC7, c2159n);
            p pVarC8 = androidx.compose.foundation.layout.d.c(mVar, 1.0f);
            float f13 = 14;
            float f14 = 64;
            objN = c2159n.N();
            if (objN == q8) {
                v7 = v15;
                objN = new C1108h4(4, v7);
                c2159n.j0(objN);
            } else {
                v7 = v15;
            }
            c0819h3 = c0819h2;
            v8 = v7;
            q7 = q8;
            AbstractC1505f0.A(pVarC8, f13, f14, false, 0L, 0.0f, (G5.a) objN, f20732a, c2159n, 14156214, 56);
            c2159n2 = c2159n;
            M mB2 = h1.b(AbstractC1529s.f19036g);
            long jA3 = Y0.a(c2159n2);
            objN2 = c2159n2.N();
            if (objN2 == q7) {
                v9 = v16;
                objN2 = new C1108h4(8, v9);
                c2159n2.j0(objN2);
            } else {
                v9 = v16;
            }
            v10 = v9;
            AbstractC1505f0.C("Paste a pairing link", mB2, jA3, androidx.compose.foundation.layout.a.k(AbstractC1523o0.g(mVar, false, 0.0f, 0.0f, null, (G5.a) objN2, 15), 0.0f, f11, 1), 0, null, 0, c2159n2, 6, 112);
            c2159n2.q(true);
            AbstractC1505f0.C("On your computer, open Alma → Settings → Remote Access.", AbstractC1529s.f19037h, Y0.f18789b.a(c2159n2), androidx.compose.foundation.layout.a.m(mVar, 0.0f, 18, 0.0f, f12, 5), 0, new k(3), 0, c2159n2, 3126, 80);
            c2159n2.q(true);
            if (aVar3 != null) {
                c2159n2.Z(1942871209);
                p pVarB3 = androidx.compose.foundation.a.b(V4.a(androidx.compose.foundation.layout.d.j(androidx.compose.foundation.layout.a.i(androidx.compose.foundation.layout.b.f7034a.a(mVar, L0.c.f2778h), 16), 30), p078l0.e.f14650a), r.c(0.07f, nVar.a(c2159n2)), eVar);
                i13 = 32;
                if ((i9 & 112) == 32) {
                    z11 = true;
                } else {
                    z11 = false;
                }
                objN10 = c2159n2.N();
                if (z11) {
                    aVar4 = aVar3;
                    i14 = 2;
                    objN10 = new G(aVar4, i14);
                    c2159n2.j0(objN10);
                } else {
                    aVar4 = aVar3;
                    i14 = 2;
                    objN10 = new G(aVar4, i14);
                    c2159n2.j0(objN10);
                }
                p pVarG2 = AbstractC1523o0.g(pVarB3, false, 0.0f, 0.0f, null, (G5.a) objN10, 15);
                H hD3 = AbstractC0528p.d(L0.c.f2780r, false);
                i15 = c2159n2.f26125P;
                InterfaceC2149h0 interfaceC2149h0N7 = c2159n2.n();
                p pVarC9 = L0.a.c(pVarG2, c2159n2);
                c2159n2.c0();
                if (c2159n2.f26124O) {
                    c2159n2.m(c0820i);
                } else {
                    c2159n2.m0();
                }
                C2136b.y(c0819h4, hD3, c2159n2);
                C2136b.y(c0819h5, interfaceC2149h0N7, c2159n2);
                if (c2159n2.f26124O) {
                    AbstractC0008e.q(i15, c2159n2, i15, c0819h3);
                } else {
                    AbstractC0008e.q(i15, c2159n2, i15, c0819h3);
                }
                C2136b.y(c0819h, pVarC9, c2159n2);
                z8 = false;
                AbstractC1502e.j("xmark", 13, Y0.a(c2159n2), null, "Close", c2159n, 24630, 8);
                c2159n2 = c2159n;
                z7 = true;
                c2159n2.q(true);
            } else {
                aVar4 = aVar3;
                z7 = true;
                i13 = 32;
                i14 = 2;
                z8 = false;
                c2159n2.Z(1939272233);
            }
            c2159n2.q(z8);
            c2159n2.q(z7);
            boolean zBooleanValue2 = ((Boolean) v8.getValue()).booleanValue();
            objN3 = c2159n2.N();
            if (objN3 == q7) {
                v11 = v8;
                objN3 = new C1108h4(9, v11);
                c2159n2.j0(objN3);
            } else {
                v11 = v8;
            }
            AbstractC1505f0.h(zBooleanValue2, (G5.a) objN3, H0.l.d(-1324629424, new C0570a0(e7, aVar4, v11, v18), c2159n2), c2159n2, 432);
            if (((Boolean) v10.getValue()).booleanValue()) {
                c2159n2.Z(-1585141913);
                EnumC1494a enumC1494a2 = EnumC1494a.f18823d;
                objN6 = c2159n2.N();
                if (objN6 == q7) {
                    v13 = v17;
                    objN6 = new C1108h4(10, v13);
                    c2159n2.j0(objN6);
                } else {
                    v13 = v17;
                }
                C1496b c1496b2 = new C1496b("Cancel", enumC1494a2, (G5.a) objN6);
                boolean zJ2 = c2159n2.j(e7);
                if ((i9 & 112) == i13) {
                    z9 = z7;
                } else {
                    z9 = z8;
                }
                z10 = zJ2 | z9;
                objN7 = c2159n2.N();
                if (z10) {
                    G5.a aVar9 = aVar4;
                    v14 = v13;
                    C0664n1 c0664n2 = new C0664n1((Object) v14, (Object) e7, (Object) aVar9, (Object) v18, 12);
                    e8 = e7;
                    aVar5 = aVar9;
                    v12 = v18;
                    c2159n2.j0(c0664n2);
                    objN7 = c0664n2;
                } else {
                    G5.a aVar10 = aVar4;
                    v14 = v13;
                    C0664n1 c0664n3 = new C0664n1((Object) v14, (Object) e7, (Object) aVar10, (Object) v18, 12);
                    e8 = e7;
                    aVar5 = aVar10;
                    v12 = v18;
                    c2159n2.j0(c0664n3);
                    objN7 = c0664n3;
                }
                List listG2 = D2.g(c1496b2, new C1496b("Connect", (G5.a) objN7, i14));
                objN8 = c2159n2.N();
                if (objN8 == q7) {
                    objN8 = new C1108h4(11, v10);
                    c2159n2.j0(objN8);
                }
                G5.a aVar11 = (G5.a) objN8;
                String str5 = (String) v14.getValue();
                objN9 = c2159n2.N();
                if (objN9 == q7) {
                    objN9 = new C0876g3(15, v14);
                    c2159n2.j0(objN9);
                }
                AbstractC1505f0.j("Paste pairing link", null, listG2, aVar11, new D("alma://connect?…", str5, (Function1) objN9), c2159n2, 3078, 2);
            } else {
                e8 = e7;
                v12 = v18;
                aVar5 = aVar4;
                i7 = i7;
                q7 = q7;
                c2159n2.Z(-1589282397);
            }
            c2159n2.q(z8);
            str3 = (String) v12.getValue();
            if (str3 == null) {
                c2159n2.Z(-1584730017);
            } else {
                c2159n2.Z(-1584730016);
                objN4 = c2159n2.N();
                if (objN4 == q7) {
                    objN4 = new C1108h4(5, v12);
                    c2159n2.j0(objN4);
                }
                List listF2 = D2.f(new C1496b("OK", (G5.a) objN4, i14));
                objN5 = c2159n2.N();
                if (objN5 == q7) {
                    objN5 = new C1108h4(7, v12);
                    c2159n2.j0(objN5);
                }
                AbstractC1505f0.j("Couldn't pair", str3, listF2, (G5.a) objN5, null, c2159n2, 3078, 16);
            }
            c2159n2.q(z8);
            aVar2 = aVar5;
        } else {
            i7 = i7;
            e8 = e7;
            c2159n2.T();
        }
        m0 m0VarV = c2159n2.v();
        if (m0VarV != null) {
            m0VarV.f26106d = new e(e8, aVar2, i, i7);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static final void c(E e7, G5.a aVar, V v7, String str) {
        c cVarH = h(str);
        if (cVarH == null) {
            v7.setValue("That isn't an Alma pairing link.");
            p129s5.n nVar = t6.r.f23388a;
            t6.r.b(q.f23386h);
            return;
        }
        try {
            e7.G(cVarH);
            p129s5.n nVar2 = t6.r.f23388a;
            t6.r.b(q.f23384c);
            if (aVar != null) {
                aVar.a();
            }
        } catch (Throwable th) {
            v7.setValue(t6.G.f(th.toString(), th));
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX WARN: Code duplicated, block: B:42:0x011b  */
    /* JADX WARN: Code duplicated, block: B:44:0x0152  */
    /* JADX WARN: Code duplicated, block: B:45:0x0156  */
    /* JADX WARN: Code duplicated, block: B:50:0x0171  */
    /* JADX WARN: Code duplicated, block: B:53:0x01ef  */
    /* JADX WARN: Code duplicated, block: B:56:0x01f4  */
    /* JADX WARN: Code duplicated, block: B:59:0x0221  */
    /* JADX WARN: Code duplicated, block: B:61:0x0241  */
    /* JADX WARN: Code duplicated, block: B:63:0x0278  */
    /* JADX WARN: Code duplicated, block: B:66:0x02ae  */
    /* JADX WARN: Code duplicated, block: B:67:0x02ba  */
    /* JADX WARN: Code duplicated, block: B:70:0x02e3  */
    /* JADX WARN: Code duplicated, block: B:72:0x02eb  */
    /* JADX WARN: Code duplicated, block: B:77:0x0309  */
    public static final void d(int i, G5.a aVar, Function1 function1, C2159n c2159n) {
        G5.a aVar2;
        V v7;
        C0819h c0819h;
        boolean zBooleanValue;
        m mVar;
        androidx.compose.foundation.layout.b bVar;
        C0819h c0819h2;
        C0819h c0819h3;
        long j7;
        C0819h c0819h4;
        C0820i c0820i;
        Q q7;
        boolean z7;
        e eVar;
        m mVar2;
        androidx.compose.foundation.layout.b bVar2;
        Object objN;
        int i7;
        int i8;
        boolean zJ;
        Object objN2;
        Q q8;
        C2159n c2159n2 = c2159n;
        h hVar = L0.c.f2780r;
        l.e("onCode", function1);
        l.e("onCancel", aVar);
        c2159n2.a0(-1208052066);
        int i9 = i | (c2159n2.j(function1) ? 4 : 2);
        if (c2159n2.Q(i9 & 1, (i9 & 19) != 18)) {
            Context context = (Context) c2159n2.l(AndroidCompositionLocals_androidKt.f7186b);
            Object objN3 = c2159n2.N();
            Q q9 = C2154k.f26087a;
            if (objN3 == q9) {
                objN3 = C2136b.u(Boolean.valueOf(P1.a.a(context, "android.permission.CAMERA") == 0));
                c2159n2.j0(objN3);
            }
            V v8 = (V) objN3;
            Object objN4 = c2159n2.N();
            if (objN4 == q9) {
                objN4 = C2136b.u(Boolean.FALSE);
                c2159n2.j0(objN4);
            }
            V v9 = (V) objN4;
            p041g.a aVar3 = new p041g.a(false);
            Object objN5 = c2159n2.N();
            if (objN5 == q9) {
                objN5 = new U1(v8, v9, 4);
                c2159n2.j0(objN5);
            }
            i iVarA = AbstractC1456v6.a(aVar3, (Function1) objN5, c2159n2, 48);
            boolean zJ2 = c2159n2.j(iVarA);
            Object objN6 = c2159n2.N();
            if (zJ2 || objN6 == q9) {
                objN6 = new F0(iVarA, v8, null, 1);
                c2159n2.j0(objN6);
            }
            C2136b.f((G5.d) objN6, y.f22805a, c2159n2);
            FillElement fillElement = androidx.compose.foundation.layout.d.f7039c;
            long j8 = r.f4032b;
            e eVar2 = S0.n.f4026a;
            p pVarB = androidx.compose.foundation.a.b(fillElement, j8, eVar2);
            H hD = AbstractC0528p.d(L0.c.f2776c, false);
            int i10 = c2159n2.f26125P;
            InterfaceC2149h0 interfaceC2149h0N = c2159n2.n();
            p pVarC = L0.a.c(pVarB, c2159n2);
            InterfaceC0822k.f13042U.getClass();
            C0820i c0820i2 = C0821j.f13033b;
            c2159n2.c0();
            if (c2159n2.f26124O) {
                c2159n2.m(c0820i2);
            } else {
                c2159n2.m0();
            }
            C0819h c0819h5 = C0821j.f13037f;
            C2136b.y(c0819h5, hD, c2159n2);
            C0819h c0819h6 = C0821j.f13036e;
            C2136b.y(c0819h6, interfaceC2149h0N, c2159n2);
            C0819h c0819h7 = C0821j.f13038g;
            if (c2159n2.f26124O) {
                v7 = v9;
            } else {
                v7 = v9;
                if (!l.a(c2159n2.N(), Integer.valueOf(i10))) {
                }
                c0819h = C0821j.f13035d;
                C2136b.y(c0819h, pVarC, c2159n2);
                zBooleanValue = ((Boolean) v7.getValue()).booleanValue();
                mVar = m.f2794c;
                bVar = androidx.compose.foundation.layout.b.f7034a;
                if (zBooleanValue) {
                    c2159n2.Z(-199300025);
                    p pVarI = androidx.compose.foundation.layout.a.i(bVar.a(mVar, hVar), 32);
                    C0533v c0533vA = AbstractC0532u.a(AbstractC0522j.g(12), L0.c.f2773Z, c2159n2, 54);
                    i8 = c2159n2.f26125P;
                    InterfaceC2149h0 interfaceC2149h0N2 = c2159n2.n();
                    p pVarC2 = L0.a.c(pVarI, c2159n2);
                    c2159n2.c0();
                    if (c2159n2.f26124O) {
                        c2159n2.m(c0820i2);
                    } else {
                        c2159n2.m0();
                    }
                    C2136b.y(c0819h5, c0533vA, c2159n2);
                    C2136b.y(c0819h6, interfaceC2149h0N2, c2159n2);
                    if (c2159n2.f26124O || !l.a(c2159n2.N(), Integer.valueOf(i8))) {
                        AbstractC0008e.q(i8, c2159n2, i8, c0819h7);
                    }
                    C2136b.y(c0819h, pVarC2, c2159n2);
                    M m7 = AbstractC1529s.f19030a;
                    long j9 = r.f4033c;
                    c0819h2 = c0819h7;
                    j7 = j8;
                    AbstractC1502e.k("camera.fill", m7, r.c(0.6f, j9), null, c2159n2, 438, 8);
                    c0820i = c0820i2;
                    c0819h4 = c0819h;
                    c0819h3 = c0819h5;
                    eVar = eVar2;
                    z7 = false;
                    c2159n2 = c2159n;
                    AbstractC1505f0.C("Camera access is off", AbstractC1529s.f19033d, j9, null, 0, null, 0, c2159n2, 438, 120);
                    AbstractC1505f0.C("Allow the camera in Settings, or paste the pairing link instead.", AbstractC1529s.f19036g, r.c(0.6f, j9), null, 0, new k(3), 0, c2159n2, 438, 88);
                    zJ = c2159n2.j(context);
                    objN2 = c2159n2.N();
                    if (zJ) {
                        q8 = q9;
                    } else {
                        q8 = q9;
                        if (objN2 == q8) {
                        }
                        q7 = q8;
                        AbstractC1505f0.c("Open Settings", null, j9, 0L, 0.0f, (G5.a) objN2, c2159n2, 390, 26);
                        c2159n2.q(true);
                        c2159n2.q(false);
                        mVar2 = mVar;
                        bVar2 = bVar;
                    }
                    objN2 = new f(context, 0);
                    c2159n2.j0(objN2);
                    q7 = q8;
                    AbstractC1505f0.c("Open Settings", null, j9, 0L, 0.0f, (G5.a) objN2, c2159n2, 390, 26);
                    c2159n2.q(true);
                    c2159n2.q(false);
                    mVar2 = mVar;
                    bVar2 = bVar;
                } else {
                    c0819h2 = c0819h7;
                    c0819h3 = c0819h5;
                    j7 = j8;
                    c0819h4 = c0819h;
                    c0820i = c0820i2;
                    q7 = q9;
                    z7 = false;
                    eVar = eVar2;
                    if (((Boolean) v8.getValue()).booleanValue()) {
                        c2159n2.Z(-198483082);
                        a(function1, c2159n2, i9 & 14);
                        mVar2 = mVar;
                        bVar2 = bVar;
                        AbstractC0528p.a(AbstractC1464w5.a(androidx.compose.foundation.layout.d.j(bVar2.a(mVar2, hVar), 240), 2, r.c(0.9f, r.f4033c), p078l0.e.a(24)), c2159n2, 0);
                    } else {
                        mVar2 = mVar;
                        bVar2 = bVar;
                        c2159n2.Z(-201716258);
                    }
                    c2159n2.q(false);
                }
                p pVarB2 = androidx.compose.foundation.a.b(V4.a(androidx.compose.foundation.layout.d.j(androidx.compose.foundation.layout.a.i(AbstractC0515c.o(mVar2), 16), 36), p078l0.e.f14650a), r.c(0.4f, j7), eVar);
                objN = c2159n2.N();
                if (objN == q7) {
                    aVar2 = aVar;
                    objN = new G(aVar2, 3);
                    c2159n2.j0(objN);
                } else {
                    aVar2 = aVar;
                }
                p pVarG = AbstractC1523o0.g(pVarB2, false, 0.0f, 0.0f, null, (G5.a) objN, 15);
                H hD2 = AbstractC0528p.d(hVar, z7);
                i7 = c2159n2.f26125P;
                InterfaceC2149h0 interfaceC2149h0N3 = c2159n2.n();
                p pVarC3 = L0.a.c(pVarG, c2159n2);
                c2159n2.c0();
                if (c2159n2.f26124O) {
                    c2159n2.m(c0820i);
                } else {
                    c2159n2.m0();
                }
                C2136b.y(c0819h3, hD2, c2159n2);
                C2136b.y(c0819h6, interfaceC2149h0N3, c2159n2);
                if (c2159n2.f26124O || !l.a(c2159n2.N(), Integer.valueOf(i7))) {
                    AbstractC0008e.q(i7, c2159n2, i7, c0819h2);
                }
                C2136b.y(c0819h4, pVarC3, c2159n2);
                M mD = h1.d(AbstractC1529s.f19034e);
                long j10 = r.f4033c;
                AbstractC1502e.k("xmark", mD, j10, null, c2159n2, 390, 8);
                c2159n2.q(true);
                AbstractC1505f0.C("Scan the QR in Alma → Settings → Remote Access", AbstractC1529s.f19037h, r.c(0.85f, j10), androidx.compose.foundation.layout.a.m(AbstractC1505f0.H(bVar2.a(mVar2, L0.c.f2782x)), 0.0f, 0.0f, 0.0f, 40, 7), 0, null, 0, c2159n2, 438, 112);
                c2159n2.q(true);
            }
            AbstractC0008e.q(i10, c2159n2, i10, c0819h7);
            c0819h = C0821j.f13035d;
            C2136b.y(c0819h, pVarC, c2159n2);
            zBooleanValue = ((Boolean) v7.getValue()).booleanValue();
            mVar = m.f2794c;
            bVar = androidx.compose.foundation.layout.b.f7034a;
            if (zBooleanValue) {
                c2159n2.Z(-199300025);
                p pVarI2 = androidx.compose.foundation.layout.a.i(bVar.a(mVar, hVar), 32);
                C0533v c0533vA2 = AbstractC0532u.a(AbstractC0522j.g(12), L0.c.f2773Z, c2159n2, 54);
                i8 = c2159n2.f26125P;
                InterfaceC2149h0 interfaceC2149h0N4 = c2159n2.n();
                p pVarC4 = L0.a.c(pVarI2, c2159n2);
                c2159n2.c0();
                if (c2159n2.f26124O) {
                    c2159n2.m(c0820i2);
                } else {
                    c2159n2.m0();
                }
                C2136b.y(c0819h5, c0533vA2, c2159n2);
                C2136b.y(c0819h6, interfaceC2149h0N4, c2159n2);
                if (c2159n2.f26124O) {
                    AbstractC0008e.q(i8, c2159n2, i8, c0819h7);
                } else {
                    AbstractC0008e.q(i8, c2159n2, i8, c0819h7);
                }
                C2136b.y(c0819h, pVarC4, c2159n2);
                M m8 = AbstractC1529s.f19030a;
                long j11 = r.f4033c;
                c0819h2 = c0819h7;
                j7 = j8;
                AbstractC1502e.k("camera.fill", m8, r.c(0.6f, j11), null, c2159n2, 438, 8);
                c0820i = c0820i2;
                c0819h4 = c0819h;
                c0819h3 = c0819h5;
                eVar = eVar2;
                z7 = false;
                c2159n2 = c2159n;
                AbstractC1505f0.C("Camera access is off", AbstractC1529s.f19033d, j11, null, 0, null, 0, c2159n2, 438, 120);
                AbstractC1505f0.C("Allow the camera in Settings, or paste the pairing link instead.", AbstractC1529s.f19036g, r.c(0.6f, j11), null, 0, new k(3), 0, c2159n2, 438, 88);
                zJ = c2159n2.j(context);
                objN2 = c2159n2.N();
                if (zJ) {
                    q8 = q9;
                    if (objN2 == q8) {
                    }
                    q7 = q8;
                    AbstractC1505f0.c("Open Settings", null, j11, 0L, 0.0f, (G5.a) objN2, c2159n2, 390, 26);
                    c2159n2.q(true);
                    c2159n2.q(false);
                    mVar2 = mVar;
                    bVar2 = bVar;
                } else {
                    q8 = q9;
                }
                objN2 = new f(context, 0);
                c2159n2.j0(objN2);
                q7 = q8;
                AbstractC1505f0.c("Open Settings", null, j11, 0L, 0.0f, (G5.a) objN2, c2159n2, 390, 26);
                c2159n2.q(true);
                c2159n2.q(false);
                mVar2 = mVar;
                bVar2 = bVar;
            } else {
                c0819h2 = c0819h7;
                c0819h3 = c0819h5;
                j7 = j8;
                c0819h4 = c0819h;
                c0820i = c0820i2;
                q7 = q9;
                z7 = false;
                eVar = eVar2;
                if (((Boolean) v8.getValue()).booleanValue()) {
                    c2159n2.Z(-198483082);
                    a(function1, c2159n2, i9 & 14);
                    mVar2 = mVar;
                    bVar2 = bVar;
                    AbstractC0528p.a(AbstractC1464w5.a(androidx.compose.foundation.layout.d.j(bVar2.a(mVar2, hVar), 240), 2, r.c(0.9f, r.f4033c), p078l0.e.a(24)), c2159n2, 0);
                } else {
                    mVar2 = mVar;
                    bVar2 = bVar;
                    c2159n2.Z(-201716258);
                }
                c2159n2.q(false);
            }
            p pVarB3 = androidx.compose.foundation.a.b(V4.a(androidx.compose.foundation.layout.d.j(androidx.compose.foundation.layout.a.i(AbstractC0515c.o(mVar2), 16), 36), p078l0.e.f14650a), r.c(0.4f, j7), eVar);
            objN = c2159n2.N();
            if (objN == q7) {
                aVar2 = aVar;
                objN = new G(aVar2, 3);
                c2159n2.j0(objN);
            } else {
                aVar2 = aVar;
            }
            p pVarG2 = AbstractC1523o0.g(pVarB3, false, 0.0f, 0.0f, null, (G5.a) objN, 15);
            H hD3 = AbstractC0528p.d(hVar, z7);
            i7 = c2159n2.f26125P;
            InterfaceC2149h0 interfaceC2149h0N5 = c2159n2.n();
            p pVarC5 = L0.a.c(pVarG2, c2159n2);
            c2159n2.c0();
            if (c2159n2.f26124O) {
                c2159n2.m(c0820i);
            } else {
                c2159n2.m0();
            }
            C2136b.y(c0819h3, hD3, c2159n2);
            C2136b.y(c0819h6, interfaceC2149h0N5, c2159n2);
            if (c2159n2.f26124O) {
                AbstractC0008e.q(i7, c2159n2, i7, c0819h2);
            } else {
                AbstractC0008e.q(i7, c2159n2, i7, c0819h2);
            }
            C2136b.y(c0819h4, pVarC5, c2159n2);
            M mD2 = h1.d(AbstractC1529s.f19034e);
            long j12 = r.f4033c;
            AbstractC1502e.k("xmark", mD2, j12, null, c2159n2, 390, 8);
            c2159n2.q(true);
            AbstractC1505f0.C("Scan the QR in Alma → Settings → Remote Access", AbstractC1529s.f19037h, r.c(0.85f, j12), androidx.compose.foundation.layout.a.m(AbstractC1505f0.H(bVar2.a(mVar2, L0.c.f2782x)), 0.0f, 0.0f, 0.0f, 40, 7), 0, null, 0, c2159n2, 438, 112);
            c2159n2.q(true);
        } else {
            aVar2 = aVar;
            c2159n2.T();
        }
        m0 m0VarV = c2159n2.v();
        if (m0VarV != null) {
            m0VarV.f26106d = new g6(function1, aVar2, i);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static String e(c cVar) {
        SecureRandom secureRandom = AbstractC1922j.f22420a;
        byte[] bytes = (cVar.f20737a + "|" + cVar.f20739c).getBytes(AbstractC0139a.f3504a);
        l.d("getBytes(...)", bytes);
        byte[] bArrG = AbstractC1922j.g(bytes);
        StringBuilder sb = new StringBuilder();
        sb.append((CharSequence) ExtensionRequestData.EMPTY_VALUE);
        int i = 0;
        for (byte b2 : bArrG) {
            i++;
            if (i > 1) {
                sb.append((CharSequence) ExtensionRequestData.EMPTY_VALUE);
            }
            sb.append((CharSequence) String.format("%02x", Arrays.copyOf(new Object[]{Byte.valueOf(b2)}, 1)));
        }
        sb.append((CharSequence) ExtensionRequestData.EMPTY_VALUE);
        return "deviceId.".concat(P5.r.W(24, sb.toString()));
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static ArrayList f() {
        URI uri;
        String strI = AbstractC1966d.i("pairing.target");
        if (strI != null) {
            boolean z7 = AbstractC1966d.b().getBoolean("pairing.e2e", false);
            String strA = F.a("connectCode");
            if (strA == null) {
                strA = ExtensionRequestData.EMPTY_VALUE;
            }
            Object obj = null;
            try {
                uri = new URI(strI);
            } catch (Throwable unused) {
                uri = null;
            }
            if (uri != null && (!z7 || strA.length() != 0)) {
                ArrayList arrayListG = g();
                int size = arrayListG.size();
                int i = 0;
                while (i < size) {
                    Object obj2 = arrayListG.get(i);
                    i++;
                    c cVar = ((b) obj2).f20734b;
                    if (cVar.f20737a.equals(uri) && cVar.f20739c.equals(strA)) {
                        obj = obj2;
                        break;
                    }
                }
                b bVar = (b) obj;
                if (bVar != null) {
                    AbstractC1966d.f("pairings.active", bVar.f20733a);
                } else {
                    String string = UUID.randomUUID().toString();
                    l.d("toString(...)", string);
                    String upperCase = string.toUpperCase(Locale.ROOT);
                    l.d("toUpperCase(...)", upperCase);
                    if (z7) {
                        p129s5.n nVar = F.f23301a;
                        F.d(strA, "connectCode.".concat(upperCase));
                    }
                    List listJ = j();
                    Instant instantNow = Instant.now();
                    l.d("now(...)", instantNow);
                    k(p135t5.m.H(new d(upperCase, strI, z7, instantNow, null), listJ));
                    AbstractC1966d.f("pairings.active", upperCase);
                }
            }
            AbstractC1966d.d("pairing.target");
            AbstractC1966d.d("pairing.e2e");
            F.c("connectCode");
        }
        return g();
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static ArrayList g() {
        List<d> listJ = j();
        ArrayList arrayList = new ArrayList();
        for (d dVar : listJ) {
            b bVar = null;
            try {
                URI uri = new URI(dVar.f20741b);
                boolean z7 = dVar.f20742c;
                String str = ExtensionRequestData.EMPTY_VALUE;
                if (z7) {
                    p129s5.n nVar = F.f23301a;
                    String strA = F.a("connectCode.".concat(dVar.f20740a));
                    if (strA != null) {
                        str = strA;
                    }
                }
                if (!dVar.f20742c || str.length() != 0) {
                    bVar = new b(dVar.f20740a, new c(uri, dVar.f20742c, str), dVar.f20743d, dVar.f20744e);
                }
            } catch (Throwable unused) {
            }
            if (bVar != null) {
                arrayList.add(bVar);
            }
        }
        return arrayList;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00f6, code lost:
    
        r2 = new java.net.URI(r1);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static p110p6.c h(java.lang.String r13) {
        /*
            java.lang.String r0 = "raw"
            H5.l.e(r0, r13)
            java.lang.CharSequence r13 = P5.r.Y(r13)
            java.lang.String r13 = r13.toString()
            r0 = 0
            java.net.URI r1 = new java.net.URI     // Catch: java.lang.Throwable -> L17f
            r1.<init>(r13)     // Catch: java.lang.Throwable -> L17f
            java.lang.String r2 = r1.getScheme()
            java.lang.String r3 = "toLowerCase(...)"
            if (r2 == 0) goto L25
            java.util.Locale r4 = java.util.Locale.ROOT
            java.lang.String r2 = r2.toLowerCase(r4)
            H5.l.d(r3, r2)
            goto L26
        L25:
            r2 = r0
        L26:
            java.lang.String r4 = "alma"
            boolean r2 = H5.l.a(r2, r4)
            if (r2 != 0) goto L30
            goto L17f
        L30:
            java.lang.String r2 = r1.getHost()
            r4 = 63
            java.lang.String r5 = ""
            r6 = 1
            r7 = 0
            if (r2 != 0) goto L51
            java.lang.String r2 = r1.getRawSchemeSpecificPart()
            if (r2 != 0) goto L43
            r2 = r5
        L43:
            java.lang.String r2 = P5.r.V(r4, r2)
            char[] r8 = new char[r6]
            r9 = 47
            r8[r7] = r9
            java.lang.String r2 = P5.r.Z(r2, r8)
        L51:
            java.lang.String r8 = "connect"
            boolean r2 = H5.l.a(r2, r8)
            if (r2 != 0) goto L5b
            goto L17f
        L5b:
            java.lang.String r1 = r1.getRawQuery()
            if (r1 != 0) goto L65
            java.lang.String r1 = P5.r.S(r13, r4, r5)
        L65:
            char[] r13 = new char[r6]
            r2 = 38
            r13[r7] = r2
            java.util.List r13 = P5.r.Q(r1, r13)
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            java.util.Iterator r13 = r13.iterator()
        L78:
            boolean r2 = r13.hasNext()
            if (r2 == 0) goto L8f
            java.lang.Object r2 = r13.next()
            r4 = r2
            java.lang.String r4 = (java.lang.String) r4
            int r4 = r4.length()
            if (r4 <= 0) goto L78
            r1.add(r2)
            goto L78
        L8f:
            java.util.ArrayList r13 = new java.util.ArrayList
            r2 = 10
            int r2 = p135t5.n.m(r1, r2)
            r13.<init>(r2)
            int r2 = r1.size()
            r4 = r7
        L9f:
            if (r4 >= r2) goto Lce
            java.lang.Object r8 = r1.get(r4)
            int r4 = r4 + 1
            java.lang.String r8 = (java.lang.String) r8
            r9 = 61
            java.lang.String r10 = P5.r.V(r9, r8)
            java.lang.String r8 = P5.r.S(r8, r9, r5)
            java.lang.String r9 = "UTF-8"
            java.lang.String r10 = java.net.URLDecoder.decode(r10, r9)
            java.lang.String r11 = "+"
            java.lang.String r12 = "%2B"
            java.lang.String r8 = P5.y.m(r8, r11, r12)
            java.lang.String r8 = java.net.URLDecoder.decode(r8, r9)
            s5.i r9 = new s5.i
            r9.<init>(r10, r8)
            r13.add(r9)
            goto L9f
        Lce:
            int r1 = r13.size()
            r2 = r7
        Ld3:
            if (r2 >= r1) goto Le9
            java.lang.Object r4 = r13.get(r2)
            int r2 = r2 + 1
            r8 = r4
            s5.i r8 = (p129s5.i) r8
            java.lang.Object r8 = r8.f22783c
            java.lang.String r9 = "u"
            boolean r8 = H5.l.a(r8, r9)
            if (r8 == 0) goto Ld3
            goto Lea
        Le9:
            r4 = r0
        Lea:
            s5.i r4 = (p129s5.i) r4
            if (r4 == 0) goto L17f
            java.lang.Object r1 = r4.f22784d
            java.lang.String r1 = (java.lang.String) r1
            if (r1 != 0) goto Lf6
            goto L17f
        Lf6:
            java.net.URI r2 = new java.net.URI     // Catch: java.lang.Throwable -> L17f
            r2.<init>(r1)     // Catch: java.lang.Throwable -> L17f
            java.lang.String r1 = r2.getScheme()
            if (r1 == 0) goto L17f
            java.util.Locale r4 = java.util.Locale.ROOT
            java.lang.String r1 = r1.toLowerCase(r4)
            H5.l.d(r3, r1)
            java.lang.String r3 = "https"
            boolean r3 = r1.equals(r3)
            if (r3 != 0) goto L11c
            java.lang.String r3 = "http"
            boolean r1 = r1.equals(r3)
            if (r1 != 0) goto L11c
            goto L17f
        L11c:
            int r1 = r13.size()
            r3 = r7
        L121:
            if (r3 >= r1) goto L137
            java.lang.Object r4 = r13.get(r3)
            int r3 = r3 + 1
            r8 = r4
            s5.i r8 = (p129s5.i) r8
            java.lang.Object r8 = r8.f22783c
            java.lang.String r9 = "e2e"
            boolean r8 = H5.l.a(r8, r9)
            if (r8 == 0) goto L121
            goto L138
        L137:
            r4 = r0
        L138:
            s5.i r4 = (p129s5.i) r4
            if (r4 == 0) goto L141
            java.lang.Object r1 = r4.f22784d
            java.lang.String r1 = (java.lang.String) r1
            goto L142
        L141:
            r1 = r0
        L142:
            java.lang.String r3 = "1"
            boolean r1 = H5.l.a(r1, r3)
            int r3 = r13.size()
            r4 = r7
        L14d:
            if (r4 >= r3) goto L163
            java.lang.Object r8 = r13.get(r4)
            int r4 = r4 + 1
            r9 = r8
            s5.i r9 = (p129s5.i) r9
            java.lang.Object r9 = r9.f22783c
            java.lang.String r10 = "k"
            boolean r9 = H5.l.a(r9, r10)
            if (r9 == 0) goto L14d
            r0 = r8
        L163:
            s5.i r0 = (p129s5.i) r0
            if (r0 == 0) goto L16f
            java.lang.Object r13 = r0.f22784d
            java.lang.String r13 = (java.lang.String) r13
            if (r13 != 0) goto L16e
            goto L16f
        L16e:
            r5 = r13
        L16f:
            p6.c r13 = new p6.c
            if (r1 == 0) goto L17a
            int r0 = r5.length()
            if (r0 <= 0) goto L17a
            goto L17b
        L17a:
            r6 = r7
        L17b:
            r13.<init>(r2, r6, r5)
            return r13
        L17f:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: p110p6.a.h(java.lang.String):p6.c");
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static void i(String str) {
        List listJ = j();
        ArrayList arrayList = new ArrayList();
        for (Object obj : listJ) {
            if (!((d) obj).f20740a.equals(str)) {
                arrayList.add(obj);
            }
        }
        k(arrayList);
        p129s5.n nVar = F.f23301a;
        F.c("connectCode.".concat(str));
        if (l.a(AbstractC1966d.i("pairings.active"), str)) {
            AbstractC1966d.d("pairings.active");
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r10v0, resolved type: java.lang.String */
    /* JADX WARN: Multi-variable type inference failed */
    public static List j() {
        List<t6.D> listC;
        String strJ;
        t6.D dB;
        String strJ2;
        Instant instant;
        Boolean boolD;
        t6.D dC = AbstractC1966d.c("pairings.v2");
        if (dC == null || (listC = dC.c()) == null) {
            return t.f23290c;
        }
        ArrayList arrayList = new ArrayList();
        for (t6.D d7 : listC) {
            l.e("j", d7);
            t6.D dB2 = d7.b("id");
            Object dVar = null;
            if (dB2 != null && (strJ = dB2.j()) != null && (dB = d7.b("target")) != null && (strJ2 = dB.j()) != null) {
                t6.D dB3 = d7.b("e2e");
                boolean zBooleanValue = (dB3 == null || (boolD = dB3.d()) == null) ? false : boolD.booleanValue();
                t6.D dB4 = d7.b("pairedAt");
                String strJ3 = dB4 != null ? dB4.j() : null;
                if (strJ3 == null) {
                    instant = null;
                } else {
                    try {
                        try {
                            instant = Instant.parse(strJ3);
                        } catch (Throwable unused) {
                            instant = null;
                        }
                    } catch (Throwable unused2) {
                        instant = OffsetDateTime.parse(strJ3, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
                    }
                }
                if (instant == null) {
                    instant = Instant.now();
                }
                Instant instant2 = instant;
                l.b(instant2);
                t6.D dB5 = d7.b("name");
                dVar = new d(strJ, strJ2, zBooleanValue, instant2, dB5 != null ? dB5.j() : null);
            }
            if (dVar != null) {
                arrayList.add(dVar);
            }
        }
        return arrayList;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static void k(ArrayList arrayList) {
        p129s5.n nVar = AbstractC1966d.f23354a;
        ArrayList arrayList2 = new ArrayList(p135t5.n.m(arrayList, 10));
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            Object obj = arrayList.get(i);
            i++;
            d dVar = (d) obj;
            dVar.getClass();
            p040f6.q qVar = t6.D.f23298a;
            p129s5.i iVar = new p129s5.i("id", dVar.f20740a);
            p129s5.i iVar2 = new p129s5.i("target", dVar.f20741b);
            p129s5.i iVar3 = new p129s5.i("e2e", Boolean.valueOf(dVar.f20742c));
            String str = DateTimeFormatter.ISO_INSTANT.format(dVar.f20743d);
            l.d("format(...)", str);
            arrayList2.add(t6.G.g(iVar, iVar2, iVar3, new p129s5.i("pairedAt", str), new p129s5.i("name", dVar.f20744e)));
        }
        AbstractC1966d.h("pairings.v2", new x(arrayList2));
    }
}
