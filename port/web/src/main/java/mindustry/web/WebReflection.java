package mindustry.web;

import org.teavm.extension.introspect.*;
import org.teavm.extension.spi.reflection.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Arc's Json reads and writes fields by reflection (rules, objectives, settings, planet data) and creates
 * objects with no-arg constructors. Opening every class makes the build run out of memory, so only the
 * classes Json can reach from what Mindustry serializes get their fields and constructors.
 */
public class WebReflection extends SimpleReflectionPolicy{
    //whole packages holding serialized types, polymorphic ones included (objectives, markers)
    static final List<String> packages = List.of("mindustry.game.", "mindustry.maps.", "mindustry.io.", "mindustry.mod.", "mindustry.net.");
    //classes passed to Json in Mindustry's code and settings
    static final List<String> roots = List.of("mindustry.game.Rules", "mindustry.type.ItemStack", "mindustry.type.LiquidStack",
        "mindustry.type.PayloadStack", "mindustry.type.ItemSeq", "mindustry.type.Planet$PlanetData", "mindustry.game.SectorInfo",
        "mindustry.game.CampaignRules", "mindustry.game.GameStats", "mindustry.world.consumers.ConsumeLiquid",
        "mindustry.mod.Mods$ModMeta", "mindustry.type.MapLocales", "mindustry.audio.MusicContainer",
        "mindustry.ui.dialogs.MapListDialog$MapViewSettings", "mindustry.ui.dialogs.JoinDialog$Server",
        "arc.graphics.Color", "arc.math.geom.Vec2", "arc.math.geom.Point2", "arc.math.geom.Rect",
        "arc.struct.Seq", "arc.struct.IntSeq", "arc.struct.IntMap", "arc.struct.ObjectMap", "arc.struct.ObjectIntMap",
        "arc.struct.ObjectFloatMap", "arc.struct.ObjectSet", "arc.struct.IntSet", "arc.struct.StringMap", "arc.struct.OrderedMap",
        "arc.struct.OrderedSet", "arc.struct.ArrayMap", "arc.struct.Queue");

    final Set<String> open = new HashSet<>();

    @Override
    protected void setup(){
        IntrospectClass<?> content = environment().findClass("mindustry.ctype.Content");
        ArrayDeque<IntrospectClass<?>> queue = new ArrayDeque<>();
        for(String root : roots) queue.add(environment().findClass(root));
        while(!queue.isEmpty()){
            IntrospectClass<?> cls = queue.poll();
            if(cls == null || cls.isPrimitive()) continue;
            if(cls.isArray()){
                queue.add(cls.componentType());
                continue;
            }
            String name = cls.name();
            //content is written by name through custom serializers
            if(!(name.startsWith("mindustry.") || name.startsWith("arc.")) || content.isAssignableFrom(cls) || !open.add(name)) continue;
            queue.add(cls.superclass());
            for(IntrospectField field : cls.declaredFields()){
                if(!serializable(field)) continue;
                queue.add(field.type());
                addTypes(field.genericType(), queue);
            }
        }

        System.out.println("[WebReflection] " + open.size() + " classes from roots: " + new TreeSet<>(open));
        selectClasses(c -> open.contains(c.name()) || packages.stream().anyMatch(p -> c.name().startsWith(p)))
            .reflectableFields(WebReflection::serializable)
            .reflectableMethods(m -> m.isConstructor() && m.parameters().isEmpty());
        //listed with getFields(): UI styles, logic layers, effects
        selectClasses(c -> statics.contains(c.name())).reflectableFields(f -> Modifier.isStatic(f.modifiers()) && Modifier.isPublic(f.modifiers()));
    }

    static final List<String> statics = List.of("mindustry.ui.Styles", "mindustry.graphics.Layer", "mindustry.content.Fx");

    static boolean serializable(IntrospectField f){
        return !Modifier.isStatic(f.modifiers()) && !Modifier.isTransient(f.modifiers());
    }

    static void addTypes(IntrospectType type, Deque<IntrospectClass<?>> queue){
        if(type instanceof IntrospectClass<?> c){
            queue.add(c);
        }else if(type instanceof IntrospectParameterizedType p){
            queue.add(p.rawType());
            for(IntrospectTypeArgument arg : p.typeArguments()){
                if(arg.type() != null) addTypes(arg.type(), queue);
            }
        }else if(type instanceof IntrospectGenericArray a){
            addTypes(a.componentType(), queue);
        }
    }
}
