package mindustry.web.emu;

import java.lang.reflect.*;

/** Stands in for sun.misc.Unsafe, which Arc probes for and falls back from when it fails. */
public final class Unsafe{
    private Unsafe(){
    }

    public long objectFieldOffset(Field field){
        throw new UnsupportedOperationException();
    }

    public long getLong(Object o, long offset){
        throw new UnsupportedOperationException();
    }

    public void copyMemory(long src, long dst, long bytes){
        throw new UnsupportedOperationException();
    }
}
