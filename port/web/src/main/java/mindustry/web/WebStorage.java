package mindustry.web;

import org.teavm.jso.*;
import org.teavm.jso.core.*;
import org.teavm.jso.typedarrays.*;

/** Mounts the asset bundle and the player's saved files, and writes changed files back to IndexedDB. */
public class WebStorage{
    final WebFileSystem fs;
    double lastFlush;

    WebStorage(WebFileSystem fs){
        this.fs = fs;
    }

    interface Entry extends JSObject{
        @JSProperty
        String getPath();
        @JSProperty
        Int8Array getData();
        @JSProperty
        double getModified();
    }

    @JSBody(script = "var a = window.mindustryHost.assets; window.mindustryHost.assets = null; return a;")
    static native JSArray<Entry> takeAssets();

    @JSBody(script = "var a = window.mindustryHost.saved; window.mindustryHost.saved = null; return a || [];")
    static native JSArray<Entry> takeSaved();

    @JSBody(params = {"path", "data", "length", "modified"}, script = "window.mindustryHost.store(path, data.subarray(0, length), modified);")
    static native void store(String path, @JSByRef byte[] data, int length, double modified);

    @JSBody(params = {"name", "data", "length"}, script = "window.mindustryHost.download(name, data.subarray(0, length));")
    static native void download(String name, @JSByRef byte[] data, int length);

    @JSBody(params = "path", script = "window.mindustryHost.remove(path);")
    static native void remove(String path);

    void mount(){
        JSArray<Entry> assets = takeAssets();
        for(int i = 0; i < assets.getLength(); i++){
            Entry e = assets.get(i);
            byte[] data = e.getData().toJavaArray();
            fs.put(WebFiles.assetsPath + e.getPath(), data, data.length, 0, false);
            fs.file(WebFiles.assetsPath + e.getPath()).readOnly = true;
        }
        JSArray<Entry> saved = takeSaved();
        for(int i = 0; i < saved.getLength(); i++){
            Entry e = saved.get(i);
            byte[] data = e.getData().toJavaArray();
            fs.put(e.getPath(), data, data.length, (long)e.getModified(), false);
        }
        fs.mkdirs(WebFileSystem.dataRoot);
    }

    void tick(){
        double now = WebApplication.now();
        if(now - lastFlush > 2000){
            flush();
        }
    }

    void flush(){
        lastFlush = WebApplication.now();
        if(fs.dirty.isEmpty()) return;
        for(String path : fs.takeDirty()){
            WebFileSystem.FileNode node = fs.file(path);
            if(path.startsWith(WebFileSystem.downloadRoot + "/")){
                //written in full by now: exports run within one frame
                if(node != null){
                    download(path.substring(path.lastIndexOf('/') + 1), node.data, node.size);
                    arc.Core.files.absolute(path).delete();
                }
            }else if(node != null){
                store(path, node.data, node.size, node.modified);
            }else if(fs.find(path) == null){
                remove(path);
            }
        }
    }
}
