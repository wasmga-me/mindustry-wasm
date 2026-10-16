package mindustry.web;

import java.net.*;

/** Replacements for JDK methods missing from the browser class library (see {@link WebPlugin}). */
public class Shims{
    public static boolean isAnonymousClass(Class<?> type){
        String name = type.getName();
        int i = name.lastIndexOf('$');
        if(i < 0 || i == name.length() - 1) return false;
        for(int j = i + 1; j < name.length(); j++){
            if(!Character.isDigit(name.charAt(j))) return false;
        }
        return true;
    }

    public static URL getResource(Class<?> type, String name){
        return null;
    }

    /** Memory mapping doesn't exist here; callers fall back to reading the file. */
    public static java.nio.ByteBuffer map(arc.files.Fi file){
        throw new arc.util.ArcRuntimeException("Cannot map files in the browser: " + file);
    }

    public static long maxMemory(Runtime runtime){
        return 2048L * 1024 * 1024;
    }

    public static void sleep(long ms, int nanos) throws InterruptedException{
        Thread.sleep(ms + (nanos >= 500_000 ? 1 : 0));
    }

    /** Logged errors carry the JS stack of each cause, since the Java one is empty here. */
    public static void printStackTrace(Throwable e, java.io.PrintWriter out){
        out.print(getStackTrace(e));
        out.flush();
    }

    public static String getStackTrace(Throwable e){
        java.io.StringWriter out = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(out));
        for(Throwable c = e; c != null; c = c.getCause()){
            String stack = WebHost.jsStack(c);
            if(!stack.isEmpty()) out.append("\n--- ").append(String.valueOf(c)).append("\n").append(stack);
        }
        return out.toString();
    }

    public static long inflaterSkip(java.io.InputStream in, long n) throws java.io.IOException{
        if(n < 0) throw new IllegalArgumentException();
        byte[] buffer = new byte[(int)Math.min(n, 4096)];
        long count = 0;
        while(count < n){
            int x = in.read(buffer, 0, (int)Math.min(n - count, buffer.length));
            if(x == -1) break;
            count += x;
        }
        return count;
    }
}
