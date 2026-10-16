package mindustry.web;

import org.teavm.jso.*;
import org.teavm.jso.dom.html.*;

import java.io.*;
import java.nio.*;

/** Calls into the page's window.mindustryHost (mindustry-host.js). */
public class WebHost{
    static String clipboard = "";

    @JSBody(script = "return window.mindustryHost.canvas;")
    static native HTMLCanvasElement canvas();

    @JSBody(script = "return document.hidden;")
    static native boolean hidden();

    @JSFunctor
    interface VisibilityListener extends JSObject{
        void changed(boolean visible);
    }

    @JSBody(params = "listener", script = "window.mindustryHost.onVisibility = listener;")
    static native void onVisibility(VisibilityListener listener);

    @JSBody(params = "text", script = "window.mindustryHost.writeClipboard(text);")
    static native void writeClipboard(String text);

    @JSBody(params = "uri", script = "window.mindustryHost.openURI(uri);")
    static native void openURI(String uri);


    @JSBody(script = "window.mindustryHost.exit();")
    static native void exit();

    @JSFunctor
    interface Picked extends JSObject{
        void picked(String name, org.teavm.jso.typedarrays.Int8Array data);
    }

    @JSBody(params = {"accept", "done"}, script = "window.mindustryHost.pickFile(accept, done);")
    static native void pickFile(String accept, Picked done);

    @JSBody(params = {"pixels", "w", "h"}, script = "return window.mindustryHost.cursorUrl(new Uint8ClampedArray(pixels.buffer, pixels.byteOffset, w * h * 4), w, h);")
    static native String cursorUrl(@JSBuffer(JSBufferType.UINT8) ByteBuffer pixels, int w, int h);

    @JSBody(params = "message", script = "window.mindustryHost.crash(message);")
    static native void crash(String message);

    @JSBody(params = "message", script = "window.mindustryHost.log(message);")
    static native void log(String message);

    @JSBody(script = "window.mindustryHost.ready();")
    static native void ready();

    /** The JS stack of a Java exception; only readable in unobfuscated builds. */
    @JSBody(params = "t", script = "var e = t.$jsException; return e && e.stack ? String(e.stack) : '';")
    static native String jsStack(Throwable t);

    static void crash(Throwable t){
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        for(Throwable c = t; c != null; c = c.getCause()){
            String stack = jsStack(c);
            if(!stack.isEmpty()) out.append("\n--- ").append(String.valueOf(c)).append("\n").append(stack);
        }
        crash(out.toString());
    }
}
