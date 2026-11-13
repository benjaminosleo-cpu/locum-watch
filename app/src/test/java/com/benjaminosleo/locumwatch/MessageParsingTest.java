package com.benjaminosleo.locumwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MessageParsingTest {
    private static final List<String> GROUPS = Arrays.asList("ENT Locums Group", "Locum Alarm Test");

    @Test
    public void groupTitleWithSenderInText() {
        MessageParsing.Msg m = MessageParsing.parseTitleText("ENT Locums Group (3 messages)",
            "Karen (ENT rota): Nights available tonight Wythenshawe please", GROUPS);
        assertEquals("ENT Locums Group", m.group);
        assertEquals("Karen (ENT rota)", m.sender);
        assertEquals("Nights available tonight Wythenshawe please", m.text);
    }

    @Test
    public void senderAtGroupTitle() {
        MessageParsing.Msg m = MessageParsing.parseTitleText("Karen (ENT rota) @ ENT Locums Group",
            "RMCH locum needed Sat and Sunday please", GROUPS);
        assertEquals("Karen (ENT rota)", m.sender);
        assertEquals("RMCH locum needed Sat and Sunday please", m.text);
    }

    @Test
    public void groupColonSenderTitle() {
        MessageParsing.Msg m = MessageParsing.parseTitleText("ENT Locums Group: Karen (ENT rota)",
            "Anyone free now to do North locum", GROUPS);
        assertEquals("Karen (ENT rota)", m.sender);
    }

    @Test
    public void otherChatsIgnored() {
        assertNull(MessageParsing.parseTitleText("Family", "Mum: SHO needed tonight", GROUPS));
        assertEquals("Locum Alarm Test", MessageParsing.matchGroup("locum alarm test", GROUPS));
    }
}
