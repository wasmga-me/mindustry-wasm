package mindustry.web.emu;

import arc.struct.*;
import arc.util.*;
import org.teavm.jso.*;

import java.nio.*;

/** Arc's Buffers with the JNI copies done on typed arrays. Direct buffers are plain JS memory here. */
public final class Buffers{
    static final Seq<ByteBuffer> unsafeBuffers = new Seq<>();
    static int allocatedUnsafe = 0;

    public static void copy(float[] src, Buffer dst, int numFloats, int offset){
        if(dst instanceof ByteBuffer)
            dst.limit(numFloats << 2);
        else if(dst instanceof FloatBuffer) dst.limit(numFloats);

        copyJni(src, dst, numFloats, offset);
        dst.position(0);
    }

    public static void copy(byte[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements);
    }

    public static void copy(short[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 1));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1);
    }

    public static void copy(float[] src, int srcOffset, int numElements, Buffer dst){
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    public static void copy(int[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    public static void copy(float[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    public static void copy(Buffer src, Buffer dst, int numElements){
        int numBytes = elementsToBytes(src, numElements);
        dst.limit(dst.position() + bytesToElements(dst, numBytes));
        copyJni(src, positionInBytes(src), dst, positionInBytes(dst), numBytes);
    }

    private static int positionInBytes(Buffer dst){
        return dst.position() << elementShift(dst);
    }

    private static int bytesToElements(Buffer dst, int bytes){
        return bytes >>> elementShift(dst);
    }

    private static int elementsToBytes(Buffer dst, int elements){
        return elements << elementShift(dst);
    }

    private static int elementShift(Buffer dst){
        if(dst instanceof ByteBuffer)
            return 0;
        else if(dst instanceof ShortBuffer || dst instanceof CharBuffer)
            return 1;
        else if(dst instanceof IntBuffer)
            return 2;
        else if(dst instanceof LongBuffer)
            return 3;
        else if(dst instanceof FloatBuffer)
            return 2;
        else if(dst instanceof DoubleBuffer)
            return 3;
        else
            throw new ArcRuntimeException("Can't copy to a " + dst.getClass().getName() + " instance");
    }

    public static FloatBuffer newFloatBuffer(int numFloats){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numFloats * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asFloatBuffer();
    }

    public static ShortBuffer newShortBuffer(int numShorts){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numShorts * 2);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asShortBuffer();
    }

    public static ByteBuffer newByteBuffer(int numBytes){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    public static IntBuffer newIntBuffer(int numInts){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numInts * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asIntBuffer();
    }

    public static void disposeUnsafeByteBuffer(ByteBuffer buffer){
        int size = buffer.capacity();
        if(!unsafeBuffers.remove(buffer, true))
            throw new IllegalArgumentException("buffer not allocated with newUnsafeByteBuffer or already disposed");
        allocatedUnsafe -= size;
    }

    public static boolean isUnsafeByteBuffer(ByteBuffer buffer){
        return unsafeBuffers.contains(buffer, true);
    }

    public static ByteBuffer newUnsafeByteBuffer(int numBytes){
        ByteBuffer buffer = newByteBuffer(numBytes);
        allocatedUnsafe += numBytes;
        unsafeBuffers.add(buffer);
        return buffer;
    }

    public static long getUnsafeBufferAddress(Buffer buffer){
        return 0;
    }

    public static ByteBuffer newUnsafeByteBuffer(ByteBuffer buffer){
        allocatedUnsafe += buffer.capacity();
        unsafeBuffers.add(buffer);
        return buffer;
    }

    public static int getAllocatedBytesUnsafe(){
        return allocatedUnsafe;
    }

    private static void copyJni(float[] src, Buffer dst, int numFloats, int offset){
        copyArray(src, offset * 4, dst, 0, numFloats << 2);
    }

    private static void copyJni(byte[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        copyArray(src, srcOffset, dst, dstOffset, numBytes);
    }

    private static void copyJni(short[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        copyArray(src, srcOffset * 2, dst, dstOffset, numBytes);
    }

    private static void copyJni(int[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        copyArray(src, srcOffset * 4, dst, dstOffset, numBytes);
    }

    private static void copyJni(float[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        copyArray(src, srcOffset * 4, dst, dstOffset, numBytes);
    }

    public static void copyJni(Buffer src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        copyBuffer(src, srcOffset, dst, dstOffset, numBytes);
    }

    @JSBody(params = {"src", "srcOffset", "dst", "dstOffset", "n"}, script =
        "new Uint8Array(dst.buffer, dst.byteOffset + dstOffset, n).set(new Uint8Array(src.buffer, src.byteOffset + srcOffset, n));")
    private static native void copyArray(@JSByRef byte[] src, int srcOffset, @JSBuffer(JSBufferType.UINT8) Buffer dst, int dstOffset, int n);

    @JSBody(params = {"src", "srcOffset", "dst", "dstOffset", "n"}, script =
        "new Uint8Array(dst.buffer, dst.byteOffset + dstOffset, n).set(new Uint8Array(src.buffer, src.byteOffset + srcOffset, n));")
    private static native void copyArray(@JSByRef short[] src, int srcOffset, @JSBuffer(JSBufferType.UINT8) Buffer dst, int dstOffset, int n);

    @JSBody(params = {"src", "srcOffset", "dst", "dstOffset", "n"}, script =
        "new Uint8Array(dst.buffer, dst.byteOffset + dstOffset, n).set(new Uint8Array(src.buffer, src.byteOffset + srcOffset, n));")
    private static native void copyArray(@JSByRef int[] src, int srcOffset, @JSBuffer(JSBufferType.UINT8) Buffer dst, int dstOffset, int n);

    @JSBody(params = {"src", "srcOffset", "dst", "dstOffset", "n"}, script =
        "new Uint8Array(dst.buffer, dst.byteOffset + dstOffset, n).set(new Uint8Array(src.buffer, src.byteOffset + srcOffset, n));")
    private static native void copyArray(@JSByRef float[] src, int srcOffset, @JSBuffer(JSBufferType.UINT8) Buffer dst, int dstOffset, int n);

    @JSBody(params = {"src", "srcOffset", "dst", "dstOffset", "n"}, script =
        "new Uint8Array(dst.buffer, dst.byteOffset + dstOffset, n).set(new Uint8Array(src.buffer, src.byteOffset + srcOffset, n));")
    private static native void copyBuffer(@JSBuffer(JSBufferType.UINT8) Buffer src, int srcOffset, @JSBuffer(JSBufferType.UINT8) Buffer dst, int dstOffset, int n);
}
