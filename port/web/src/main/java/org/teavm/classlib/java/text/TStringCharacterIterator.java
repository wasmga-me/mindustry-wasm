package org.teavm.classlib.java.text;

public final class TStringCharacterIterator implements TCharacterIterator {
    private final String text;
    private final int begin;
    private final int end;
    private int pos;

    public TStringCharacterIterator(String text) {
        this(text, 0, text.length(), 0);
    }

    public TStringCharacterIterator(String text, int pos) {
        this(text, 0, text.length(), pos);
    }

    public TStringCharacterIterator(String text, int begin, int end, int pos) {
        this.text = text;
        this.begin = begin;
        this.end = end;
        this.pos = pos;
    }

    @Override
    public Object clone() {
        return new TStringCharacterIterator(text, begin, end, pos);
    }

    @Override
    public char current() {
        return pos >= begin && pos < end ? text.charAt(pos) : DONE;
    }

    @Override
    public char first() {
        pos = begin;
        return current();
    }

    @Override
    public int getBeginIndex() {
        return begin;
    }

    @Override
    public int getEndIndex() {
        return end;
    }

    @Override
    public int getIndex() {
        return pos;
    }

    @Override
    public char last() {
        pos = end > begin ? end - 1 : end;
        return current();
    }

    @Override
    public char next() {
        if (pos < end) {
            pos++;
        }
        return current();
    }

    @Override
    public char previous() {
        if (pos <= begin) {
            return DONE;
        }
        pos--;
        return current();
    }

    @Override
    public char setIndex(int location) {
        pos = location;
        return current();
    }
}
