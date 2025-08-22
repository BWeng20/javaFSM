package com.bw.fsm;

import com.bw.fsm.executable_content.ForEach;
import com.bw.fsm.executable_content.If;
import com.bw.fsm.executable_content.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class ScxmlReaderTest {

    /**
     * Checks for and returns the initial S0 state, uses in some tests
     */
    State getS0(Fsm fsm) {
        Assertions.assertNotNull(fsm);
        Assertions.assertNotNull(fsm.pseudo_root);
        Assertions.assertNotNull(fsm.pseudo_root.initial);
        Assertions.assertEquals(1, fsm.pseudo_root.initial.target.size());
        State s0 = fsm.pseudo_root.initial.target.get(0);
        Assertions.assertEquals("s0", s0.name);
        return s0;
    }

    @Test
    void state() throws IOException {

        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="s0" version="1.0" datamodel="ecmascript">
                    <state id="s0"/>
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull(fsm);
        Assertions.assertNotNull(fsm.pseudo_root);
        Assertions.assertEquals(1, fsm.pseudo_root.states.size(), "Needs to read the state and not more.");
    }

    @Test
    void data() throws IOException {
        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" version="1.0" datamodel="ecmascript">
                    <datamodel>
                        <data id="Var1" expr="0"/>
                        <data id="Var2"/>
                        <data id="Var3"/>
                        <data id="Var4" expr="7"/>
                        <data id="Var5">
                            [1,2,3]
                        </data>
                    </datamodel>\
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

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
    void transition() throws IOException {
        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="s0" version="1.0" datamodel="ecmascript">
                    <state id="s0">
                      <transition event="ev1 ev2.*" cond="true" target="s1"/>\
                      <transition event="*" cond="false" target="s1"/>\
                    </state>
                    <state id="s1">
                    </state>
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));


        State s0 = getS0(fsm);
        Assertions.assertEquals(2, s0.transitions.size());

        Transition t0 = s0.transitions.data.get(0);
        Assertions.assertEquals(s0, t0.source);
        Assertions.assertNotNull(t0.cond);
        Assertions.assertEquals(2, t0.events.size());
        Assertions.assertEquals(1, t0.target.size());
        Assertions.assertEquals("ev1", t0.events.get(0));
        Assertions.assertEquals("ev2", t0.events.get(1));
        Assertions.assertFalse(t0.wildcard);

        State s1 = t0.target.get(0);
        Assertions.assertEquals("s1", s1.name);
        Assertions.assertEquals(0, s1.transitions.size());

        Transition t1 = s0.transitions.data.get(1);
        Assertions.assertEquals(s0, t1.source);
        Assertions.assertNotNull(t1.cond);
        Assertions.assertEquals(1, t1.events.size());
        Assertions.assertEquals(1, t1.target.size());
        Assertions.assertEquals("*", t1.events.get(0));
        Assertions.assertTrue(t1.wildcard);
        Assertions.assertEquals(s1, t1.target.get(0));

    }

    @Test
    void executableContent_if() throws IOException {
        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="s0" version="1.0" datamodel="ecmascript">
                    <state id="s0">
                        <onentry>
                            <if cond="false">
                              <log label="Test" expr="'Yes'"/>\
                            <else/>
                              <log label="Test" expr="'No'"/>\
                              <log label="Test" expr="'Again No'"/>\
                            </if>
                            <log label="Test" expr="'End'"/>\
                        </onentry>\
                    </state>
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        State s0 = getS0(fsm);

        Assertions.assertEquals(1, s0.onentry.size());

        ExecutableContentRegion block = s0.onentry.get(0);
        Assertions.assertEquals(2, block.content.size(), "'if' and last 'log' must be placed in two entries");
        var ecIf = block.content.get(0);
        Assertions.assertInstanceOf(If.class, ecIf);
        var ifEc = (If) ecIf;
        Assertions.assertEquals(1, ifEc.content.content.size());
        Assertions.assertInstanceOf(Log.class, ifEc.content.content.get(0));
        Assertions.assertEquals(new Data.Source("'Yes'"), ((Log) ifEc.content.content.get(0)).expression);
        Assertions.assertEquals("Test", ((Log) ifEc.content.content.get(0)).label);

        Assertions.assertEquals(2, ifEc.else_content.content.size());
        Assertions.assertInstanceOf(Log.class, ifEc.else_content.content.get(0));
        Assertions.assertEquals(new Data.Source("'No'"), ((Log) ifEc.else_content.content.get(0)).expression);

        Assertions.assertInstanceOf(Log.class, ifEc.else_content.content.get(1));
        Assertions.assertEquals(new Data.Source("'Again No'"), ((Log) ifEc.else_content.content.get(1)).expression);

        var ecLog = block.content.get(1);
        Assertions.assertInstanceOf(Log.class, ecLog);
        Assertions.assertEquals(new Data.Source("'End'"), ((Log) ecLog).expression);

    }

    @Test
    void executableContent_elseif() throws IOException {
        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="s0" version="1.0" datamodel="ecmascript">
                    <state id="s0">
                        <onentry>
                            <if cond="false">
                              <log label="Test" expr="'Yes'"/>\
                            <elseif cond="true"/>
                              <log label="Test" expr="'elseif'"/>\
                            <else/>
                              <log label="Test" expr="'else'"/>\
                            </if>
                            <log label="Test" expr="'End'"/>\
                        </onentry>\
                    </state>
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        State s0 = getS0(fsm);

        Assertions.assertEquals(1, s0.onentry.size());
        ExecutableContentRegion block = s0.onentry.get(0);
        Assertions.assertEquals(2, block.content.size(), "'if' and last 'log' must be placed in two entries");
        var ecIf = block.content.get(0);
        Assertions.assertInstanceOf(If.class, ecIf);
        var ifEc = (If) ecIf;
        Assertions.assertEquals(1, ifEc.content.content.size());
        Assertions.assertInstanceOf(Log.class, ifEc.content.content.get(0));
        Assertions.assertEquals(new Data.Source("'Yes'"), ((Log) ifEc.content.content.get(0)).expression);
        Assertions.assertEquals("Test", ((Log) ifEc.content.content.get(0)).label);

        Assertions.assertEquals(1, ifEc.else_content.content.size());
        Assertions.assertInstanceOf(If.class, ifEc.else_content.content.get(0), "Generated content for an elseif region shall be 'if'");

        If elseIf = (If) ifEc.else_content.content.get(0);
        Assertions.assertInstanceOf(Log.class, elseIf.content.content.get(0));
        Assertions.assertEquals(new Data.Source("'elseif'"), ((Log) elseIf.content.content.get(0)).expression);
        Assertions.assertEquals(new Data.Source("'else'"), ((Log) elseIf.else_content.content.get(0)).expression);

        var ecLog = block.content.get(1);
        Assertions.assertInstanceOf(Log.class, ecLog);
        Assertions.assertEquals(new Data.Source("'End'"), ((Log) ecLog).expression);
    }

    @Test
    void executableContent_foreach_wo_index() throws IOException {
        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="s0" version="1.0" datamodel="ecmascript">
                    <state id="s0">
                        <onentry>
                            <foreach item="Var2" array="Var3">
                               <log label="Test" expr="Var2"/>\
                            </foreach>\
                            <log expr="'End'"/>\
                        </onentry>\
                    </state>
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        State s0 = getS0(fsm);

        Assertions.assertEquals(1, s0.onentry.size());
        ExecutableContentRegion region = s0.onentry.get(0);
        Assertions.assertEquals(2, region.content.size(), "'foreach' and last 'log' must be placed in two entries");
        var ecforEach = region.content.get(0);
        Assertions.assertInstanceOf(ForEach.class, ecforEach);
        var forEachEc = (ForEach)ecforEach;

        Assertions.assertNull(forEachEc.index);
        Assertions.assertEquals("Var2", forEachEc.item);
        Assertions.assertEquals(new Data.Source("Var3"), forEachEc.array);
        Assertions.assertEquals(1, forEachEc.content.content.size());
    }

    @Test
    void executableContent_foreach_w_index() throws IOException {
        String scxml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="s0" version="1.0" datamodel="ecmascript">
                    <state id="s0">
                        <onentry>
                            <foreach index="index" item="Var2" array="Var3">
                               <log label="Test" expr="Var2"/>\
                            </foreach>\
                            <log expr="'End'"/>\
                        </onentry>\
                    </state>
                </scxml>""";

        ScxmlReader reader = new ScxmlReader();
        Fsm fsm = reader.parse(null, new ByteArrayInputStream(scxml.getBytes(StandardCharsets.UTF_8)));

        State s0 = getS0(fsm);

        Assertions.assertEquals(1, s0.onentry.size());
        ExecutableContentRegion block = s0.onentry.get(0);
        Assertions.assertEquals(2, block.content.size(), "'foreach' and last 'log' must be placed in two entries");
        var ecforEach = block.content.get(0);
        Assertions.assertInstanceOf(ForEach.class, ecforEach);
        var forEachEc = (ForEach) ecforEach;

        Assertions.assertEquals("index", forEachEc.index);
        Assertions.assertEquals("Var2", forEachEc.item);
        Assertions.assertEquals(new Data.Source("Var3"), forEachEc.array);
        Assertions.assertEquals(1, forEachEc.content.content.size());
    }
}