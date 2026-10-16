package mindustry.web;

import org.teavm.model.*;
import org.teavm.model.emit.*;
import org.teavm.model.instructions.*;
import org.teavm.vm.spi.*;

import java.util.*;

/**
 * Compile-time redirects for JDK methods the browser class library lacks: each call is
 * rewritten into a static call to {@link Shims}, with the receiver as the first argument.
 */
public class WebPlugin implements TeaVMPlugin{
    static final Map<MethodReference, MethodReference> redirects = new HashMap<>();

    static void redirect(String cls, String name, ValueType ret, ValueType... args){
        boolean instance = !cls.startsWith("static:");
        cls = cls.replace("static:", "");
        ValueType[] shimArgs = args;
        if(instance){
            shimArgs = new ValueType[args.length + 1];
            shimArgs[0] = ValueType.object(cls);
            System.arraycopy(args, 0, shimArgs, 1, args.length);
        }
        ValueType[] from = Arrays.copyOf(args, args.length + 1);
        from[args.length] = ret;
        ValueType[] to = Arrays.copyOf(shimArgs, shimArgs.length + 1);
        to[shimArgs.length] = ret;
        redirects.put(new MethodReference(cls, name, from), new MethodReference(Shims.class.getName(), name, to));
    }

    static{
        ValueType str = ValueType.object("java.lang.String");
        redirect("java.lang.Class", "isAnonymousClass", ValueType.BOOLEAN);
        redirect("java.lang.Class", "getResource", ValueType.object("java.net.URL"), str);
        redirect("java.lang.Runtime", "maxMemory", ValueType.LONG);
        redirect("arc.files.Fi", "map", ValueType.object("java.nio.ByteBuffer"));
        redirect("static:java.lang.Thread", "sleep", ValueType.VOID, ValueType.LONG, ValueType.INTEGER);
        redirect("static:arc.util.Strings", "getStackTrace", str, ValueType.object("java.lang.Throwable"));
        redirect("java.lang.Throwable", "printStackTrace", ValueType.VOID, ValueType.object("java.io.PrintWriter"));
    }

    @Override
    public void install(TeaVMHost host){
        host.add((ClassHolderTransformer)(cls, context) -> {
            if(cls.getName().equals(Shims.class.getName())) return;
            //TeaVM's InflaterInputStream.skip() reads into the buffer that still holds compressed input,
            //corrupting maps and saves whose chunks get skipped; route it through Shims.inflaterSkip
            if(cls.getName().endsWith("InflaterInputStream") && (cls.getName().startsWith("java.util.zip.") || cls.getName().startsWith("org.teavm.classlib.java.util.zip."))){
                MethodHolder skip = cls.getMethod(new MethodDescriptor("skip", ValueType.LONG, ValueType.LONG));
                if(skip != null){
                    ProgramEmitter pe = ProgramEmitter.create(skip, context.getHierarchy());
                    pe.invoke(Shims.class.getName(), "inflaterSkip", ValueType.LONG,
                        pe.var(0, ValueType.object("java.io.InputStream")), pe.var(1, ValueType.LONG)).returnValue();
                    System.out.println("[WebPlugin] patched " + cls.getName() + ".skip");
                }
            }
            for(MethodHolder method : cls.getMethods()){
                //Arc natives with no browser version (Pixmap's stb_image path, env vars) are never taken
                //here, since ArcNativesLoader.loaded stays false; give them a body that throws
                if(cls.getName().startsWith("arc.") && method.hasModifier(ElementModifier.NATIVE)
                    && !method.getName().contains("$js_body$") && method.getAnnotations().get("org.teavm.jso.JSBody") == null){
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    ProgramEmitter pe = ProgramEmitter.create(method, context.getHierarchy());
                    pe.construct(UnsupportedOperationException.class, pe.constant(method.getReference().toString())).raise();
                    continue;
                }
                Program program = method.getProgram();
                if(program == null) continue;
                for(BasicBlock block : program.getBasicBlocks()){
                    for(Instruction insn : block){
                        if(!(insn instanceof InvokeInstruction invoke)) continue;
                        MethodReference target = redirects.get(invoke.getMethod());
                        if(target == null) continue;
                        if(invoke.getInstance() != null){
                            List<Variable> args = new ArrayList<>();
                            args.add(invoke.getInstance());
                            args.addAll(invoke.getArguments());
                            invoke.setInstance(null);
                            invoke.setArguments(args.toArray(new Variable[0]));
                        }
                        invoke.setType(InvocationType.SPECIAL);
                        invoke.setMethod(target);
                    }
                }
            }
        });
    }
}
