package com.chatwatch;

import java.text.Normalizer;
import java.util.*;

/** Pure-Java matcher: catches leetspeak, spacing/punctuation tricks, stretched letters and common endings. */
public final class WordFilter {
    private static final String[] SUFFIX = {"", "s", "es", "ed", "er", "ers", "ing", "in", "y", "ie", "ies"};

    private final List<String> squashed = new ArrayList<>();   // runs of 3+ -> 2
    private final List<String> collapsed = new ArrayList<>();  // runs -> 1
    private final Set<String> allow = new HashSet<>();
    private boolean fuzzy;

    public synchronized void configure(Collection<String> words, Collection<String> allowed, boolean fuzzy) {
        squashed.clear(); collapsed.clear(); allow.clear();
        this.fuzzy = fuzzy;
        for (String w : words) {
            String n = norm(w);
            if (n.isEmpty()) continue;
            squashed.add(squash(n));
            collapsed.add(collapse(n));
        }
        for (String a : allowed) {
            String n = norm(a);
            if (!n.isEmpty()) { allow.add(n); allow.add(squash(n)); allow.add(collapse(n)); }
        }
    }

    public synchronized boolean isBad(String message) {
        for (String t : tokens(message)) if (tokenBad(t)) return true;
        return false;
    }

    private boolean tokenBad(String t) {
        String sq = squash(t), co = collapse(t);
        if (allow.contains(t) || allow.contains(sq) || allow.contains(co)) return false;
        for (int i = 0; i < squashed.size(); i++) {
            String wsq = squashed.get(i), wco = collapsed.get(i);
            for (String suf : SUFFIX) if (sq.equals(wsq + suf)) return true;
            if (wco.length() >= 4 && co.contains(wco)) return true;
            if (fuzzy && wco.length() >= 5 && Math.abs(co.length() - wco.length()) <= 1 && lev(co, wco) <= 1) return true;
        }
        return false;
    }

    private static List<String> tokens(String message) {
        List<String> norms = new ArrayList<>();
        for (String raw : message.split("\\s+")) {
            String n = norm(raw);
            if (!n.isEmpty()) norms.add(n);
        }
        List<String> out = new ArrayList<>(norms);
        // "f u c k" style: join runs of 3+ single-letter tokens
        int i = 0;
        while (i < norms.size()) {
            if (norms.get(i).length() == 1) {
                int j = i; StringBuilder sb = new StringBuilder();
                while (j < norms.size() && norms.get(j).length() == 1) { sb.append(norms.get(j)); j++; }
                if (j - i >= 3) out.add(sb.toString());
                i = j;
            } else i++;
        }
        return out;
    }

    static String norm(String s) {
        String d = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (char c : d.toCharArray()) {
            switch (c) {
                case '@': case '4': c = 'a'; break;
                case '3': c = 'e'; break;
                case '1': case '!': c = 'i'; break;
                case '0': c = 'o'; break;
                case '$': case '5': c = 's'; break;
                case '7': case '+': c = 't'; break;
                default: break;
            }
            if (c >= 'a' && c <= 'z') sb.append(c);
        }
        return sb.toString();
    }

    static String squash(String s) { return runs(s, 2); }
    static String collapse(String s) { return runs(s, 1); }

    private static String runs(String s, int keep) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int j = i;
            while (j < s.length() && s.charAt(j) == s.charAt(i)) j++;
            int len = j - i;
            int n = keep == 1 ? 1 : (len >= 3 ? 2 : len);
            for (int k = 0; k < n; k++) sb.append(s.charAt(i));
            i = j;
        }
        return sb.toString();
    }

    private static int lev(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++)
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
