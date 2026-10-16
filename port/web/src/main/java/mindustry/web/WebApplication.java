package mindustry.web;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import org.teavm.interop.*;
import org.teavm.jso.*;
import org.teavm.jso.browser.*;

/** Runs the Arc application loop on requestAnimationFrame. */
public class WebApplication implements Application{
    private final Seq<ApplicationListener> listeners = new Seq<>();
    private final TaskQueue runnables = new TaskQueue();
    private final Thread mainThread;

    final WebGraphics graphics;
    final WebInput input;
    boolean running = true, paused;

    public WebApplication(ApplicationListener listener){
        listeners.add(listener);
        mainThread = Thread.currentThread();

        Core.app = this;
        Core.files = new WebFiles();
        Core.graphics = graphics = new WebGraphics(WebHost.canvas());
        Core.input = input = new WebInput(WebHost.canvas(), graphics);
        Core.settings = new Settings();
        Core.audio = new Audio(true);

        WebHost.onVisibility(this::visibility);
    }

    void visibility(boolean visible){
        if(visible == !paused) return;
        paused = !visible;
        if(paused){
            listen(ApplicationListener::pause);
            WebLauncher.storage.flush();
        }else{
            listen(ApplicationListener::resume);
        }
    }

    public void run(){
        boolean first = true;
        try{
            graphics.update();
            graphics.updateSize();
            listen(ApplicationListener::init);
        }catch(Throwable t){
            running = false;
            WebHost.crash(t);
            throw t;
        }

        while(running){
            waitFrame();
            try{
                frame();
                if(first){
                    first = false;
                    WebHost.ready();
                }
            }catch(Throwable t){
                running = false;
                WebHost.crash(t);
                throw t;
            }
        }
        listen(l -> {
            l.pause();
            l.dispose();
        });
    }

    void frame(){
        if(graphics.updateSize()){
            listen(l -> l.resize(graphics.getWidth(), graphics.getHeight()));
        }
        graphics.update();
        input.update();
        defaultUpdate();
        listen(ApplicationListener::update);
        runnables.run();
        input.postUpdate();
        WebLauncher.storage.tick();
        WebMusic.update();
    }

    @Async
    static native void waitFrame();

    static void waitFrame(AsyncCallback<Void> callback){
        Window.requestAnimationFrame(time -> callback.complete(null));
    }

    private void listen(Cons<ApplicationListener> cons){
        for(ApplicationListener l : listeners){
            cons.get(l);
        }
    }

    @Override
    public Seq<ApplicationListener> getListeners(){
        return listeners;
    }

    @Override
    public ApplicationType getType(){
        return ApplicationType.web;
    }

    @Override
    public Thread getMainThread(){
        return mainThread;
    }

    @Override
    public boolean isOnMainThread(){
        return true;
    }

    @Override
    public String getClipboardText(){
        return WebHost.clipboard;
    }

    @Override
    public void setClipboardText(String text){
        WebHost.clipboard = text;
        WebHost.writeClipboard(text);
    }

    @Override
    public boolean openURI(String uri){
        if(uri == null || uri.isEmpty()) return false;
        WebHost.openURI(uri);
        return true;
    }

    @Override
    public void post(Runnable runnable){
        runnables.post(runnable);
    }

    @Override
    public void exit(){
        //after importing data the game asks to start over: the page restarts it once the files are saved
        WebLauncher.storage.flush();
        WebHost.exit();
    }

    @JSBody(script = "return performance.now();")
    static native double now();
}
