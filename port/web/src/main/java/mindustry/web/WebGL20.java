package mindustry.web;

import arc.graphics.*;
import org.teavm.jso.*;
import org.teavm.jso.webgl.*;

import java.nio.*;
import java.util.*;

/**
 * GL20 on a WebGL 2 context. GL object names are small ints mapped to WebGL objects; the
 * int is stored on the object as $id so getParameter results can be mapped back.
 */
public class WebGL20 implements GL20{
    final WebGLRenderingContext gl;
    private final ArrayList<JSObject> objects = new ArrayList<>();
    private final ArrayList<Integer> free = new ArrayList<>();
    private final ArrayList<WebGLUniformLocation> uniforms = new ArrayList<>();
    private final HashMap<Integer, HashMap<String, Integer>> uniformIds = new HashMap<>();

    public WebGL20(WebGLRenderingContext gl){
        this.gl = gl;
        objects.add(null);
        uniforms.add(null);
    }

    int add(JSObject obj){
        if(obj == null) return 0;
        int id;
        if(!free.isEmpty()){
            id = free.remove(free.size() - 1);
            objects.set(id, obj);
        }else{
            id = objects.size();
            objects.add(obj);
        }
        setId(obj, id);
        return id;
    }

    @SuppressWarnings("unchecked")
    <T extends JSObject> T get(int id){
        return id <= 0 || id >= objects.size() ? null : (T)objects.get(id);
    }

    void remove(int id){
        if(id > 0 && id < objects.size() && objects.get(id) != null){
            objects.set(id, null);
            free.add(id);
        }
    }

    @JSBody(params = {"obj", "id"}, script = "obj.$id = id;")
    static native void setId(JSObject obj, int id);

    @JSBody(params = "obj", script = "return obj && obj.$id ? obj.$id : 0;")
    static native int getId(JSObject obj);

    static int elementSize(Buffer buffer){
        if(buffer instanceof ByteBuffer) return 1;
        if(buffer instanceof ShortBuffer || buffer instanceof CharBuffer) return 2;
        if(buffer instanceof DoubleBuffer || buffer instanceof LongBuffer) return 8;
        return 4;
    }

    /** Byte range of a buffer starting at its position: 0 = Uint8, 1 = Uint16, 2 = Float32, 3 = Int32 */
    @JSBody(params = {"view", "offset", "length", "kind"}, script =
        "var b = view.buffer, o = view.byteOffset + offset;" +
        "if(kind == 1) return new Uint16Array(b, o, length >> 1);" +
        "if(kind == 2) return new Float32Array(b, o, length >> 2);" +
        "if(kind == 3) return new Int32Array(b, o, length >> 2);" +
        "return new Uint8Array(b, o, length);")
    static native JSObject slice(@JSBuffer(JSBufferType.DATA_VIEW) Buffer view, int offset, int length, int kind);

    static JSObject bytes(Buffer buffer, int byteLength, int kind){
        int size = elementSize(buffer);
        int offset = buffer.position() * size;
        if(byteLength < 0) byteLength = buffer.remaining() * size;
        return slice(buffer, offset, byteLength, kind);
    }

    static int kindFor(int type){
        switch(type){
            case GL_UNSIGNED_SHORT:
            case GL_UNSIGNED_SHORT_4_4_4_4:
            case GL_UNSIGNED_SHORT_5_5_5_1:
            case GL_UNSIGNED_SHORT_5_6_5:
            case 0x8D61: //half float (GLES 3)
            case 0x140B: //half float
                return 1;
            case GL_FLOAT:
                return 2;
            case GL_INT:
            case GL_UNSIGNED_INT:
                return 3;
            default:
                return 0;
        }
    }

    static int bytesPerPixel(int format, int type){
        int channels;
        switch(format){
            case GL_ALPHA:
            case GL_LUMINANCE:
            case 0x1903: //red
                channels = 1;
                break;
            case GL_LUMINANCE_ALPHA:
            case 0x8227: //rg
                channels = 2;
                break;
            case GL_RGB:
                channels = 3;
                break;
            default:
                channels = 4;
        }
        switch(type){
            case GL_UNSIGNED_SHORT_4_4_4_4:
            case GL_UNSIGNED_SHORT_5_5_5_1:
            case GL_UNSIGNED_SHORT_5_6_5:
                return 2;
            case GL_FLOAT:
                return channels * 4;
            case 0x8D61:
            case 0x140B:
                return channels * 2;
            default:
                return channels;
        }
    }

    @Override
    public void glActiveTexture(int texture){
        gl.activeTexture(texture);
    }

    @Override
    public void glBindTexture(int target, int texture){
        gl.bindTexture(target, get(texture));
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor){
        gl.blendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask){
        gl.clear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha){
        gl.clearColor(red, green, blue, alpha);
    }

    @Override
    public void glClearDepthf(float depth){
        gl.clearDepth(depth);
    }

    @Override
    public void glClearStencil(int s){
        gl.clearStencil(s);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha){
        gl.colorMask(red, green, blue, alpha);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data){
        compressedTexImage2D(gl, target, level, internalformat, width, height, border, bytes(data, imageSize, 0));
    }

    @JSBody(params = {"gl", "target", "level", "internalformat", "width", "height", "border", "data"}, script = "gl.compressedTexImage2D(target, level, internalformat, width, height, border, data);")
    static native void compressedTexImage2D(WebGLRenderingContext gl, int target, int level, int internalformat, int width, int height, int border, JSObject data);

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data){
        compressedTexSubImage2D(gl, target, level, xoffset, yoffset, width, height, format, bytes(data, imageSize, 0));
    }

    @JSBody(params = {"gl", "target", "level", "x", "y", "width", "height", "format", "data"}, script = "gl.compressedTexSubImage2D(target, level, x, y, width, height, format, data);")
    static native void compressedTexSubImage2D(WebGLRenderingContext gl, int target, int level, int x, int y, int width, int height, int format, JSObject data);

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border){
        gl.copyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height){
        gl.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glCullFace(int mode){
        gl.cullFace(mode);
    }

    @Override
    public void glDeleteTexture(int texture){
        gl.deleteTexture(get(texture));
        remove(texture);
    }

    @Override
    public void glDepthFunc(int func){
        gl.depthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag){
        gl.depthMask(flag);
    }

    @Override
    public void glDepthRangef(float zNear, float zFar){
        gl.depthRange(zNear, zFar);
    }

    @Override
    public void glDisable(int cap){
        gl.disable(cap);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count){
        gl.drawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices){
        //client-side index arrays don't exist in WebGL; Mesh always uses buffers here
        gl.drawElements(mode, count, type, indices.position() * elementSize(indices));
    }

    @Override
    public void glEnable(int cap){
        gl.enable(cap);
    }

    @Override
    public void glFinish(){
        gl.finish();
    }

    @Override
    public void glFlush(){
        gl.flush();
    }

    @Override
    public void glFrontFace(int mode){
        gl.frontFace(mode);
    }

    @Override
    public int glGenTexture(){
        return add(gl.createTexture());
    }

    @Override
    public int glGetError(){
        return gl.getError();
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer params){
        int pos = params.position();
        switch(pname){
            case GL_VIEWPORT:
            case GL_SCISSOR_BOX:
            case 0x846D: //aliased point size range
            case GL_ALIASED_LINE_WIDTH_RANGE:
            case GL_MAX_VIEWPORT_DIMS:
                int[] values = intArray(gl, pname);
                for(int i = 0; i < values.length && pos + i < params.limit(); i++) params.put(pos + i, values[i]);
                break;
            case GL_FRAMEBUFFER_BINDING:
            case GL_RENDERBUFFER_BINDING:
            case GL_TEXTURE_BINDING_2D:
            case GL_TEXTURE_BINDING_CUBE_MAP:
            case GL_ARRAY_BUFFER_BINDING:
            case GL_ELEMENT_ARRAY_BUFFER_BINDING:
            case GL_CURRENT_PROGRAM:
                params.put(pos, getId(gl.getParameter(pname)));
                break;
            default:
                params.put(pos, intParameter(gl, pname));
        }
    }

    @JSBody(params = {"gl", "pname"}, script = "var v = gl.getParameter(pname); return v == null ? [] : Array.from(v, function(x){ return x | 0; });")
    static native int[] intArray(WebGLRenderingContext gl, int pname);

    @JSBody(params = {"gl", "pname"}, script = "var v = gl.getParameter(pname); return typeof v === 'boolean' ? (v ? 1 : 0) : (v | 0);")
    static native int intParameter(WebGLRenderingContext gl, int pname);

    @Override
    public String glGetString(int name){
        String s = stringParameter(gl, name);
        return s == null ? "" : s;
    }

    @JSBody(params = {"gl", "pname"}, script = "var v = gl.getParameter(pname); return v == null ? null : '' + v;")
    static native String stringParameter(WebGLRenderingContext gl, int pname);

    @Override
    public void glHint(int target, int mode){
        gl.hint(target, mode);
    }

    @Override
    public void glLineWidth(float width){
        gl.lineWidth(width);
    }

    @Override
    public void glPixelStorei(int pname, int param){
        gl.pixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units){
        gl.polygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels){
        readPixels(gl, x, y, width, height, format, type, bytes(pixels, width * height * bytesPerPixel(format, type), kindFor(type)));
    }

    @JSBody(params = {"gl", "x", "y", "w", "h", "format", "type", "data"}, script = "gl.readPixels(x, y, w, h, format, type, data);")
    static native void readPixels(WebGLRenderingContext gl, int x, int y, int w, int h, int format, int type, JSObject data);

    @Override
    public void glScissor(int x, int y, int width, int height){
        gl.scissor(x, y, width, height);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask){
        gl.stencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask){
        gl.stencilMask(mask);
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass){
        gl.stencilOp(fail, zfail, zpass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels){
        if(pixels == null){
            texImage2D(gl, target, level, internalformat, width, height, border, format, type, null);
        }else{
            texImage2D(gl, target, level, internalformat, width, height, border, format, type, bytes(pixels, width * height * bytesPerPixel(format, type), kindFor(type)));
        }
    }

    @JSBody(params = {"gl", "target", "level", "internalformat", "w", "h", "border", "format", "type", "data"}, script = "gl.texImage2D(target, level, internalformat, w, h, border, format, type, data);")
    static native void texImage2D(WebGLRenderingContext gl, int target, int level, int internalformat, int w, int h, int border, int format, int type, JSObject data);

    @Override
    public void glTexParameterf(int target, int pname, float param){
        gl.texParameterf(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels){
        texSubImage2D(gl, target, level, xoffset, yoffset, width, height, format, type, bytes(pixels, width * height * bytesPerPixel(format, type), kindFor(type)));
    }

    @JSBody(params = {"gl", "target", "level", "x", "y", "w", "h", "format", "type", "data"}, script = "gl.texSubImage2D(target, level, x, y, w, h, format, type, data);")
    static native void texSubImage2D(WebGLRenderingContext gl, int target, int level, int x, int y, int w, int h, int format, int type, JSObject data);

    @Override
    public void glViewport(int x, int y, int width, int height){
        gl.viewport(x, y, width, height);
    }

    @Override
    public void glAttachShader(int program, int shader){
        gl.attachShader(get(program), get(shader));
    }

    @Override
    public void glBindAttribLocation(int program, int index, String name){
        gl.bindAttribLocation(get(program), index, name);
    }

    @Override
    public void glBindBuffer(int target, int buffer){
        gl.bindBuffer(target, get(buffer));
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer){
        gl.bindFramebuffer(target, get(framebuffer));
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer){
        gl.bindRenderbuffer(target, get(renderbuffer));
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha){
        gl.blendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendEquation(int mode){
        gl.blendEquation(mode);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha){
        gl.blendEquationSeparate(modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha){
        gl.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int usage){
        if(data == null){
            gl.bufferData(target, size, usage);
        }else{
            bufferData(gl, target, bytes(data, size, 0), usage);
        }
    }

    @JSBody(params = {"gl", "target", "data", "usage"}, script = "gl.bufferData(target, data, usage);")
    static native void bufferData(WebGLRenderingContext gl, int target, JSObject data, int usage);

    @Override
    public void glBufferSubData(int target, int offset, int size, Buffer data){
        bufferSubData(gl, target, offset, bytes(data, size, 0));
    }

    @JSBody(params = {"gl", "target", "offset", "data"}, script = "gl.bufferSubData(target, offset, data);")
    static native void bufferSubData(WebGLRenderingContext gl, int target, int offset, JSObject data);

    @Override
    public int glCheckFramebufferStatus(int target){
        return gl.checkFramebufferStatus(target);
    }

    @Override
    public void glCompileShader(int shader){
        gl.compileShader(get(shader));
    }

    @Override
    public int glCreateProgram(){
        return add(gl.createProgram());
    }

    @Override
    public int glCreateShader(int type){
        return add(gl.createShader(type));
    }

    @Override
    public void glDeleteBuffer(int buffer){
        gl.deleteBuffer(get(buffer));
        remove(buffer);
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer){
        gl.deleteFramebuffer(get(framebuffer));
        remove(framebuffer);
    }

    @Override
    public void glDeleteProgram(int program){
        gl.deleteProgram(get(program));
        HashMap<String, Integer> ids = uniformIds.remove(program);
        if(ids != null){
            for(int id : ids.values()) if(id > 0) uniforms.set(id, null);
        }
        remove(program);
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer){
        gl.deleteRenderbuffer(get(renderbuffer));
        remove(renderbuffer);
    }

    @Override
    public void glDeleteShader(int shader){
        gl.deleteShader(get(shader));
        remove(shader);
    }

    @Override
    public void glDetachShader(int program, int shader){
        gl.detachShader(get(program), get(shader));
    }

    @Override
    public void glDisableVertexAttribArray(int index){
        gl.disableVertexAttribArray(index);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, int indices){
        gl.drawElements(mode, count, type, indices);
    }

    @Override
    public void glEnableVertexAttribArray(int index){
        gl.enableVertexAttribArray(index);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer){
        gl.framebufferRenderbuffer(target, attachment, renderbuffertarget, get(renderbuffer));
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level){
        gl.framebufferTexture2D(target, attachment, textarget, get(texture), level);
    }

    @Override
    public int glGenBuffer(){
        return add(gl.createBuffer());
    }

    @Override
    public void glGenerateMipmap(int target){
        gl.generateMipmap(target);
    }

    @Override
    public int glGenFramebuffer(){
        return add(gl.createFramebuffer());
    }

    @Override
    public int glGenRenderbuffer(){
        return add(gl.createRenderbuffer());
    }

    @Override
    public String glGetActiveAttrib(int program, int index, IntBuffer size, IntBuffer type){
        WebGLActiveInfo info = gl.getActiveAttrib(get(program), index);
        if(info == null) return null;
        size.put(size.position(), info.getSize());
        type.put(type.position(), info.getType());
        return info.getName();
    }

    @Override
    public String glGetActiveUniform(int program, int index, IntBuffer size, IntBuffer type){
        WebGLActiveInfo info = gl.getActiveUniform(get(program), index);
        if(info == null) return null;
        size.put(size.position(), info.getSize());
        type.put(type.position(), info.getType());
        return info.getName();
    }

    @Override
    public int glGetAttribLocation(int program, String name){
        return gl.getAttribLocation(get(program), name);
    }

    @Override
    public void glGetBooleanv(int pname, Buffer params){
        int v = intParameter(gl, pname);
        if(params instanceof ByteBuffer b) b.put(b.position(), (byte)v);
        else if(params instanceof IntBuffer b) b.put(b.position(), v);
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params){
        params.put(params.position(), toInt(gl.getBufferParameter(target, pname)));
    }

    @JSBody(params = "v", script = "return typeof v === 'boolean' ? (v ? 1 : 0) : (v | 0);")
    static native int toInt(JSObject v);

    @Override
    public void glGetFloatv(int pname, FloatBuffer params){
        float[] values = floatArray(gl, pname);
        int pos = params.position();
        for(int i = 0; i < values.length && pos + i < params.limit(); i++) params.put(pos + i, values[i]);
    }

    @JSBody(params = {"gl", "pname"}, script = "var v = gl.getParameter(pname); if(v == null) return []; if(typeof v === 'number') return [v]; return Array.from(v);")
    static native float[] floatArray(WebGLRenderingContext gl, int pname);

    @Override
    public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params){
        params.put(params.position(), objectOrInt(gl.getFramebufferAttachmentParameter(target, attachment, pname)));
    }

    @JSBody(params = "v", script = "if(v == null) return 0; if(typeof v === 'object') return v.$id | 0; return typeof v === 'boolean' ? (v ? 1 : 0) : (v | 0);")
    static native int objectOrInt(JSObject v);

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params){
        WebGLProgram p = get(program);
        int value;
        if(pname == GL_INFO_LOG_LENGTH){
            String log = gl.getProgramInfoLog(p);
            value = log == null ? 0 : log.length() + 1;
        }else{
            value = toInt(gl.getProgramParameter(p, pname));
        }
        params.put(params.position(), value);
    }

    @Override
    public String glGetProgramInfoLog(int program){
        String log = gl.getProgramInfoLog(get(program));
        return log == null ? "" : log;
    }

    @Override
    public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params){
        params.put(params.position(), toInt(gl.getRenderbufferParameter(target, pname)));
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params){
        WebGLShader s = get(shader);
        int value;
        if(pname == GL_INFO_LOG_LENGTH){
            String log = gl.getShaderInfoLog(s);
            value = log == null ? 0 : log.length() + 1;
        }else{
            value = toInt(gl.getShaderParameter(s, pname));
        }
        params.put(params.position(), value);
    }

    @Override
    public String glGetShaderInfoLog(int shader){
        String log = gl.getShaderInfoLog(get(shader));
        return log == null ? "" : log;
    }

    @Override
    public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision){
        WebGLShaderPrecisionFormat f = gl.getShaderPrecisionFormat(shadertype, precisiontype);
        range.put(range.position(), f.getRangeMin());
        range.put(range.position() + 1, f.getRangeMax());
        precision.put(precision.position(), f.getPrecision());
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params){
        params.put(params.position(), toFloat(gl.getTexParameter(target, pname)));
    }

    @JSBody(params = "v", script = "return +v || 0;")
    static native float toFloat(JSObject v);

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params){
        params.put(params.position(), toInt(gl.getTexParameter(target, pname)));
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params){
        params.put(params.position(), toFloat(gl.getUniform(get(program), uniforms.get(location))));
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params){
        params.put(params.position(), toInt(gl.getUniform(get(program), uniforms.get(location))));
    }

    @Override
    public int glGetUniformLocation(int program, String name){
        HashMap<String, Integer> ids = uniformIds.computeIfAbsent(program, k -> new HashMap<>());
        Integer cached = ids.get(name);
        if(cached != null) return cached;
        WebGLUniformLocation loc = gl.getUniformLocation(get(program), name);
        int id = -1;
        if(loc != null){
            id = uniforms.size();
            uniforms.add(loc);
        }
        ids.put(name, id);
        return id;
    }

    WebGLUniformLocation loc(int location){
        return location <= 0 || location >= uniforms.size() ? null : uniforms.get(location);
    }

    @Override
    public void glGetVertexAttribfv(int index, int pname, FloatBuffer params){
        params.put(params.position(), toFloat(gl.getVertexAttrib(index, pname)));
    }

    @Override
    public void glGetVertexAttribiv(int index, int pname, IntBuffer params){
        params.put(params.position(), objectOrInt(gl.getVertexAttrib(index, pname)));
    }

    @Override
    public boolean glIsBuffer(int buffer){
        return buffer > 0 && gl.isBuffer(get(buffer));
    }

    @Override
    public boolean glIsEnabled(int cap){
        return gl.isEnabled(cap);
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer){
        return framebuffer > 0 && gl.isFramebuffer(get(framebuffer));
    }

    @Override
    public boolean glIsProgram(int program){
        return program > 0 && gl.isProgram(get(program));
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer){
        return renderbuffer > 0 && gl.isRenderbuffer(get(renderbuffer));
    }

    @Override
    public boolean glIsShader(int shader){
        return shader > 0 && gl.isShader(get(shader));
    }

    @Override
    public boolean glIsTexture(int texture){
        return texture > 0 && gl.isTexture(get(texture));
    }

    @Override
    public void glLinkProgram(int program){
        gl.linkProgram(get(program));
    }

    @Override
    public void glReleaseShaderCompiler(){
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height){
        //GL_DEPTH24_STENCIL8_OES and friends: WebGL 2 knows them under the same numbers
        gl.renderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glSampleCoverage(float value, boolean invert){
        gl.sampleCoverage(value, invert);
    }

    @Override
    public void glShaderSource(int shader, String string){
        gl.shaderSource(get(shader), string);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask){
        gl.stencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask){
        gl.stencilMaskSeparate(face, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass){
        gl.stencilOpSeparate(face, fail, zfail, zpass);
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params){
        gl.texParameterf(target, pname, params.get(params.position()));
    }

    @Override
    public void glTexParameteri(int target, int pname, int param){
        gl.texParameteri(target, pname, param);
    }

    @Override
    public void glTexParameteriv(int target, int pname, IntBuffer params){
        gl.texParameteri(target, pname, params.get(params.position()));
    }

    @JSBody(params = {"arr", "offset", "length"}, script = "return arr.subarray(offset, offset + length);")
    static native JSObject sub(@JSByRef float[] arr, int offset, int length);

    @JSBody(params = {"arr", "offset", "length"}, script = "return arr.subarray(offset, offset + length);")
    static native JSObject sub(@JSByRef int[] arr, int offset, int length);

    @JSBody(params = {"gl", "fn", "loc", "data"}, script = "gl[fn](loc, data);")
    static native void uniformv(WebGLRenderingContext gl, String fn, WebGLUniformLocation loc, JSObject data);

    @JSBody(params = {"gl", "fn", "loc", "transpose", "data"}, script = "gl[fn](loc, transpose, data);")
    static native void uniformMatrix(WebGLRenderingContext gl, String fn, WebGLUniformLocation loc, boolean transpose, JSObject data);

    void uniformf(String fn, int location, int count, int n, FloatBuffer v){
        WebGLUniformLocation l = loc(location);
        if(l != null) uniformv(gl, fn, l, bytes(v, count * n * 4, 2));
    }

    void uniformf(String fn, int location, int count, int n, float[] v, int offset){
        WebGLUniformLocation l = loc(location);
        if(l != null) uniformv(gl, fn, l, sub(v, offset, count * n));
    }

    void uniformi(String fn, int location, int count, int n, IntBuffer v){
        WebGLUniformLocation l = loc(location);
        if(l != null) uniformv(gl, fn, l, bytes(v, count * n * 4, 3));
    }

    void uniformi(String fn, int location, int count, int n, int[] v, int offset){
        WebGLUniformLocation l = loc(location);
        if(l != null) uniformv(gl, fn, l, sub(v, offset, count * n));
    }

    @Override
    public void glUniform1f(int location, float x){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform1f(l, x);
    }

    @Override
    public void glUniform1fv(int location, int count, FloatBuffer v){
        uniformf("uniform1fv", location, count, 1, v);
    }

    @Override
    public void glUniform1fv(int location, int count, float[] v, int offset){
        uniformf("uniform1fv", location, count, 1, v, offset);
    }

    @Override
    public void glUniform1i(int location, int x){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform1i(l, x);
    }

    @Override
    public void glUniform1iv(int location, int count, IntBuffer v){
        uniformi("uniform1iv", location, count, 1, v);
    }

    @Override
    public void glUniform1iv(int location, int count, int[] v, int offset){
        uniformi("uniform1iv", location, count, 1, v, offset);
    }

    @Override
    public void glUniform2f(int location, float x, float y){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform2f(l, x, y);
    }

    @Override
    public void glUniform2fv(int location, int count, FloatBuffer v){
        uniformf("uniform2fv", location, count, 2, v);
    }

    @Override
    public void glUniform2fv(int location, int count, float[] v, int offset){
        uniformf("uniform2fv", location, count, 2, v, offset);
    }

    @Override
    public void glUniform2i(int location, int x, int y){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform2i(l, x, y);
    }

    @Override
    public void glUniform2iv(int location, int count, IntBuffer v){
        uniformi("uniform2iv", location, count, 2, v);
    }

    @Override
    public void glUniform2iv(int location, int count, int[] v, int offset){
        uniformi("uniform2iv", location, count, 2, v, offset);
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform3f(l, x, y, z);
    }

    @Override
    public void glUniform3fv(int location, int count, FloatBuffer v){
        uniformf("uniform3fv", location, count, 3, v);
    }

    @Override
    public void glUniform3fv(int location, int count, float[] v, int offset){
        uniformf("uniform3fv", location, count, 3, v, offset);
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform3i(l, x, y, z);
    }

    @Override
    public void glUniform3iv(int location, int count, IntBuffer v){
        uniformi("uniform3iv", location, count, 3, v);
    }

    @Override
    public void glUniform3iv(int location, int count, int[] v, int offset){
        uniformi("uniform3iv", location, count, 3, v, offset);
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform4f(l, x, y, z, w);
    }

    @Override
    public void glUniform4fv(int location, int count, FloatBuffer v){
        uniformf("uniform4fv", location, count, 4, v);
    }

    @Override
    public void glUniform4fv(int location, int count, float[] v, int offset){
        uniformf("uniform4fv", location, count, 4, v, offset);
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w){
        WebGLUniformLocation l = loc(location);
        if(l != null) gl.uniform4i(l, x, y, z, w);
    }

    @Override
    public void glUniform4iv(int location, int count, IntBuffer v){
        uniformi("uniform4iv", location, count, 4, v);
    }

    @Override
    public void glUniform4iv(int location, int count, int[] v, int offset){
        uniformi("uniform4iv", location, count, 4, v, offset);
    }

    void matrix(String fn, int location, int count, int n, boolean transpose, FloatBuffer value){
        WebGLUniformLocation l = loc(location);
        if(l != null) uniformMatrix(gl, fn, l, transpose, bytes(value, count * n * 4, 2));
    }

    void matrix(String fn, int location, int count, int n, boolean transpose, float[] value, int offset){
        WebGLUniformLocation l = loc(location);
        if(l != null) uniformMatrix(gl, fn, l, transpose, sub(value, offset, count * n));
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value){
        matrix("uniformMatrix2fv", location, count, 4, transpose, value);
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset){
        matrix("uniformMatrix2fv", location, count, 4, transpose, value, offset);
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value){
        matrix("uniformMatrix3fv", location, count, 9, transpose, value);
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset){
        matrix("uniformMatrix3fv", location, count, 9, transpose, value, offset);
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value){
        matrix("uniformMatrix4fv", location, count, 16, transpose, value);
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset){
        matrix("uniformMatrix4fv", location, count, 16, transpose, value, offset);
    }

    @Override
    public void glUseProgram(int program){
        gl.useProgram(get(program));
    }

    @Override
    public void glValidateProgram(int program){
        gl.validateProgram(get(program));
    }

    @Override
    public void glVertexAttrib1f(int indx, float x){
        gl.vertexAttrib1f(indx, x);
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values){
        gl.vertexAttrib1f(indx, values.get(values.position()));
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y){
        gl.vertexAttrib2f(indx, x, y);
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values){
        int p = values.position();
        gl.vertexAttrib2f(indx, values.get(p), values.get(p + 1));
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z){
        gl.vertexAttrib3f(indx, x, y, z);
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values){
        int p = values.position();
        gl.vertexAttrib3f(indx, values.get(p), values.get(p + 1), values.get(p + 2));
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w){
        gl.vertexAttrib4f(indx, x, y, z, w);
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values){
        int p = values.position();
        gl.vertexAttrib4f(indx, values.get(p), values.get(p + 1), values.get(p + 2), values.get(p + 3));
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr){
        throw new UnsupportedOperationException("Client-side vertex arrays are not available in WebGL");
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr){
        gl.vertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }
}
