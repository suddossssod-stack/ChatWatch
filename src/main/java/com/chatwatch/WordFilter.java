package com.chatwatch;

import java.text.Normalizer;
import java.util.*;

public final class WordFilter {
    private static final String[] SUFFIX = {"", "s", "es", "ed", "er", "ers", "ing", "in", "y", "ie", "ies"};

    private final List<String> squashed = new ArrayList<>();
    private final List<String> collapsed = new ArrayList<>();
    private final Set<String> allow = new HashSet<>();
    private boolean fuzzy;

    public synchronized void configure(Collection<String> words, Collection<String> allowed, boolean fuzzy) {
        squashed.clear();
        collapsed.clear();
        allow.clear();
        this.fuzzy = fuzzy;

        for (String w : words) {
            String n = norm(w);
            if (n.isEmpty()) continue;
            squashed.add(squash(n));
            collapsed.add(collapse(n));
        }

        for (String a : allowed) {
            String n = norm(a);
            if (!n.isEmpty()) {
                allow.add(n);
                allow.add(squash(n));
                allow.add(collapse(n));
            }
        }
    }

    public synchronized boolean isBad(String message) {
        return !badRanges(message).isEmpty();
    }

    public synchronized List<int[]> badRanges(String message) {
        List<int[]> ranges = new ArrayList<>();
        if (message == null || message.isEmpty()) return ranges;

        List<int[]> tokenBounds = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\S+").matcher(message);

        while (matcher.find()) {
            String normalized = norm(matcher.group());
            tokenBounds.add(new int[]{matcher.start(), matcher.end()});

            if (!normalized.isEmpty() && tokenBad(normalized)) {
                ranges.add(new int[]{matcher.start(), matcher.end()});
            }
        }

        int i = 0;
        while (i < tokenBounds.size()) {
            int j = i;
            StringBuilder joined = new StringBuilder();

            while (j < tokenBounds.size()) {
                int[] b = tokenBounds.get(j);
                String n = norm(message.substring(b[0], b[1]));
                if (n.length() != 1) break;
                joined.append(n);
                j++;
            }

            if (j - i >= 3 && tokenBad(joined.toString())) {
                ranges.add(new int[]{tokenBounds.get(i)[0], tokenBounds.get(j - 1)[1]});
            }

            i = j > i ? j : i + 1;
        }

        ranges.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();

        for (int[] r : ranges) {
            if (merged.isEmpty() || r[0] > merged.get(merged.size() - 1)[1]) {
                merged.add(r.clone());
            } else {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], r[1]);
            }
        }

        return merged;
    }

    private boolean tokenBad(String t) {
        String sq = squash(t);
        String co = collapse(t);

        if (allow.contains(t) || allow.contains(sq) || allow.contains(co)) return false;

        for (int i = 0; i < squashed.size(); i++) {
            String wsq = squashed.get(i);
            String wco = collapsed.get(i);

            for (String suf : SUFFIX) {
                if (sq.equals(wsq + suf)) return true;
            }

            if (wco.length() >= 4 && co.contains(wco)) return true;

            if (fuzzy && wco.length() >= 5 &&
                    Math.abs(co.length() - wco.length()) <= 1 &&
                    lev(co, wco) <= 1) return true;
        }

        return false;
    }

    static String norm(String s) {
        String d = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

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

    static String squash(String s) {
        return runs(s, 2);
    }

    static String collapse(String s) {
        return runs(s, 1);
    }

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
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;

            for (int j = 1; j <= b.length(); j++) {
                cur[j] = Math.min(
                        Math.min(cur[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)
                );
            }

            int[] t = prev;
            prev = cur;
            cur = t;
        }

        return prev[b.length()];
    }
}
