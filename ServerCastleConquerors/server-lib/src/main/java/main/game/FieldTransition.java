package main.game;

import messagesbase.messagesfromserver.ETerrain;

public class FieldTransition {
    private final ETerrain from;
    private final ETerrain to;

    public FieldTransition(ETerrain from, ETerrain to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldTransition that = (FieldTransition) o;

        if (from != that.from) return false;
        return to == that.to;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }
}

