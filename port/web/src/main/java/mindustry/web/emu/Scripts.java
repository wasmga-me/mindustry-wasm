package mindustry.web.emu;

import arc.files.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.mod.Mods.*;

/** Stands in for mindustry.mod.Scripts: there is no JavaScript engine for mods in this build. */
public class Scripts implements Disposable{
    public boolean hasErrored(){
        return false;
    }

    public String runConsole(String text){
        return "Scripts are not available in the browser.";
    }

    public void log(String source, String message){
        log(LogLevel.info, source, message);
    }

    public void log(LogLevel level, String source, String message){
        Log.log(level, "[@]: @", source, message);
    }

    public float[] newFloats(int capacity){
        return new float[capacity];
    }

    public Class<?> getClass(Object object){
        return object.getClass();
    }

    public void run(LoadedMod mod, Fi file){
        Log.warn("Skipping script @ from @: scripts are not available in the browser.", file.name(), mod.name);
    }

    @Override
    public void dispose(){
    }
}
