package com.benjaminosleo.locumwatch;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

/** Every message in the group's history must get the decision the tuned filter gave it. */
public class LocumFilterHistoryTest {
    @Test
    public void matchesTunedDecisionsOnWholeHistory() throws Exception {
        List<HistoryFixture.Row> rows = HistoryFixture.load();
        StringBuilder wrong = new StringBuilder();
        int requests = 0;
        for (HistoryFixture.Row r : rows) {
            boolean got = LocumFilter.isAlarm(r.sender, r.text);
            if (got) requests++;
            if (got != r.expected) wrong.append(r.expected ? "MISSED: " : "FALSE ALARM: ")
                .append(r.sender).append(" | ").append(r.text).append('\n');
        }
        assertEquals("Decisions that differ from the tuned filter:\n" + wrong, "", wrong.toString());
        assertEquals(441, rows.size());
        assertEquals(172, requests);
    }

    @Test
    public void swapsAndRepliesNeverAlarm() {
        String[][] quiet = {
            {"Member 1", "Hi! Would anyone be willing to swap weekend nights 25-27th September"},
            {"Member 2", "Sorry Karen I'm on call that weekend"},
            {"Member 3", "Anyone know if teaching is on today?"},
            {"Member 4", "I'm on call tonight if anyone needs advice"},
            {"Karen (ENT rota)", "Morning this will have to be the full week swap including zeros please"},
            {"Karen (ENT rota)", "Just Friday left now please"},
            {"Karen (ENT rota)", "All covered  now thanks"},
        };
        for (String[] q : quiet) assertEquals(q[1], false, LocumFilter.isAlarm(q[0], q[1]));
    }

    @Test
    public void typicalRequestsAlarm() {
        String[][] loud = {
            {"Karen (ENT rota)", "Night on call needed tonight please Wythenshawe"},
            {"Karen (ENT rota)", "Nights 26th 27th 28th June MRI"},
            {"Karen (ENT rota)", "Anyone available Tues"},
            {"Member 5", "Anyone available to do tonight’s sho oncall At wythenshawe please"},
            {"Member 6", "Gap in the rota tonight, anyone available?"},
        };
        for (String[] l : loud) assertEquals(l[1], true, LocumFilter.isAlarm(l[0], l[1]));
    }

    /** If Karen's contact is renamed or unsaved ("~Karen"), her messages still get her rule. */
    @Test
    public void karenIsRecognisedHoweverSheIsSaved() {
        String futureShift = "Nights 26th 27th 28th June MRI";
        assertEquals(true, LocumFilter.isAlarm("Karen (ENT rota)", futureShift));
        assertEquals(true, LocumFilter.isAlarm("~Karen", futureShift));
        assertEquals(true, LocumFilter.isAlarm("Karen Rota Coordinator", futureShift));
        assertEquals(false, LocumFilter.isAlarm("Member 7", futureShift));
    }

    /** Wordings Ben tried on the night that were judged as designed. */
    @Test
    public void colleaguesNeedATimeAndAnAskingWord() {
        assertEquals(true, LocumFilter.isAlarm("Ben", "Need locum tonight"));
        assertEquals(false, LocumFilter.isAlarm("Ben", "Locum at MRI please"));
        assertEquals(false, LocumFilter.isAlarm("Ben", "Long day at MRI tomorrow"));
        assertEquals(true, LocumFilter.isAlarm("Ben", "Long day needed at MRI tomorrow"));
        assertEquals(true, LocumFilter.isAlarm("Karen (ENT rota)", "Long day at MRI tomorrow"));
    }
}
