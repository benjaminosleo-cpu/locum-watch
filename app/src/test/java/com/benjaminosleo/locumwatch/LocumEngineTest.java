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
        // Her own message is only reported once, however often WhatsApp re-posts it.
        assertEquals(LocumEngine.Outcome.DUPLICATE, e.evaluate(new MessageParsing.Msg(G, "", req, t), t, st));
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

    /** An alarm in the test group must not silence the real group for an hour. */
    @Test
    public void quietHourIsPerGroup() {
        LocumEngine e = new LocumEngine();
        MemoryState st = new MemoryState();
        long t = 1_800_000_000_000L;
        String testGroup = "Locum Alarm Test";
        assertEquals(LocumEngine.Outcome.ALARM,
            e.evaluate(new MessageParsing.Msg(testGroup, "Ben", "Need locum tonight", t), t, st));
        assertEquals(LocumEngine.Outcome.COOLDOWN,
            e.evaluate(new MessageParsing.Msg(testGroup, "Ben", "SHO needed tonight, anyone available?", t + 60_000), t + 60_000, st));
        assertEquals(LocumEngine.Outcome.ALARM,
            e.evaluate(new MessageParsing.Msg(G, "Karen (ENT rota)", "Night on call needed tonight please Wythenshawe", t + 120_000), t + 120_000, st));
    }

    /** One message with a wildly wrong (future) time must not make later ones look old. */
    @Test
    public void bogusFutureTimestampDoesNotBlockLaterMessages() {
        LocumEngine e = new LocumEngine();
        MemoryState st = new MemoryState();
        long t = 1_800_000_000_000L;
        String k = "Karen (ENT rota)";
        e.evaluate(new MessageParsing.Msg(G, k, "Thanks all", t + 30L * 24 * 3_600_000), t, st);
        assertEquals(LocumEngine.Outcome.ALARM,
            e.evaluate(new MessageParsing.Msg(G, k, "Long day needed at MRI tomorrow", t + 20 * 60_000), t + 20 * 60_000, st));
    }
}
