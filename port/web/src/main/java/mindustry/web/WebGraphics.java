package mindustry.web;

import arc.*;
import arc.graphics.*;
import arc.graphics.Pixmap;
import arc.Graphics.Cursor.*;
import arc.graphics.gl.*;
import arc.struct.*;
import org.teavm.jso.*;
import org.teavm.jso.dom.html.*;
import org.teavm.jso.webgl.*;

public class WebGraphics extends Graphics{
    final HTMLCanvasElement canvas;
    final WebGLRenderingContext context;
    final WebGL20 gl20;
    final GLVersion glVersion;
    final BufferFormat bufferFormat = new BufferFormat(8, 8, 8, 8, 16, 8, 0, false);
    final ObjectSet<String> extensions = new ObjectSet<>();

    private double lastFrameTime = -1, frameCounterStart;
    private float deltaTime;
    private long frameId;
    private int frames, fps;
    int width, height, backWidth, backHeight;
    float density = 1f;

    WebGraphics(HTMLCanvasElement canvas){
        this.canvas = canvas;
        context = createContext(canvas);
        if(context == null) throw new RuntimeException("WebGL 2 is not available.");

        String[] ext = supportedExtensions(context);
        for(String e : ext) extensions.add(e);
        //needed for float textures used by the light/fog buffers
        enableExtension(context, "EXT_color_buffer_float");
        enableExtension(context, "EXT_color_buffer_half_float");
        enableExtension(context, "OES_texture_float_linear");

        Core.gl = Core.gl20 = gl20 = new WebGL20(context);
        glVersion = new GLVersion(Application.ApplicationType.web, gl20.glGetString(GL20.GL_VERSION), gl20.glGetString(GL20.GL_VENDOR), gl20.glGetString(GL20.GL_RENDERER));
    }

    @JSBody(params = "canvas", script =
        "var gl = canvas.getContext('webgl2', {alpha: false, antialias: false, depth: true, stencil: true, premultipliedAlpha: false, preserveDrawingBuffer: false, powerPreference: 'high-performance'});" +
        "return gl || null;")
    static native WebGLRenderingContext createContext(HTMLCanvasElement canvas);

    @JSBody(params = "gl", script = "return gl.getSupportedExtensions() || [];")
    static native String[] supportedExtensions(WebGLRenderingContext gl);

    @JSBody(params = {"gl", "name"}, script = "return gl.getExtension(name) != null;")
    static native boolean enableExtension(WebGLRenderingContext gl, String name);

    @JSBody(params = "canvas", script = "return canvas.clientWidth | 0;")
    static native int clientWidth(HTMLCanvasElement canvas);

    @JSBody(params = "canvas", script = "return canvas.clientHeight | 0;")
    static native int clientHeight(HTMLCanvasElement canvas);

    @JSBody(script = "return window.devicePixelRatio || 1;")
    static native double pixelRatio();

    void update(){
        double time = WebApplication.now();
        if(lastFrameTime < 0) lastFrameTime = time;
        deltaTime = (float)((time - lastFrameTime) / 1000.0);
        //a hidden tab stops frames; don't let the game jump ahead when it comes back
        if(deltaTime > 0.25f) deltaTime = 1f / 60f;
        lastFrameTime = time;

        if(time - frameCounterStart >= 1000){
            fps = frames;
            frames = 0;
            frameCounterStart = time;
        }
        frames++;
        frameId++;
    }

    /** @return whether the size changed. */
    boolean updateSize(){
        double ratio = pixelRatio();
        int w = Math.max(clientWidth(canvas), 1), h = Math.max(clientHeight(canvas), 1);
        int bw = (int)Math.round(w * ratio), bh = (int)Math.round(h * ratio);
        if(bw == backWidth && bh == backHeight) return false;
        backWidth = bw;
        backHeight = bh;
        //the game works in physical pixels, like the desktop build with HdpiMode.pixels
        width = bw;
        height = bh;
        density = (float)ratio;
        canvas.setWidth(bw);
        canvas.setHeight(bh);
        gl20.glViewport(0, 0, bw, bh);
        return true;
    }

    @Override
    public boolean isGL30Available(){
        return false;
    }

    @Override
    public GL20 getGL20(){
        return gl20;
    }

    @Override
    public void setGL20(GL20 gl20){
        Core.gl = Core.gl20 = gl20;
    }

    @Override
    public GL30 getGL30(){
        return null;
    }

    @Override
    public void setGL30(GL30 gl30){
    }

    @Override
    public int getWidth(){
        return width;
    }

    @Override
    public int getHeight(){
        return height;
    }

    @Override
    public int getBackBufferWidth(){
        return backWidth;
    }

    @Override
    public int getBackBufferHeight(){
        return backHeight;
    }

    @Override
    public boolean isHidden(){
        return WebHost.hidden();
    }

    @Override
    public long getFrameId(){
        return frameId;
    }

    @Override
    public float getDeltaTime(){
        return deltaTime;
    }

    @Override
    public int getFramesPerSecond(){
        return fps;
    }

    @Override
    public GLVersion getGLVersion(){
        return glVersion;
    }

    @Override
    public float getPpiX(){
        return 96 * density;
    }

    @Override
    public float getPpiY(){
        return 96 * density;
    }

    @Override
    public float getPpcX(){
        return 96 * density / 2.54f;
    }

    @Override
    public float getPpcY(){
        return 96 * density / 2.54f;
    }

    @Override
    public float getDensity(){
        return density;
    }

    @Override
    public boolean setFullscreen(boolean fullscreen){
        //the page's own button owns full screen
        return false;
    }

    @Override
    public boolean isFullscreen(){
        return false;
    }

    @Override
    public void setTitle(String title){
    }

    @Override
    public void setVSync(boolean vsync){
    }

    @Override
    public BufferFormat getBufferFormat(){
        return bufferFormat;
    }

    @Override
    public boolean supportsExtension(String extension){
        return extensions.contains(extension);
    }

    @Override
    public boolean isContinuousRendering(){
        return true;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous){
    }

    @Override
    public void requestRendering(){
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot){
        return new WebCursor(WebHost.cursorUrl(pixmap.pixels, pixmap.width, pixmap.height), xHotspot, yHotspot);
    }

    @Override
    protected void setCursor(Cursor cursor){
        WebCursor c = (WebCursor)cursor;
        setCanvasCursor(canvas, "url(" + c.url + ") " + c.x + " " + c.y + ", auto");
    }

    @Override
    protected void setSystemCursor(SystemCursor cursor){
        String name = switch(cursor){
            case arrow -> "default";
            case ibeam -> "text";
            case crosshair -> "crosshair";
            case hand -> "pointer";
            case horizontalResize -> "ew-resize";
            case verticalResize -> "ns-resize";
        };
        setCanvasCursor(canvas, name);
    }

    @JSBody(params = {"canvas", "cursor"}, script = "canvas.style.cursor = cursor;")
    static native void setCanvasCursor(HTMLCanvasElement canvas, String cursor);

    static class WebCursor implements Cursor{
        final String url;
        final int x, y;

        WebCursor(String url, int x, int y){
            this.url = url;
            this.x = x;
            this.y = y;
        }

        @Override
        public void dispose(){
        }
    }
}
