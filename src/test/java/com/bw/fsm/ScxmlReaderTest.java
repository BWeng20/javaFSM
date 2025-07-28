package com.bw.fsm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class ScxmlReaderTest {

    @Test
    void read_state() throws XMLStreamException, IOException {

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
    void read_data()  throws XMLStreamException, IOException {
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
        Assertions.assertEquals( 5, fsm.pseudo_root.data.size());
        Assertions.assertEquals( new Data.Source("0"), fsm.pseudo_root.data.get("Var1"));
        Assertions.assertEquals( Data.NULL, fsm.pseudo_root.data.get("Var2"));
        Assertions.assertEquals( Data.NULL, fsm.pseudo_root.data.get("Var3"));
        Assertions.assertEquals( new Data.Source("7"), fsm.pseudo_root.data.get("Var4"));
        Assertions.assertEquals( new Data.Source("[1,2,3]"), fsm.pseudo_root.data.get("Var5"));
    }
}