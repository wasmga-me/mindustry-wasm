package mindustry.web.emu;

import arc.func.*;

/** Stands in for mindustry.net.BeControl (desktop self-updates). */
public class BeControl{
    public boolean active(){
        return false;
    }

    public void init(){
    }

    public void checkUpdate(Boolc done){
        done.get(false);
    }

    public boolean isUpdateAvailable(){
        return false;
    }

    public void showUpdateDialog(){
    }
}
