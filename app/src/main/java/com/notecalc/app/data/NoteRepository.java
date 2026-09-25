package com.notecalc.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteRepository {
    private static final String PREF_NAME = "jacks_calc_prefs";
    private static final String KEY_NOTES = "notes_json";
    private final SharedPreferences prefs;

    public NoteRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<NoteItem> getAllNotes() {
        List<NoteItem> list = new ArrayList<>();
        String json = prefs.getString(KEY_NOTES, null);
        if (json == null || json.isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new NoteItem(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("title", "Untitled Note"),
                        obj.optString("content", ""),
                        obj.optLong("timestamp", System.currentTimeMillis())
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void saveNote(NoteItem note) {
        List<NoteItem> notes = getAllNotes();
        boolean found = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(note.getId())) {
                notes.set(i, note);
                found = true;
                break;
            }
        }
        if (!found) {
            notes.add(0, note);
        }
        saveAll(notes);
    }

    public void deleteNote(String id) {
        List<NoteItem> notes = getAllNotes();
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(id)) {
                notes.remove(i);
                break;
            }
        }
        saveAll(notes);
    }

    private void saveAll(List<NoteItem> notes) {
        try {
            JSONArray arr = new JSONArray();
            for (NoteItem item : notes) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("title", item.getTitle());
                obj.put("content", item.getContent());
                obj.put("timestamp", item.getTimestamp());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_NOTES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
