package com.benjaminosleo.locumwatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Loads the anonymised group history: expected flag, time, sender, text. */
final class HistoryFixture {
    static final class Row {
        final boolean expected; final long time; final String sender; final String text;
        Row(boolean e, long t, String s, String x) { expected = e; time = t; sender = s; text = x; }
    }

    static List<Row> load() throws Exception {
        InputStream in = HistoryFixture.class.getClassLoader().getResourceAsStream("history.tsv");
        if (in == null) throw new IllegalStateException("history.tsv not found");
        List<Row> rows = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            Base64.Decoder d = Base64.getDecoder();
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\t");
                rows.add(new Row(f[0].equals("1"), Long.parseLong(f[1]),
                    new String(d.decode(f[2]), StandardCharsets.UTF_8),
                    new String(d.decode(f[3]), StandardCharsets.UTF_8)));
            }
        }
        return rows;
    }
}
