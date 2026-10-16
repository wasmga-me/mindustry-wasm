package mindustry.web;

import arc.*;
import arc.input.*;
import arc.scene.ui.*;
import org.teavm.jso.*;
import org.teavm.jso.core.*;
import org.teavm.jso.dom.html.*;

import java.util.*;

/**
 * Keyboard, mouse, wheel and touch. The page's host script records DOM events into a queue
 * (see mindustry-host.js); they are drained here once per frame.
 */
public class WebInput extends Input{
    static final int maxPointers = 10;
    static final HashMap<String, KeyCode> codes = new HashMap<>();

    final InputEventQueue queue = new InputEventQueue();
    final WebGraphics graphics;
    final int[] x = new int[maxPointers], y = new int[maxPointers], dx = new int[maxPointers], dy = new int[maxPointers];
    final boolean[] touched = new boolean[maxPointers];
    final int[] touchIds = new int[maxPointers];
    int mouseButtons;
    boolean wantsText;

    WebInput(HTMLCanvasElement canvas, WebGraphics graphics){
        this.graphics = graphics;
        Arrays.fill(touchIds, -1);
    }

    interface Event extends JSObject{
        @JSProperty
        String getT();
        @JSProperty
        String getCode();
        @JSProperty
        String getKey();
        @JSProperty
        double getX();
        @JSProperty
        double getY();
        @JSProperty
        int getB();
        @JSProperty
        int getId();
        @JSProperty
        boolean isRepeat();
    }

    @JSBody(script = "return window.mindustryHost.events.splice(0);")
    static native JSArray<Event> poll();

    @JSBody(params = "on", script = "window.mindustryHost.textInput(on);")
    static native void textInput(boolean on);

    int sx(double v){
        return (int)Math.round(v * graphics.density);
    }

    int sy(double v){
        return graphics.height - 1 - (int)Math.round(v * graphics.density);
    }

    int pointerFor(int id, boolean create){
        for(int i = 0; i < maxPointers; i++) if(touchIds[i] == id) return i;
        if(!create) return -1;
        for(int i = 0; i < maxPointers; i++){
            if(touchIds[i] == -1){
                touchIds[i] = id;
                return i;
            }
        }
        return -1;
    }

    void update(){
        JSArray<Event> events = poll();
        for(int i = 0; i < events.getLength(); i++){
            handle(events.get(i));
        }
        queue.setProcessor(inputMultiplexer);
        queue.drain();

        boolean text = Core.scene != null && Core.scene.getKeyboardFocus() instanceof TextField;
        if(text != wantsText){
            wantsText = text;
            textInput(text);
        }
    }

    void handle(Event e){
        switch(e.getT()){
            case "kd": {
                KeyCode key = key(e.getCode());
                if(!e.isRepeat()) queue.keyDown(key);
                if(key == KeyCode.backspace) queue.keyTyped((char)8);
                else if(key == KeyCode.tab) queue.keyTyped('\t');
                else if(key == KeyCode.enter) queue.keyTyped((char)13);
                else if(key == KeyCode.forwardDel || key == KeyCode.del) queue.keyTyped((char)127);
                break;
            }
            case "ku":
                queue.keyUp(key(e.getCode()));
                break;
            case "ch": {
                String s = e.getKey();
                for(int i = 0; i < s.length(); i++) queue.keyTyped(s.charAt(i));
                break;
            }
            case "md": {
                KeyCode button = button(e.getB());
                move(0, e.getX(), e.getY());
                mouseButtons++;
                touched[0] = true;
                queue.touchDown(x[0], y[0], 0, button);
                break;
            }
            case "mu": {
                KeyCode button = button(e.getB());
                move(0, e.getX(), e.getY());
                mouseButtons = Math.max(0, mouseButtons - 1);
                touched[0] = mouseButtons > 0;
                queue.touchUp(x[0], y[0], 0, button);
                break;
            }
            case "mm":
                move(0, e.getX(), e.getY());
                if(mouseButtons > 0){
                    queue.touchDragged(x[0], y[0], 0);
                }else{
                    queue.mouseMoved(x[0], y[0]);
                }
                break;
            case "wh":
                queue.scrolled((float)e.getX(), (float)e.getY());
                break;
            case "td": {
                int p = pointerFor(e.getId(), true);
                if(p < 0) break;
                x[p] = sx(e.getX());
                y[p] = sy(e.getY());
                dx[p] = dy[p] = 0;
                touched[p] = true;
                queue.touchDown(x[p], y[p], p, KeyCode.mouseLeft);
                break;
            }
            case "tm": {
                int p = pointerFor(e.getId(), false);
                if(p < 0) break;
                move(p, e.getX(), e.getY());
                queue.touchDragged(x[p], y[p], p);
                break;
            }
            case "tu": {
                int p = pointerFor(e.getId(), false);
                if(p < 0) break;
                move(p, e.getX(), e.getY());
                touched[p] = false;
                touchIds[p] = -1;
                queue.touchUp(x[p], y[p], p, KeyCode.mouseLeft);
                break;
            }
            case "blur":
                //keys held while the page lost focus would otherwise stay down
                keyboard.postUpdate();
                for(KeyCode k : KeyCode.all){
                    if(keyboard.isPressed(k)) queue.keyUp(k);
                }
                break;
        }
    }

    void move(int p, double px, double py){
        int nx = sx(px), ny = sy(py);
        dx[p] = nx - x[p];
        dy[p] = ny - y[p];
        x[p] = nx;
        y[p] = ny;
    }

    static KeyCode button(int b){
        switch(b){
            case 1: return KeyCode.mouseMiddle;
            case 2: return KeyCode.mouseRight;
            case 3: return KeyCode.mouseBack;
            case 4: return KeyCode.mouseForward;
            default: return KeyCode.mouseLeft;
        }
    }

    static KeyCode key(String code){
        KeyCode key = codes.get(code);
        return key == null ? KeyCode.unknown : key;
    }

    void postUpdate(){
        keyboard.postUpdate();
        Arrays.fill(dx, 0);
        Arrays.fill(dy, 0);
    }

    @Override
    public int mouseX(){
        return x[0];
    }

    @Override
    public int mouseX(int pointer){
        return pointer >= 0 && pointer < maxPointers ? x[pointer] : 0;
    }

    @Override
    public int deltaX(){
        return dx[0];
    }

    @Override
    public int deltaX(int pointer){
        return pointer >= 0 && pointer < maxPointers ? dx[pointer] : 0;
    }

    @Override
    public int mouseY(){
        return y[0];
    }

    @Override
    public int mouseY(int pointer){
        return pointer >= 0 && pointer < maxPointers ? y[pointer] : 0;
    }

    @Override
    public int deltaY(){
        return dy[0];
    }

    @Override
    public int deltaY(int pointer){
        return pointer >= 0 && pointer < maxPointers ? dy[pointer] : 0;
    }

    @Override
    public boolean isTouched(){
        for(boolean t : touched) if(t) return true;
        return false;
    }

    @Override
    public boolean justTouched(){
        return keyTap(KeyCode.mouseLeft) || keyTap(KeyCode.mouseRight);
    }

    @Override
    public boolean isTouched(int pointer){
        return pointer >= 0 && pointer < maxPointers && touched[pointer];
    }

    @Override
    public int getTouches(){
        int n = 0;
        for(boolean t : touched) if(t) n++;
        return n;
    }

    @Override
    public long getCurrentEventTime(){
        return queue.getCurrentEventTime();
    }

    static void map(String code, KeyCode key){
        codes.put(code, key);
    }

    static{
        for(char c = 'A'; c <= 'Z'; c++) map("Key" + c, KeyCode.valueOf(String.valueOf(Character.toLowerCase(c))));
        for(int i = 0; i <= 9; i++){
            map("Digit" + i, KeyCode.valueOf("num" + i));
            map("Numpad" + i, KeyCode.valueOf("num" + i));
        }
        for(int i = 1; i <= 12; i++) map("F" + i, KeyCode.valueOf("f" + i));
        map("Backspace", KeyCode.backspace);
        map("Tab", KeyCode.tab);
        map("Enter", KeyCode.enter);
        map("NumpadEnter", KeyCode.enter);
        map("Escape", KeyCode.escape);
        map("Space", KeyCode.space);
        map("NumpadAdd", KeyCode.plus);
        map("Comma", KeyCode.comma);
        map("NumpadComma", KeyCode.comma);
        map("Minus", KeyCode.minus);
        map("NumpadSubtract", KeyCode.minus);
        map("Period", KeyCode.period);
        map("NumpadDecimal", KeyCode.period);
        map("Slash", KeyCode.slash);
        map("NumpadDivide", KeyCode.slash);
        map("NumpadMultiply", KeyCode.asterisk);
        map("Semicolon", KeyCode.semicolon);
        map("Quote", KeyCode.apostrophe);
        map("Equal", KeyCode.equals);
        map("BracketLeft", KeyCode.leftBracket);
        map("BracketRight", KeyCode.rightBracket);
        map("Backslash", KeyCode.backslash);
        map("IntlBackslash", KeyCode.backslash);
        map("Backquote", KeyCode.backtick);
        map("MetaLeft", KeyCode.sym);
        map("MetaRight", KeyCode.sym);
        map("ControlLeft", KeyCode.controlLeft);
        map("ControlRight", KeyCode.controlRight);
        map("ShiftLeft", KeyCode.shiftLeft);
        map("ShiftRight", KeyCode.shiftRight);
        map("AltLeft", KeyCode.altLeft);
        map("AltRight", KeyCode.altRight);
        map("CapsLock", KeyCode.capsLock);
        map("PrintScreen", KeyCode.printScreen);
        map("ScrollLock", KeyCode.scrollLock);
        map("Pause", KeyCode.pause);
        map("Insert", KeyCode.insert);
        map("Delete", KeyCode.del);
        map("Home", KeyCode.home);
        map("End", KeyCode.end);
        map("PageUp", KeyCode.pageUp);
        map("PageDown", KeyCode.pageDown);
        map("ArrowRight", KeyCode.right);
        map("ArrowLeft", KeyCode.left);
        map("ArrowDown", KeyCode.down);
        map("ArrowUp", KeyCode.up);
        map("ContextMenu", KeyCode.menu);
    }
}
