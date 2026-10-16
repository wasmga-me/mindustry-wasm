package mindustry.web.emu;

import java.net.*;

/** Stands in for the jar class loader in mindustry.core.Platform#loadJar. */
public class Platform$1 extends ClassLoader{
    public Platform$1(mindustry.core.Platform platform, URL[] urls, ClassLoader loader, ClassLoader parent){
        throw new UnsupportedOperationException("Java mods are not supported in the browser.");
    }
}
