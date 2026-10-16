package mindustry.web;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.net.*;
import mindustry.net.Net.*;

public class WebLauncher extends ClientLauncher{
    static WebFileSystem fs;
    static WebStorage storage;

    public static void main(String[] args){
        //read by arc.util.OS; there is no user account or home folder here
        System.setProperty("user.name", "player");
        System.setProperty("user.home", WebFileSystem.dataRoot);
        System.setProperty("java.home", "/java");
        fs = WebFileSystem.install();
        storage = new WebStorage(fs);
        storage.mount();

        mindustry.world.Block.webBuildTypes = WebBuildTypes::get;
        //touch screens get the game's touch controls and layout
        Vars.testMobile = touchScreen();
        WebMusic.install();
        Log.logger = (level, text) -> WebHost.log("[" + level.name() + "] " + Log.removeColors(text));

        //building the app opens WebGL 2, which can fail before run() reports errors itself
        WebApplication app;
        try{
            app = new WebApplication(new WebLauncher());
        }catch(Throwable t){
            WebHost.crash(t);
            return;
        }
        app.run();
    }

    @Override
    public void setup(){
        Core.settings.setDataDirectory(Core.files.absolute(WebFileSystem.dataRoot));
        super.setup();
    }

    @org.teavm.jso.JSBody(script = "return matchMedia('(pointer: coarse)').matches;")
    static native boolean touchScreen();

    /** Saving gives a file that becomes a download; opening asks the browser for one. */
    @Override
    public void showFileChooser(mindustry.ui.FileChooser.FileChooserParams params){
        if(!params.open){
            arc.files.Fi dir = Core.files.absolute(WebFileSystem.downloadRoot);
            dir.mkdirs();
            params.handleChooseResult(dir.child(params.fileName));
            return;
        }
        WebHost.pickFile("." + String.join(",.", params.extensions), (name, data) -> Core.app.post(() -> {
            arc.files.Fi file = Core.files.absolute("/uploads").child(name);
            file.writeBytes(data.toJavaArray());
            params.handleChooseResult(file);
        }));
    }

    @Override
    public NetProvider getNet(){
        return new OfflineNet();
    }

    /** No sockets in a browser: hosting and joining games are unavailable. */
    static class OfflineNet implements NetProvider{
        @Override
        public void connectClient(String ip, int port, Runnable success){
            Vars.net.showError(new RuntimeException("Multiplayer is not available in the browser."));
        }

        @Override
        public void sendClient(Object object, boolean reliable){
        }

        @Override
        public void disconnectClient(){
        }

        @Override
        public void discoverServers(arc.func.Cons<Host> callback, Runnable done){
            done.run();
        }

        @Override
        public void pingHost(String address, int port, arc.func.Cons<Host> valid, arc.func.Cons<Exception> failed){
            failed.get(new RuntimeException("Multiplayer is not available in the browser."));
        }

        @Override
        public void hostServer(int port){
            throw new RuntimeException("Multiplayer is not available in the browser.");
        }

        @Override
        public Iterable<? extends NetConnection> getConnections(){
            return new arc.struct.Seq<>();
        }

        @Override
        public void closeServer(){
        }
    }
}
