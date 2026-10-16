package mindustry.web.emu;

import arc.*;
import arc.files.*;

/** Helpers for the natives that read files: the wasm side has no file system, so they get the bytes. */
public class WebNatives{
    public static byte[] read(String path){
        Fi file = path.startsWith("/") ? Core.files.absolute(path) : Core.files.internal(path);
        return file.readBytes();
    }
}
