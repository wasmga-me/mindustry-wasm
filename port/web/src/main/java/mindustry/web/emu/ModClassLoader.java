package mindustry.web.emu;

/** Stands in for mindustry.mod.ModClassLoader; Java mods can't load in the browser. */
public class ModClassLoader extends ClassLoader{
    public ModClassLoader(ClassLoader parent){
        super(parent);
    }

    public void addChild(ClassLoader child){
    }
}
