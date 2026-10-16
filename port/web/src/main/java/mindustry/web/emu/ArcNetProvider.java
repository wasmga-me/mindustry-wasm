package mindustry.web.emu;

import arc.func.*;
import mindustry.*;
import mindustry.net.*;
import mindustry.net.Net.*;

/** Stands in for mindustry.net.ArcNetProvider: browsers have no sockets, so there is no multiplayer. */
public class ArcNetProvider implements NetProvider{
    static RuntimeException unavailable(){
        return new RuntimeException("Multiplayer is not available in the browser.");
    }

    @Override
    public void connectClient(String ip, int port, Runnable success){
        Vars.net.showError(unavailable());
    }

    @Override
    public void sendClient(Object object, boolean reliable){
    }

    @Override
    public void disconnectClient(){
    }

    @Override
    public void discoverServers(Cons<Host> callback, Runnable done){
        done.run();
    }

    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed){
        failed.get(unavailable());
    }

    @Override
    public void hostServer(int port){
        throw unavailable();
    }

    @Override
    public Iterable<? extends NetConnection> getConnections(){
        return new arc.struct.Seq<>();
    }

    @Override
    public void closeServer(){
    }
}
