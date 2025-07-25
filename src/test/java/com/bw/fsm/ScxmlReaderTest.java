package com.bw.fsm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class ScxmlReaderTest {

    @Test
    void read() throws XMLStreamException, IOException {
        ScxmlReader reader = new ScxmlReader();

        String scxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"s0\" version=\"1.0\" datamodel=\"ecmascript\">\n" +
                "    <state id=\"s0\"/>\n" +
                "</scxml>";

        Fsm fsm = reader.read(new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull( fsm);
        Assertions.assertNotNull( fsm.pseudo_root);

    }
}