package com.benjaminosleo.locumwatch;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class LocumEngineTest {
    private static final String G = "ENT Locums Group";

    /** Replaying the history the way WhatsApp delivers it gives one alarm per request burst. */
    @Test
    public void replayGives145AlarmsEvenWithReposts() throws Exception {
        List<HistoryFixture.Row> rows = HistoryFixture.load();
        LocumEngine engine = new LocumEngine();
        MemoryState st = new MemoryState();
        int alarms = 0;
        for (int i = 0; i < rows.size(); i++) {
            HistoryFixture.Row r = rows.get(i);
            // WhatsApp re-posts the last few messages each time: feed them again first.
            for (int j = Math.max(0, i - 5); j < i; j++) {
                HistoryFixture.Row p = rows.get(j);
                assertEquals(LocumEngine.Outcome.DUPLICATE,
                    engine.evaluate(new MessageParsing.Msg(G, p.sender, p.text, p.time), r.time, st));
            }
            if (engine.evaluate(new MessageParsing.Msg(G, r.sender, r.text, r.time), r.time, st)
                    == LocumEngine.Outcome.ALARM) alarms++;
        }
        assertEquals(145, alarms);
    }

    @Test
    public void switchCooldownAgeAndOwnMessages() {
        LocumEngine e = new LocumEngine();
        MemoryState st = new MemoryState();
        long t = 1_800_000_000_000L;
        String k = "Karen (ENT rota)";
        String req = "Long day needed at Wythenshawe today anyone available please";

        assertEquals(LocumEngine.Outcome.OWN_MESSAGE, e.evaluate(new MessageParsing.Msg(G, "", req, t), t, st));
        st.armed = false;
        assertEquals(LocumEngine.Outcome.SWITCHED_OFF, e.evaluate(new MessageParsing.Msg(G, k, req, t + 1), t + 1, st));
        st.armed = true;
        assertEquals(LocumEngine.Outcome.ALARM, e.evaluate(new MessageParsing.Msg(G, k, req, t + 2), t + 2, st));
        assertEquals(LocumEngine.Outcome.COOLDOWN, e.evaluate(new MessageParsing.Msg(G, k, req + "!", t + 60_000), t + 60_000, st));
        long later = t + 3 * 3_600_000L;
        assertEquals(LocumEngine.Outcome.TOO_OLD,
            e.evaluate(new MessageParsing.Msg(G, k, req + "?", later - 2 * 3_600_000L + 1), later, st));
        assertEquals(LocumEngine.Outcome.NOT_REQUEST, e.evaluate(new MessageParsing.Msg(G, k, "Thanks all", later + 1), later + 1, st));
        // No timestamp (title/text fallback): the same text twice is only judged once.
        assertEquals(LocumEngine.Outcome.ALARM, e.evaluate(new MessageParsing.Msg(G, k, req, 0), later + 2, st));
        assertEquals(LocumEngine.Outcome.DUPLICATE, e.evaluate(new MessageParsing.Msg(G, k, req, 0), later + 3, st));
    }
}
