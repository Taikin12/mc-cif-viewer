package dev.taikin.mcplugins.cifviewer.render;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerModels {

    private final Map<Integer, ViewSession> models = new LinkedHashMap<>();
    private int activeId;
    private int nextId = 1;

    public List<ViewSession> models() {
        return List.copyOf(models.values());
    }

    public int size() {
        return models.size();
    }

    public ViewSession active() {
        return models.get(activeId);
    }

    public int activeId() {
        return activeId;
    }

    public int nextId() {
        return nextId;
    }

    public void nextId(int nextId) {
        this.nextId = Math.max(this.nextId, Math.max(1, nextId));
    }

    public ViewSession get(int id) {
        return models.get(id);
    }

    public int add(ViewSession session) {
        int id = nextId++;
        session.id(id);
        models.put(id, session);
        activeId = id;
        return id;
    }

    public void restore(int id, ViewSession session) {
        session.id(id);
        models.put(id, session);
        nextId = Math.max(nextId, id + 1);
    }

    public boolean select(int id) {
        if (!models.containsKey(id)) {
            return false;
        }
        activeId = id;
        return true;
    }

    public ViewSession remove(int id) {
        ViewSession removed = models.remove(id);
        if (removed != null && activeId == id) {
            activeId = models.keySet().stream().findFirst().orElse(0);
        }
        return removed;
    }

    public void clear() {
        models.clear();
        activeId = 0;
        nextId = 1;
    }
}
