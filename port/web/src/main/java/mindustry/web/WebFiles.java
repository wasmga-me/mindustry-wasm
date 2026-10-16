package mindustry.web;

import arc.*;
import arc.files.*;

import java.io.*;

/** Internal and classpath files live under /assets, local and external ones under /data. */
public class WebFiles implements Files{
    public static final String assetsPath = "/assets/";
    public static final String localPath = WebFileSystem.dataRoot + "/";

    @Override
    public Fi get(String fileName, FileType type){
        return new WebFi(fileName, type);
    }

    @Override
    public String getExternalStoragePath(){
        return localPath;
    }

    @Override
    public boolean isExternalStorageAvailable(){
        return true;
    }

    @Override
    public String getLocalStoragePath(){
        return localPath;
    }

    @Override
    public boolean isLocalStorageAvailable(){
        return true;
    }

    public static class WebFi extends Fi{
        public WebFi(String fileName, FileType type){
            super(fileName, type);
        }

        public WebFi(File file, FileType type){
            super(file, type);
        }

        @Override
        public Fi child(String name){
            if(file.getPath().length() == 0) return new WebFi(new File(name), type);
            return new WebFi(new File(file, name), type);
        }

        @Override
        public Fi sibling(String name){
            return new WebFi(new File(file.getParent(), name), type);
        }

        @Override
        public Fi parent(){
            File parent = file.getParentFile();
            if(parent == null){
                parent = type == FileType.absolute ? new File("/") : new File("");
            }
            return new WebFi(parent, type);
        }

        @Override
        public File file(){
            switch(type){
                case internal:
                case classpath:
                    return new File(assetsPath, file.getPath());
                case local:
                case external:
                    return new File(localPath, file.getPath());
                default:
                    return file;
            }
        }

        @Override
        public InputStream read(){
            try{
                return new FileInputStream(file());
            }catch(Exception e){
                if(file().isDirectory()) throw new arc.util.ArcRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", e);
                throw new arc.util.ArcRuntimeException("Error reading file: " + file + " (" + type + ")", e);
            }
        }

        @Override
        public byte[] readBytes(){
            WebFileSystem.FileNode node = WebLauncher.fs.file(file().getPath());
            if(node == null) throw new arc.util.ArcRuntimeException("File not found: " + file + " (" + type + ")");
            byte[] out = new byte[node.size];
            System.arraycopy(node.data, 0, out, 0, node.size);
            return out;
        }

        @Override
        public long length(){
            return file().length();
        }

        @Override
        public boolean exists(){
            return file().exists();
        }

        @Override
        public boolean isDirectory(){
            return file().isDirectory();
        }
    }
}
