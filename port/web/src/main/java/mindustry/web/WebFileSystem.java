package mindustry.web;

import org.teavm.runtime.fs.*;

import java.io.*;
import java.util.*;

/**
 * In-memory file system behind java.io.File. The game's assets are mounted read-only under /assets
 * straight from the downloaded bundle (no copies); everything under /data is the player's and is
 * mirrored to IndexedDB by {@link WebStorage}.
 */
public class WebFileSystem implements VirtualFileSystem{
    public static final String dataRoot = "/data";
    /** Files the game exports land here and are handed to the browser as downloads. */
    public static final String downloadRoot = "/downloads";

    final Dir root = new Dir("", null);
    /** Paths under /data written or deleted since the last flush. */
    final Set<String> dirty = new LinkedHashSet<>();

    public static WebFileSystem install(){
        WebFileSystem fs = new WebFileSystem();
        VirtualFileSystemProvider.setInstance(fs);
        return fs;
    }

    @Override
    public String getUserDir(){
        //code that builds plain internal Fi objects (new Fi("version.properties", internal)) reads relative to here
        return "/assets";
    }

    @Override
    public VirtualFile getFile(String path){
        return new FileRef(normalize(path));
    }

    @Override
    public boolean isWindows(){
        return false;
    }

    @Override
    public String canonicalize(String path){
        return normalize(path);
    }

    @Override
    public String[] getRoots(){
        return new String[]{"/"};
    }

    static String normalize(String path){
        if(path.isEmpty()) return "/";
        String[] parts = path.split("/");
        ArrayList<String> out = new ArrayList<>();
        for(String part : parts){
            if(part.isEmpty() || part.equals(".")) continue;
            if(part.equals("..")){
                if(!out.isEmpty()) out.remove(out.size() - 1);
            }else{
                out.add(part);
            }
        }
        StringBuilder b = new StringBuilder();
        for(String p : out) b.append('/').append(p);
        return b.length() == 0 ? "/" : b.toString();
    }

    Node find(String path){
        if(path.equals("/")) return root;
        Node node = root;
        int start = 1;
        while(node instanceof Dir){
            int end = path.indexOf('/', start);
            String name = end < 0 ? path.substring(start) : path.substring(start, end);
            node = ((Dir)node).children.get(name);
            if(node == null || end < 0) return node;
            start = end + 1;
        }
        return null;
    }

    Dir mkdirs(String path){
        Node node = root;
        if(path.equals("/")) return root;
        for(String part : path.substring(1).split("/")){
            Dir dir = (Dir)node;
            Node next = dir.children.get(part);
            if(next == null){
                next = new Dir(part, dir);
                dir.children.put(part, next);
            }
            node = next;
        }
        return (Dir)node;
    }

    /** Mounts a file without copying it (used for the asset bundle and restored saves). */
    public void put(String path, byte[] data, int length, long modified, boolean track){
        path = normalize(path);
        int slash = path.lastIndexOf('/');
        Dir dir = mkdirs(slash <= 0 ? "/" : path.substring(0, slash));
        String name = path.substring(slash + 1);
        FileNode file = new FileNode(name, dir);
        file.data = data;
        file.size = length;
        file.modified = modified;
        dir.children.put(name, file);
        if(track) markDirty(path);
    }

    void markDirty(String path){
        if(path.startsWith(dataRoot + "/") || path.startsWith(downloadRoot + "/")) dirty.add(path);
    }

    public String[] takeDirty(){
        String[] out = dirty.toArray(new String[0]);
        dirty.clear();
        return out;
    }

    /** @return file contents, or null if the path is not a file. The array may be longer than the file. */
    public FileNode file(String path){
        Node node = find(normalize(path));
        return node instanceof FileNode f ? f : null;
    }

    static abstract class Node{
        String name;
        Dir parent;
        long modified = System.currentTimeMillis();
        boolean readOnly;

        Node(String name, Dir parent){
            this.name = name;
            this.parent = parent;
        }

        String path(){
            if(parent == null) return "/";
            String p = parent.path();
            return (p.equals("/") ? "" : p) + "/" + name;
        }
    }

    static class Dir extends Node{
        final LinkedHashMap<String, Node> children = new LinkedHashMap<>();

        Dir(String name, Dir parent){
            super(name, parent);
        }
    }

    public static class FileNode extends Node{
        public byte[] data = new byte[0];
        public int size;

        FileNode(String name, Dir parent){
            super(name, parent);
        }

        void ensure(int capacity){
            if(data.length < capacity){
                data = Arrays.copyOf(data, Math.max(capacity, data.length * 2));
            }
        }
    }

    class FileRef implements VirtualFile{
        final String path;

        FileRef(String path){
            this.path = path;
        }

        @Override
        public String getName(){
            return path.substring(path.lastIndexOf('/') + 1);
        }

        @Override
        public boolean isDirectory(){
            return find(path) instanceof Dir;
        }

        @Override
        public boolean isFile(){
            return find(path) instanceof FileNode;
        }

        @Override
        public String[] listFiles(){
            Node node = find(path);
            if(!(node instanceof Dir dir)) return null;
            return dir.children.keySet().toArray(new String[0]);
        }

        @Override
        public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append){
            Node node = find(path);
            if(!(node instanceof FileNode file)) return null;
            if(writable && file.readOnly) return null;
            return new Accessor(file, path, append);
        }

        @Override
        public boolean createFile(String fileName) throws IOException{
            Node node = find(path);
            if(!(node instanceof Dir dir)) throw new IOException("Not a directory: " + path);
            if(dir.children.containsKey(fileName)) return false;
            dir.children.put(fileName, new FileNode(fileName, dir));
            markDirty((path.equals("/") ? "" : path) + "/" + fileName);
            return true;
        }

        @Override
        public boolean createDirectory(String fileName){
            Node node = find(path);
            if(!(node instanceof Dir dir) || dir.children.containsKey(fileName)) return false;
            dir.children.put(fileName, new Dir(fileName, dir));
            return true;
        }

        @Override
        public boolean delete(){
            Node node = find(path);
            if(node == null || node.parent == null || node.readOnly) return false;
            if(node instanceof Dir dir && !dir.children.isEmpty()) return false;
            node.parent.children.remove(node.name);
            markDirty(path);
            return true;
        }

        @Override
        public boolean adopt(VirtualFile file, String fileName){
            Node node = find(path);
            Node other = find(((FileRef)file).path);
            if(!(node instanceof Dir dir) || other == null || other.parent == null) return false;
            String oldPath = other.path();
            other.parent.children.remove(other.name);
            other.name = fileName;
            other.parent = dir;
            dir.children.put(fileName, other);
            markTree(oldPath, other);
            markTree(other.path(), other);
            return true;
        }

        void markTree(String at, Node node){
            markDirty(at);
            if(node instanceof Dir dir){
                for(Node child : dir.children.values()){
                    markTree(at + "/" + child.name, child);
                }
            }
        }

        @Override
        public boolean canRead(){
            return find(path) != null;
        }

        @Override
        public boolean canWrite(){
            Node node = find(path);
            return node != null && !node.readOnly;
        }

        @Override
        public long lastModified(){
            Node node = find(path);
            return node == null ? 0 : node.modified;
        }

        @Override
        public boolean setLastModified(long lastModified){
            Node node = find(path);
            if(node == null) return false;
            node.modified = lastModified;
            return true;
        }

        @Override
        public boolean setReadOnly(boolean readOnly){
            Node node = find(path);
            if(node == null) return false;
            node.readOnly = readOnly;
            return true;
        }

        @Override
        public int length(){
            Node node = find(path);
            return node instanceof FileNode f ? f.size : 0;
        }
    }

    class Accessor implements VirtualFileAccessor{
        final FileNode file;
        final String path;
        int pos;

        Accessor(FileNode file, String path, boolean append){
            this.file = file;
            this.path = path;
            if(append) pos = file.size;
        }

        @Override
        public int read(byte[] buffer, int offset, int limit){
            int n = Math.min(limit, file.size - pos);
            if(n <= 0) return -1;
            System.arraycopy(file.data, pos, buffer, offset, n);
            pos += n;
            return n;
        }

        @Override
        public void write(byte[] buffer, int offset, int limit){
            file.ensure(pos + limit);
            System.arraycopy(buffer, offset, file.data, pos, limit);
            pos += limit;
            if(pos > file.size) file.size = pos;
            file.modified = System.currentTimeMillis();
            markDirty(path);
        }

        @Override
        public int tell(){
            return pos;
        }

        @Override
        public void seek(int target){
            pos = target;
        }

        @Override
        public void skip(int amount){
            pos += amount;
        }

        @Override
        public int size(){
            return file.size;
        }

        @Override
        public void resize(int size){
            file.ensure(size);
            if(size > file.size) Arrays.fill(file.data, file.size, size, (byte)0);
            file.size = size;
            if(pos > size) pos = size;
            markDirty(path);
        }

        @Override
        public void close(){
        }

        @Override
        public void flush(){
        }
    }
}
