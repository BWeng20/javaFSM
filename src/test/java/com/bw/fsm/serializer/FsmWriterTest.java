package com.bw.fsm.serializer;

import com.bw.fsm.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class FsmWriterTest {

    @Test
    void write() throws IOException {
        URL source = FsmWriterTest.class.getResource("/serializer_test.scxml");
        assertNotNull(source);

        ScxmlReader sr = new ScxmlReader();
        Fsm fsm = sr.parse_from_url(source);
        assertNotNull(fsm);

        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);

        FsmWriter writer = new FsmWriter(new DefaultProtocolWriter(os));
        writer.write(fsm);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        FsmReader reader = new FsmReader(new DefaultProtocolReader(is).throwOnError());
        Fsm fsm2 = reader.read();
        assertFalse(reader.has_error());

        assertEquals(fsm.name, fsm2.name);
        assertEquals(fsm.datamodel, fsm2.datamodel);
        assertEquals(fsm.statesNames.size(), fsm2.statesNames.size());
        for (var entry : fsm.statesNames.entrySet()) {
            State s2 = fsm2.statesNames.get(entry.getKey());
            assertNotNull(s2, "State " + entry.getKey() + " missing");
            checkState(entry.getValue(), s2);
        }
    }

    void checkState(State s1, State s2) {
        assertEquals(s1.data, s2.data);
        assertEquals(s1.states.size(), s2.states.size());
        assertEquals((s1.parent != null ? s1.parent.name : null), (s2.parent != null ? s2.parent.name : null));
        assertEquals(s1.history_type, s2.history_type);
        assertEquals(s1.donedata, s2.donedata);
        checkTransition(s1.initial, s2.initial);
        assertEquals(s1.transitions.size(), s2.transitions.size());
        for (int i = 0; i < s1.transitions.size(); ++i)
            checkTransition(s1.transitions.data.get(i), s2.transitions.data.get(i));
    }

    public void checkTransition(Transition t1, Transition t2) {
        if (t1 == null && t2 == null)
            return;
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotNull(t1.source);
        assertNotNull(t2.source, "Source of Transition can't not be null");
        assertEquals(t1.source.name, t2.source.name);
        assertEquals(t1.wildcard, t2.wildcard);
        assertEquals(t1.transition_type, t2.transition_type);
        assertEquals(t1.cond, t2.cond);
        assertEquals(t1.events, t2.events);
        checkContentBlock(t1.content, t2.content);
    }

    public void checkContentBlock(ExecutableContentBlock b1, ExecutableContentBlock b2) {
        if (b1 == null && b2 == null)
            return;
        assertNotNull(b1);
        assertNotNull(b2);
        assertEquals(b1.content.size(), b2.content.size());
        for (int i = 0; i < b1.content.size(); ++i)
            checkContent(b1.content.get(i), b2.content.get(i));

    }

    public void checkContent(ExecutableContent e1, ExecutableContent e2) {
        assertNotNull(e1);
        assertNotNull(e2);
        assertEquals(e1.get_type(), e2.get_type());
        assertEquals(e1.get_trace(), e2.get_trace());
    }
}