package com.bw.fsm.serializer;

import com.bw.fsm.Fsm;
import com.bw.fsm.FsmExecutor;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FsmReaderTest {

    @Test
    void read() throws Exception {

        var executor = new FsmExecutor(false);
        URL source = FsmReaderTest.class.getResource("/simple.rfsm");

        try (InputStream is = source.openStream()) {
            FsmReader reader = new FsmReader(new DefaultProtocolReader(is));
            Fsm fsm = reader.read();
            assertEquals(5, fsm.statesNames.size());
            // @TODO
        }
    }
}