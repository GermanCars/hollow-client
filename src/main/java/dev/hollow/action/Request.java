package dev.hollow.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Something a module wants to do this tick.
 *
 * @param rot   where to look, or null when the direction doesn't matter
 * @param exact the look direction is needed for the result (block facing), even with rotations off
 * @param slot  hotbar slot to hold, or -1 for the current one
 */
public record Request(Priority priority, Rot rot, boolean exact, int slot, Runnable run) {
    static final float SHARED_ANGLE = 12f;

    /** Picks what runs this tick: the highest priority sets the view, others run only if that view suits them. */
    public static List<Request> choose(List<Request> all, boolean rotate) {
        List<Request> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparing(Request::priority));
        Rot view = null;
        List<Request> out = new ArrayList<>();
        for (Request r : sorted) {
            if (r.needsView(rotate)) {
                if (view == null) view = r.rot;
                else if (r.rot.angleTo(view) > (r.exact ? 0.01f : SHARED_ANGLE)) continue;
            }
            out.add(r);
        }
        return out;
    }

    public static Rot view(List<Request> chosen, boolean rotate) {
        return chosen.stream().filter(r -> r.needsView(rotate)).map(Request::rot).findFirst().orElse(null);
    }

    private boolean needsView(boolean rotate) { return rot != null && (rotate || exact); }
}
