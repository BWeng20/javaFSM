package com.bw.fsm.serializer;

import com.bw.fsm.Fsm;
import com.bw.fsm.ScxmlReader;
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
    }
}