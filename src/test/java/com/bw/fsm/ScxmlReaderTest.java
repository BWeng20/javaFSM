package com.bw.fsm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class ScxmlReaderTest {

    @Test
    void state() throws XMLStreamException, IOException {

        String scxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"s0\" version=\"1.0\" datamodel=\"ecmascript\">\n" +
                "    <state id=\"s0\"/>\n" +
                "</scxml>";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.read(new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull(fsm);
        Assertions.assertNotNull(fsm.pseudo_root);
        Assertions.assertEquals(1, fsm.pseudo_root.states.size(), "Needs to read the state and not more.");
    }

    @Test
    void data() throws XMLStreamException, IOException {
        String scxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\" datamodel=\"ecmascript\">\n" +
                "    <datamodel>\n" +
                "        <data id=\"Var1\" expr=\"0\"/>\n" +
                "        <data id=\"Var2\"/>\n" +
                "        <data id=\"Var3\"/>\n" +
                "        <data id=\"Var4\" expr=\"7\"/>\n" +
                "        <data id=\"Var5\">\n" +
                "            [1,2,3]\n" +
                "        </data>\n" +
                "    </datamodel>" +
                "</scxml>";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.read(new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull(fsm);
        Assertions.assertNotNull(fsm.pseudo_root);
        Assertions.assertEquals(5, fsm.pseudo_root.data.size());
        Assertions.assertEquals(new Data.Source("0"), fsm.pseudo_root.data.get("Var1"));
        Assertions.assertEquals(Data.Null.NULL, fsm.pseudo_root.data.get("Var2"));
        Assertions.assertEquals(Data.Null.NULL, fsm.pseudo_root.data.get("Var3"));
        Assertions.assertEquals(new Data.Source("7"), fsm.pseudo_root.data.get("Var4"));
        Assertions.assertEquals(new Data.Source("[1,2,3]"), fsm.pseudo_root.data.get("Var5"));
    }

    @Test
    void transition() throws XMLStreamException, IOException {
        String scxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"s0\" version=\"1.0\" datamodel=\"ecmascript\">\n" +
                "    <state id=\"s0\">\n" +
                "      <transition event=\"ev1 ev2.*\" cond=\"true\" target=\"s1\"/>" +
                "      <transition event=\"*\" cond=\"false\" target=\"s1\"/>" +
                "    </state>\n" +
                "    <state id=\"s1\">\n" +
                "    </state>\n" +
                "</scxml>";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.read(new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull(fsm);
        Assertions.assertNotNull(fsm.pseudo_root);
        Assertions.assertNotNull(fsm.pseudo_root.initial);
        Assertions.assertEquals(1, fsm.pseudo_root.initial.target.size());
        State s0 = fsm.pseudo_root.initial.target.get(0);
        Assertions.assertEquals( "s0", s0.name);
        Assertions.assertEquals(2, s0.transitions.size());

        Transition t0 = s0.transitions.data.get(0);
        Assertions.assertEquals(s0, t0.source);
        Assertions.assertNotNull(t0.cond);
        Assertions.assertEquals(2, t0.events.size());
        Assertions.assertEquals(1, t0.target.size());
        Assertions.assertEquals("ev1", t0.events.get(0));
        Assertions.assertEquals("ev2", t0.events.get(1));
        Assertions.assertFalse( t0.wildcard);

        State s1 = t0.target.get(0);
        Assertions.assertEquals( "s1", s1.name);
        Assertions.assertEquals(0, s1.transitions.size());

        Transition t1 = s0.transitions.data.get(1);
        Assertions.assertEquals(s0, t1.source);
        Assertions.assertNotNull(t1.cond);
        Assertions.assertEquals(1, t1.events.size());
        Assertions.assertEquals(1, t1.target.size());
        Assertions.assertEquals("*", t1.events.get(0));
        Assertions.assertTrue( t1.wildcard);
        Assertions.assertEquals( s1, t1.target.get(0));

    }

}