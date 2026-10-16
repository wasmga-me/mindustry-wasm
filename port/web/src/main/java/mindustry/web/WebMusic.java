package mindustry.web;

import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import org.teavm.jso.*;
import org.teavm.jso.core.*;
import org.teavm.jso.typedarrays.*;

/** Music comes in after the game starts (the page downloads it); a track plays once its bytes are in. */
public class WebMusic{
    static final IntMap<Music> pending = new IntMap<>();
    static int lastId;

    interface Loaded extends JSObject{
        @JSProperty
        int getId();
        @JSProperty
        Int8Array getData();
    }

    @JSBody(params = {"id", "path"}, script = "window.mindustryHost.fetchLazy(id, path);")
    static native void fetch(int id, String path);

    @JSBody(script = "return window.mindustryHost.lazyLoaded.splice(0);")
    static native JSArray<Loaded> poll();

    static void install(){
        Music.webLoader = (music, file) -> {
            int id = ++lastId;
            pending.put(id, music);
            //internal files are named relative to the asset folder
            fetch(id, file.path());
        };
    }

    /** Called once per frame. */
    static void update(){
        if(pending.isEmpty()) return;
        JSArray<Loaded> loaded = poll();
        for(int i = 0; i < loaded.getLength(); i++){
            Loaded l = loaded.get(i);
            Music music = pending.remove(l.getId());
            if(music == null) continue;
            if(l.getData() == null){
                Log.err("Could not load music: " + music.file);
                continue;
            }
            try{
                music.load(l.getData().toJavaArray());
            }catch(Exception e){
                Log.err("Could not load music: " + music.file, e);
            }
        }
    }
}
