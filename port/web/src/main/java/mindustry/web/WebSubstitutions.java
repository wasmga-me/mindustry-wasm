package mindustry.web;

import org.teavm.extension.spi.substitution.*;

/** Swaps in browser stand-ins for JDK internals that Arc and Mindustry reference. */
public class WebSubstitutions extends SimpleSubstitutionPolicy{
    @Override
    public void contribute(SubstitutionSink sink){
        sink.selectClasses(named("sun.misc.Unsafe")).replacePackage("sun.misc", "mindustry.web.emu");
        sink.selectClasses(named("mindustry.net.ArcNetProvider").or(named("mindustry.net.BeControl")))
            .replacePackage("mindustry.net", "mindustry.web.emu");
        sink.selectClasses(named("mindustry.mod.ModClassLoader").or(named("mindustry.mod.Scripts")))
            .replacePackage("mindustry.mod", "mindustry.web.emu");
        sink.selectClasses(named("mindustry.core.Platform$1")).replacePackage("mindustry.core", "mindustry.web.emu");
        //natives: Buffers on typed arrays, FreeType and SoLoud through arc-natives.wasm
        sink.selectClasses(named("arc.util.Buffers")).replacePackage("arc.util", "mindustry.web.emu");
        sink.selectClasses(named("arc.audio.Soloud")).replacePackage("arc.audio", "mindustry.web.emu");
        sink.selectClasses(namePattern("arc.freetype.FreeType").or(namePattern("arc.freetype.FreeType$*")))
            .replacePackage("arc.freetype", "mindustry.web.emu");
    }
}
