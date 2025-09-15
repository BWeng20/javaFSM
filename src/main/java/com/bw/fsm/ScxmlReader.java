package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;
import com.bw.fsm.executableContent.*;
import com.bw.fsm.executableContent.Log;
import org.jetbrains.annotations.NotNull;

import javax.xml.stream.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads SCXML files via SAX parser and produces an FSM model.<br>
 */
@SuppressWarnings("unused")
public class ScxmlReader {

    public static final String TAG_SCXML = "scxml";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_BINDING = "binding";
    public static final String ATTR_DATAMODEL = "datamodel";
    public static final String TAG_DATAMODEL = "datamodel";
    public static final String TAG_DATA = "data";
    public static final String TAG_VERSION = "version";
    public static final String TAG_INITIAL = "initial";
    public static final String ATTR_ID = "id";
    public static final String TAG_STATE = "state";
    public static final String ATTR_INITIAL = "initial";
    public static final String TAG_HISTORY = "history";
    public static final String TAG_PARALLEL = "parallel";
    public static final String TAG_FINAL = "final";
    public static final String TAG_TRANSITION = "transition";
    public static final String ATTR_COND = "cond";
    public static final String TAG_EVENT = "event";
    public static final String TAG_TYPE = "type";
    public static final String TAG_ON_ENTRY = "onentry";
    public static final String TAG_ON_EXIT = "onexit";
    public static final String TAG_INVOKE = "invoke";
    public static final String ATTR_SRCEXPR = "srcexpr";
    public static final String ATTR_AUTOFORWARD = "autoforward";
    public static final String TAG_FINALIZE = "finalize";
    public static final String TAG_DONEDATA = "donedata";
    public static final String TAG_INCLUDE = "include";
    public static final String TAG_HREF = "href";
    public static final String ATTR_PARSE = "parse";
    public static final String ATTR_XPOINTER = "xpointer";
    public static final String TAG_RAISE = "raise";
    public static final String TAG_SEND = "send";
    public static final String ATTR_EVENT = "event";

    // Executable content
    public static final String ATTR_EVENTEXPR = "eventexpr";
    public static final String ATTR_TARGET = "target";
    public static final String ATTR_TARGETEXPR = "targetexpr";
    public static final String TARGET_INTERNAL = "_internal";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_TYPEEXPR = "typeexpr";
    public static final String ATTR_IDLOCATION = "idlocation";
    public static final String ATTR_DELAY = "delay";
    public static final String ATTR_DELAYEXPR = "delayexpr";
    public static final String ATTR_NAMELIST = "namelist";
    public static final String TAG_PARAM = "param";
    public static final String TAG_CONTENT = "content";
    public static final String TAG_LOG = "log";
    public static final String TAG_SCRIPT = "script";
    public static final String ATTR_SRC = "src";
    public static final String TAG_ASSIGN = "assign";
    public static final String ATTR_LOCATION = "location";
    public static final String TAG_IF = "if";
    public static final String TAG_FOR_EACH = "foreach";
    public static final String ATTR_ARRAY = "array";
    public static final String ATTR_ITEM = "item";
    public static final String ATTR_INDEX = "index";
    public static final String TAG_CANCEL = "cancel";
    public static final String ATTR_SENDIDEXPR = "sendidexpr";
    public static final String ATTR_SENDID = "sendid";
    public static final String TAG_ELSE = "else";
    public static final String TAG_ELSEIF = "elseif";
    public static final String ATTR_LABEL = "label";
    public static final String ATTR_EXPR = "expr";
    public static final String NS_XINCLUDE = "http://www.w3.org/2001/XInclude";
    public static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    static final AtomicInteger DOC_ID_COUNTER = new AtomicInteger(1);
    static final AtomicInteger SOURCE_ID_COUNTER = new AtomicInteger(1);
    static final Pattern split_whitespace = Pattern.compile("\\s");

    static XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    static XMLInputFactory factory = XMLInputFactory.newInstance();

    IncludePaths includePaths = new IncludePaths();

    /**
     * Read and parse the FSM from an XML file
     */
    public Fsm parse_from_xml_file(Path path) throws IOException {
        com.bw.fsm.Log.info("Reading " + path);
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            Fsm fsm = parse(path.getParent(), input);
            if (fsm.name == null)
                fsm.name = path.toString();
            return fsm;
        }
    }

    /**
     * Read and parse the FSM from a URL
     */
    public Fsm parse_from_url(URL url) throws IOException {
        long start;
        if (StaticOptions.debug_reader)
            start = System.currentTimeMillis();
        try {
            if ("file".equals(url.getProtocol())) {
                // We need to apply all include paths
                try {
                    Path file = includePaths.resolvePath(url.toURI().getSchemeSpecificPart());
                    return parse_from_xml_file(file);
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
            try (InputStream input = url.openStream()) {
                return parse(null, input);
            }
        } finally {
            if (StaticOptions.debug_reader)
                com.bw.fsm.Log.debug("'%s' loaded in %dms", url, System.currentTimeMillis() - start);

        }
    }

    /**
     * Reads the FSM from an XML String
     */
    public Fsm parse_from_xml(String xml) throws IOException {
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return parse(null, input);
        }
    }

    public Fsm parse(Path workingDir, InputStream input) throws IOException {
        try {
            StatefulReader statefulReader = new StatefulReader();
            statefulReader.include_paths.add(workingDir);
            statefulReader.include_paths.add(this.includePaths);
            XMLStreamReader reader = factory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        statefulReader.start_element(reader.getName().getLocalPart(),
                                new Attributes(reader), reader);
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        statefulReader.end_element(reader.getName().getLocalPart());
                        break;
                }
            }
            return statefulReader.fsm;
        } catch (XMLStreamException xs) {
            throw new IOException(xs);
        }
    }

    protected static class ReaderStackItem {
        State current_state;
        Transition current_transition;
        String current_tag;

        ReaderStackItem() {
            current_tag = "";
        }

        ReaderStackItem(ReaderStackItem o) {
            current_state = o.current_state;
            current_transition = o.current_transition;
            current_tag = o.current_tag;
        }
    }

    protected static class Attributes {

        Map<String, String> values = new HashMap<>();

        public Attributes(XMLStreamReader element) {
            int N = element.getAttributeCount();
            for (int i = 0; i < N; ++i) {
                values.put(element.getAttributeLocalName(i), element.getAttributeValue(i));
            }
        }

        public String getValue(String name) {
            return this.values.get(name);
        }
    }

    protected static class StatefulReader implements StaticOptions {

        // True if reader in inside a scxml element
        boolean in_scxml;
        int id_count;
        Path file;
        String content;
        // The resulting fsm
        Fsm fsm;
        ReaderStackItem current;
        Stack<ReaderStackItem> stack;

        protected static String strip_suffix(String v, String suffix) {
            if (v.endsWith(suffix)) {
                return v.substring(0, v.length() - suffix.length());
            } else {
                return null;
            }
        }

        static class ExecutableContentStackItem {
            /**
             * the element for which the item was pushed (that replaced current)
             */
            String for_tag;

            /**
             * the pushed block
             */
            ExecutableContentBlock block;

            ExecutableContentStackItem(String tag, ExecutableContentBlock block) {
                this.for_tag = tag;
                this.block = block;
            }

            @Override
            public String toString() {
                return "#" + for_tag + " " + block;
            }
        }

        Stack<ExecutableContentStackItem> executable_content_stack;
        ExecutableContentBlock current_executable_content;
        IncludePaths include_paths = new IncludePaths();

        public StatefulReader() {
            in_scxml = false;
            id_count = 0;
            stack = new Stack<>();
            executable_content_stack = new Stack<>();
            current_executable_content = null;
            current = new ReaderStackItem();

            fsm = new Fsm();
            file = Paths.get("Buffer");
            content = "";
        }

        protected void push(String tag) {
            this.stack.push(new ReaderStackItem(this.current));
            this.current.current_tag = tag;
        }

        protected void pop() {
            if (!this.stack.empty())
                this.current = this.stack.pop();
        }

        protected @NotNull String generate_name() {
            ++this.id_count;
            return String.format("__id%d", this.id_count);
        }

        protected @NotNull Data create_source(String src) {
            return new Data.Source(new SourceCode(
                    src,
                    SOURCE_ID_COUNTER.incrementAndGet()));
        }

        protected void parse_location_expressions(String location_expr, List<String> targets) {
            targets.addAll(List.of(split_whitespace.split(location_expr)));
        }

        protected void parse_state_specification(String target_name, List<State> targets) {
            Arrays.stream(split_whitespace.split(target_name)).forEach(
                    target -> targets.add(this.get_or_create_state(target, false)));
        }

        protected boolean parse_boolean(String value, boolean defaultVaLue) {
            return (value != null) ? "true".equalsIgnoreCase(value) : defaultVaLue;
        }

        protected State get_current_state() {
            State id = this.current.current_state;
            if (id == null) {
                com.bw.fsm.Log.panic("Internal error: Current State is unknown");
            }
            return id;
        }

        protected Transition get_current_transition() {
            Transition transition = this.current.current_transition;
            if (transition == null) {
                com.bw.fsm.Log.panic("Internal error: Current Transition is unknown");
            }
            return transition;
        }

        /**
         * Starts a new block of executable content.<br>
         * A stack is used to handle nested executable content.
         * This stack works independent of the main element stack, but should be
         * considered as synchronized with it.<br>
         * <b>Arguments</b>
         * <table><caption>Arguments</caption>
         * <tr><td>stack</td><td>If true, the current block is put on stack,
         *     continued after the matching {@link #end_executable_content_block(String)}.
         *     If false, the current stack is discarded.</td></tr>
         * <tr><td>tag</td><td>Tag for which this block was started. Used to mark the block for later clean-up.
         * </td></tr></table>
         */
        protected ExecutableContentBlock start_executable_content_block(boolean stack, String tag) {
            if (stack) {
                if (debug)
                    com.bw.fsm.Log.debug(" push executable content block [%s]", this.current_executable_content);
                this.executable_content_stack.push(new ExecutableContentStackItem(tag, this.current_executable_content));
            } else {
                this.executable_content_stack.clear();
            }
            this.current_executable_content = new ExecutableContentBlock((ExecutableContent) null, tag);
            if (debug)
                com.bw.fsm.Log.debug(" start executable content block [%s]", this.current_executable_content);
            return this.current_executable_content;
        }

        /// Get the last entry for the current content block.
        protected ExecutableContent get_last_executable_content_entry_for_block(ExecutableContentBlock ec_id) {
            if (ec_id == null || ec_id.content.isEmpty())
                return null;
            return ec_id.content.get(ec_id.content.size() - 1);
        }

        /**
         * Ends the current executable content block and returns the old block.
         * The current id is reset to 0 or popped from stack if the stack is not empty.
         * See {@link #start_executable_content_block(boolean, String)}.
         */
        protected ExecutableContentBlock end_executable_content_block(String tag) {
            if (this.current_executable_content == null) {
                com.bw.fsm.Log.panic("Try to get executable content in unsupported document part.");
                return null;
            } else {
                if (debug)
                    com.bw.fsm.Log.debug(" end executable content block [%s]", this.current_executable_content);
                ExecutableContentBlock ec = this.current_executable_content;
                ExecutableContentStackItem item = this.executable_content_stack.isEmpty() ? null : this.executable_content_stack.pop();
                if (item != null) {
                    this.current_executable_content = item.block;
                    if (debug)
                        com.bw.fsm.Log.debug(" pop executable content block [%s]", item);
                    if (this.current_executable_content != null) {
                        if (tag != null && !tag.equals(item.for_tag)) {
                            this.end_executable_content_block(tag);
                        }
                    }
                } else {
                    this.current_executable_content = null;
                }
                return ec;
            }
        }

        /**
         * Adds content to the current executable content block.
         */
        protected void add_executable_content(ExecutableContent ec) {
            if (this.current_executable_content == null) {
                com.bw.fsm.Log.panic("Try to add executable content to unsupported document part.");
            } else {
                if (StaticOptions.debug_reader)
                    com.bw.fsm.Log.debug(
                            "Adding Executable Content [%s] to [%s]",
                            ec,
                            this.current_executable_content
                    );
                this.current_executable_content.content.add(ec);
            }
        }

        protected String get_parent_tag() {
            String r = "";
            if (!this.stack.isEmpty()) {
                r = this.stack.peek().current_tag;
            }
            return r;
        }

        protected String verify_parent_tag(String name, String[] allowed_parents) {
            String parent_tag = this.get_parent_tag();
            if (!Arrays.asList(allowed_parents).contains(parent_tag)) {
                com.bw.fsm.Log.panic(
                        "<%s> inside <%s>. Only allowed inside %s",
                        name, parent_tag, String.join(",", allowed_parents)
                );
            }
            return parent_tag;
        }

        protected State get_or_create_state(String name, boolean parallel) {
            State state = this.fsm.statesNames.get(name);
            if (state == null) {
                state = new State(name);
                state.id = this.fsm.statesNames.size() + 1;
                state.is_parallel = parallel;
                this.fsm.statesNames.put(state.name, state);
            } else {
                if (parallel) {
                    state.is_parallel = true;
                }
            }
            return state;
        }

        protected State get_or_create_state_with_attributes(Attributes attr, boolean parallel, State parent) {
            String sname = attr.getValue(ATTR_ID);
            if (sname == null) {
                sname = this.generate_name();
            }
            State state = this.get_or_create_state(sname, parallel);

            String initialName = attr.getValue(ATTR_INITIAL);
            if (initialName != null) {
                // Create initial-transition with the initial states
                Transition initial = new Transition();
                initial.id = ID_COUNTER.incrementAndGet();
                initial.doc_id = DOC_ID_COUNTER.incrementAndGet();
                initial.transition_type = TransitionType.Internal;
                initial.source = state;
                initial.events = Collections.emptyList();
                this.parse_state_specification(initialName, initial.target);
                if (StaticOptions.debug_reader)
                    com.bw.fsm.Log.debug(
                            " %s.initial = %s -> %s",
                            sname, initialName,
                            initial);
                state.initial = initial;
            }

            state.doc_id = DOC_ID_COUNTER.incrementAndGet();

            if (parent != null) {
                state.parent = parent;
                if (debug)
                    com.bw.fsm.Log.debug(
                            " state #%s %s%s parent %s",
                            state, (parallel ? "(parallel) " : ""), sname, parent.name
                    );
                if (!parent.states.contains(state)) {
                    parent.states.add(state);
                }
            } else {
                if (debug)
                    com.bw.fsm.Log.debug(
                            " state #%s %s%s no parent",
                            state, (parallel ? "(parallel) " : ""), sname);
            }

            return state;
        }

        protected String get_required_attr(String tag, String attribute, Attributes attributes) {
            String attr = attributes.getValue(attribute);
            if (attr == null) {
                com.bw.fsm.Log.panic("<%s> requires attribute %s", tag, attribute);
            }
            return attr;
        }

        protected String read_from_uri(String uri) throws IOException {
            try {
                URI url_result = new URI(uri);
                if ("file".equals(url_result.getScheme())) {
                    return this.read_from_relative_path(url_result.getSchemeSpecificPart());
                }
                if (debug)
                    com.bw.fsm.Log.debug("read from URL %s", url_result);
                ByteArrayOutputStream sb = new ByteArrayOutputStream(1024);
                try (InputStream is = url_result.toURL().openStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        sb.write(buffer, 0, len);
                    }
                }
                return sb.toString(StandardCharsets.UTF_8);
            } catch (URISyntaxException syntaxException) {
                if (debug)
                    com.bw.fsm.Log.debug(
                            "%s is not a URI (%s). Try loading as relative path...",
                            uri, syntaxException.getMessage()
                    );
                return this.read_from_relative_path(uri);
            }
        }

        protected String read_from_relative_path(String path) throws IOException {
            Path file_src = include_paths.resolvePath(path);
            this.file = file_src;
            return Files.readString(file_src, StandardCharsets.UTF_8);
        }

        /**
         * A new "parallel" element started
         */
        protected State start_parallel(Attributes attr) {
            this.verify_parent_tag(TAG_PARALLEL, new String[]{TAG_SCXML, TAG_STATE, TAG_PARALLEL});
            State state = this.get_or_create_state_with_attributes(attr, true, this.current.current_state);
            this.current.current_state = state;
            return state;
        }

        /**
         * A new "final" element started
         */
        protected State start_final(Attributes attr) {
            this.verify_parent_tag(TAG_FINAL, new String[]{TAG_SCXML, TAG_STATE});
            State state = this.get_or_create_state_with_attributes(attr, false, this.current.current_state);
            state.is_final = true;
            this.current.current_state = state;
            return state;
        }

        /**
         * A new "donedata" element started
         */
        protected void start_donedata() {
            verify_parent_tag(TAG_DONEDATA, new String[]{TAG_FINAL});
            get_current_state().donedata = new DoneData();
        }

        /**
         * A new "history" element started
         */
        protected State start_history(Attributes attr) {
            this.verify_parent_tag(TAG_HISTORY, new String[]{TAG_STATE, TAG_PARALLEL});
            // Don't add history-states to "states" (parent = null)
            var hstate = get_or_create_state_with_attributes(attr, false, null);
            if (current.current_state != null) {
                var parent_state = get_current_state();
                parent_state.history.push(hstate);
            }
            // Assign parent manually, as we didn't provide get_or_create_state_with_attributes the parent.
            hstate.parent = current.current_state;

            String type_name = attr.getValue(TAG_TYPE);
            if (type_name != null) {
                hstate.history_type = HistoryType.fromString(type_name);
            } else {
                hstate.history_type = HistoryType.Shallow;
            }
            current.current_state = hstate;
            return hstate;
        }

        /**
         * A new "state" element started
         */
        protected State start_state(Attributes attr) {
            this.verify_parent_tag(TAG_STATE, new String[]{TAG_SCXML, TAG_STATE, TAG_PARALLEL});
            State state = this.get_or_create_state_with_attributes(attr, false, this.current.current_state);
            this.current.current_state = state;
            return state;
        }

        // A "datamodel" element started (node, not attribute)
        protected void start_datamodel() {
            this.verify_parent_tag(TAG_DATAMODEL, new String[]{TAG_SCXML, TAG_STATE, TAG_PARALLEL});
        }

        protected void start_data(Attributes attr, XMLStreamReader reader) throws IOException {
            this.verify_parent_tag(TAG_DATA, new String[]{TAG_DATAMODEL});

            String id = get_required_attr(TAG_DATA, ATTR_ID, attr);
            String src = attr.getValue(ATTR_SRC);

            String expr = attr.getValue(ATTR_EXPR);
            String content = this.read_content(TAG_DATA, reader);

            // W3C:
            // In a conformant SCXML document, a \<data}> element may have either a 'src' or an 'expr' attribute,
            // but must not have both. Furthermore, if either attribute is present, the element must not have any children.
            // Thus 'src', 'expr' and children are mutually exclusive in the <data> element.

            String data_value;
            if (src != null) {
                if (!(expr == null && content.isEmpty())) {
                    com.bw.fsm.Log.panic(
                            "%s shall have only %s, %s or children, but not some combination of it.",
                            TAG_DATA, ATTR_SRC, ATTR_EXPR
                    );
                }
                // W3C:
                // Gives the location from which the data object should be fetched.
                // If the 'src' attribute is present, the Platform must fetch the specified object
                // at the time specified by the 'binding' attribute of \<scxml\> and must assign it as
                // the value of the data element
                data_value = this.read_from_uri(src);
                if (data_value != null) {
                    if (StaticOptions.debug_reader)
                        com.bw.fsm.Log.debug("src='%s':\n%s", src, data_value);
                } else {
                    com.bw.fsm.Log.panic("Can't read data source '%s'", src);
                }
            } else if (expr != null) {
                if (!content.isEmpty()) {
                    com.bw.fsm.Log.panic(
                            "%s shall have only %s, %s or children, but not some combination of it.",
                            TAG_DATA, ATTR_SRC, ATTR_EXPR
                    );
                }
                data_value = expr;
            } else if (!content.isEmpty()) {
                data_value = content;
            } else {
                data_value = null;
            }
            this.get_current_state()
                    .data
                    .put(id, data_value == null ? Data.Null.NULL : this.create_source(data_value.trim()));
        }

        /// A "initial" element started (the element, not the attribute)
        protected void start_initial() {
            String parent_tag = this
                    .verify_parent_tag(
                            TAG_INITIAL,
                            new String[]{TAG_STATE, TAG_PARALLEL}
                    );
            if (get_current_state().initial != null) {
                com.bw.fsm.Log.panic(
                        "<%s> must not be specified if %s-attribute was given",
                        TAG_INITIAL, ATTR_INITIAL);
            }
        }

        protected void start_invoke(Attributes attr) {
            String parent_tag = this
                    .verify_parent_tag(
                            TAG_INVOKE,
                            new String[]{TAG_STATE, TAG_PARALLEL}
                    );

            var invoke = new Invoke();
            invoke.doc_id = DOC_ID_COUNTER.incrementAndGet();

            String type_opt = attr.getValue(ATTR_TYPE);
            if (type_opt != null) {
                invoke.type_name = create_source(type_opt);
            }
            String typeexpr = attr.getValue(ATTR_TYPEEXPR);
            if (typeexpr != null) {
                invoke.type_expr = create_source(typeexpr);
            }

            // W3c: Must not occur with the 'srcexpr' attribute or the <content> element.
            String src = attr.getValue(ATTR_SRC);
            if (src != null) {
                invoke.src = create_source(src);
            }
            String srcexpr = attr.getValue(ATTR_SRCEXPR);
            if (srcexpr != null) {
                invoke.src_expr = create_source(srcexpr);
            }

            // TODO--
            String id = attr.getValue(ATTR_ID);
            if (id != null) {
                invoke.invoke_id = id;
            }

            invoke.parent_state_name = get_current_state().name;

            String idlocation = attr.getValue(ATTR_IDLOCATION);
            if (idlocation != null) {
                invoke.external_id_location = idlocation;
            }

            String name_list = attr.getValue(ATTR_NAMELIST);
            if (name_list != null) {
                parse_location_expressions(name_list, invoke.name_list);
            }
            invoke.autoforward = parse_boolean(attr.getValue(ATTR_AUTOFORWARD), false);
            get_current_state().invoke.push(invoke);
        }

        protected void start_finalize(Attributes attr) {
            String parent_tag = this
                    .verify_parent_tag(
                            TAG_FINALIZE,
                            new String[]{TAG_INVOKE}
                    );
            start_executable_content_block(false, TAG_FINALIZE);
        }

        protected void end_finalize() {
            get_current_state().invoke.last().finalize = end_executable_content_block(TAG_FINALIZE);
        }

        protected void start_transition(Attributes attr) {
            String parent_tag = this
                    .verify_parent_tag(
                            TAG_TRANSITION,
                            new String[]{TAG_HISTORY, TAG_INITIAL, TAG_STATE, TAG_PARALLEL}
                    );

            Transition t = new Transition();
            t.doc_id = DOC_ID_COUNTER.incrementAndGet();

            // Start script.
            this.start_executable_content_block(false, TAG_TRANSITION);

            String event = attr.getValue(TAG_EVENT);
            if (event != null) {
                t.events = Arrays.stream(split_whitespace.split(event))
                        .map(s -> {
                            // Strip redundant "." and ".*" suffix
                            var rt = s;
                            var do_it = true;
                            while (do_it) {
                                do_it = false;
                                String r = strip_suffix(rt, ".*");
                                if (r != null) {
                                    do_it = true;
                                    rt = r;
                                }
                                r = strip_suffix(rt, ".");
                                if (r != null) {
                                    do_it = true;
                                    rt = r;
                                }
                            }
                            return rt;
                        })
                        .collect(Collectors.toList());
                t.wildcard = t.events.contains("*");
            } else {
                t.events = Collections.emptyList();
            }

            String cond = attr.getValue(ATTR_COND);
            if (cond != null) {
                t.cond = this.create_source(cond);
            }

            String target = attr.getValue(ATTR_TARGET);
            if (target != null) {
                this.parse_state_specification(target, t.target);
            }

            String trans_type = attr.getValue(TAG_TYPE);
            if (trans_type != null) {
                t.transition_type = TransitionType.map_transition_type(trans_type);
            }

            State state = this.get_current_state();

            if (TAG_INITIAL.equals(parent_tag)) {
                if (state.initial != null) {
                    com.bw.fsm.Log.panic("<initial> must not be specified if initial-attribute was given");
                }
                if (StaticOptions.debug_reader)
                    com.bw.fsm.Log.debug(" %s#%s.initial = %s", state.name, state.id, t);
                state.initial = t;
            } else {
                state.transitions.push(t);
            }
            t.source = state;
            this.current.current_transition = t;
        }

        protected void end_transition() {
            var ec = this.end_executable_content_block(TAG_TRANSITION);
            Transition trans = this.get_current_transition();
            // Assign the collected content to the transition.
            trans.content = ec;
        }

        protected void start_script(Attributes attr, XMLStreamReader reader) throws IOException {
            boolean at_root = TAG_SCXML.equals(this.get_parent_tag());
            if (!at_root) {
                this.verify_parent_tag(
                        TAG_SCRIPT,
                        new String[]{
                                TAG_SCXML,
                                TAG_TRANSITION,
                                TAG_ON_EXIT,
                                TAG_ON_ENTRY,
                                TAG_IF,
                                TAG_FOR_EACH,
                                TAG_FINALIZE}
                );
            }
            if (at_root) {
                this.start_executable_content_block(false, TAG_SCRIPT);
            }

            Expression s = new Expression();

            String file_src = attr.getValue(ATTR_SRC);
            if (file_src != null) {
                // W3C:
                // If the script can not be downloaded within a platform-specific timeout interval,
                // the document is considered non-conformant, and the platform must reject it.
                String source = this.read_from_uri(file_src);
                if (source != null) {
                    if (StaticOptions.debug_reader)
                        com.bw.fsm.Log.debug("src='%s':\n%s", file_src, source);
                    s.content = this.create_source(source);
                } else {
                    com.bw.fsm.Log.panic("Can't read script '%s'", file_src);
                }
            }

            String script_text = this.read_content(TAG_SCRIPT, reader);

            if (!script_text.isEmpty()) {
                if (!s.content.is_empty()) {
                    com.bw.fsm.Log.panic("<script> with 'src' attribute shall not have content.");
                }
                s.content = this.create_source(script_text);
            }

            this.add_executable_content(s);
            if (at_root) {
                this.fsm.script = this.end_executable_content_block(TAG_SCRIPT);
            }
        }

        protected void start_for_each(Attributes attr) {
            verify_parent_tag(
                    TAG_FOR_EACH,
                    new String[]{
                            TAG_ON_ENTRY,
                            TAG_ON_EXIT,
                            TAG_TRANSITION,
                            TAG_FOR_EACH,
                            TAG_IF,
                            TAG_FINALIZE}
            );

            var fe = new ForEach();
            fe.array = this.create_source(get_required_attr(TAG_FOR_EACH, ATTR_ARRAY, attr));
            fe.item = get_required_attr(TAG_FOR_EACH, ATTR_ITEM, attr);
            fe.index = attr.getValue(ATTR_INDEX);
            this.add_executable_content(fe);
            fe.content = this.start_executable_content_block(true, TAG_FOR_EACH);
        }

        protected void end_for_each() {
            this.end_executable_content_block(TAG_FOR_EACH);
        }

        protected void start_cancel(Attributes attr) {
            this.verify_parent_tag(
                    TAG_CANCEL,
                    new String[]{
                            TAG_TRANSITION,
                            TAG_ON_EXIT,
                            TAG_ON_ENTRY,
                            TAG_IF,
                            TAG_FOR_EACH,
                    }
            );

            String sendid = attr.getValue(ATTR_SENDID);
            String sendidexpr = attr.getValue(ATTR_SENDIDEXPR);

            Cancel cancel = new Cancel();

            if (sendid != null) {
                if (sendidexpr != null) {
                    com.bw.fsm.Log.panic(
                            "%s: attributes %s and %s must not occur both", TAG_CANCEL, ATTR_SENDID, ATTR_SENDIDEXPR);
                }
                cancel.send_id = sendid;
            } else if (sendidexpr != null) {
                cancel.send_id_expr = create_source(sendidexpr);
            } else {
                com.bw.fsm.Log.panic("%s: attribute %s or %s must be given", TAG_CANCEL, ATTR_SENDID, ATTR_SENDIDEXPR);
            }
            add_executable_content(cancel);
        }

        protected void start_on_entry(Attributes _attr) {
            this.verify_parent_tag(TAG_ON_ENTRY, new String[]{TAG_STATE, TAG_PARALLEL, TAG_FINAL});
            this.start_executable_content_block(false, TAG_ON_ENTRY);
        }

        protected void end_on_entry() {
            ExecutableContentBlock ec_block = this.end_executable_content_block(TAG_ON_ENTRY);
            State state = this.get_current_state();
            state.onentry.add(ec_block);
        }

        protected void start_on_exit(Attributes _attr) {
            this.verify_parent_tag(TAG_ON_EXIT, new String[]{TAG_STATE, TAG_PARALLEL, TAG_FINAL});
            this.start_executable_content_block(false, TAG_ON_EXIT);
        }

        protected void end_on_exit() {
            ExecutableContentBlock ec_block = this.end_executable_content_block(TAG_ON_EXIT);
            // Add the collected content to the on-exit.
            this.get_current_state().onexit.add(ec_block);
        }

        protected void start_if(Attributes attr) {
            this.verify_parent_tag(
                    TAG_IF,
                    new String[]{
                            TAG_ON_ENTRY,
                            TAG_ON_EXIT,
                            TAG_TRANSITION,
                            TAG_FOR_EACH,
                            TAG_IF,
                            TAG_FINALIZE
                    }
            );

            If ec_if = new If(this.create_source(this.get_required_attr(TAG_IF, ATTR_COND, attr)));
            this.add_executable_content(ec_if);
            ExecutableContentBlock parent_content_id = this.current_executable_content;

            this.start_executable_content_block(true, TAG_IF);
            ExecutableContentBlock if_cid = this.current_executable_content;

            ExecutableContent if_ec = this.get_last_executable_content_entry_for_block(parent_content_id);
            if (if_ec instanceof If evc_if) {
                evc_if.content = if_cid;
            } else {
                com.bw.fsm.Log.panic(
                        "Internal Error: Executable Content missing in start_if in block #%s",
                        parent_content_id
                );
            }
        }

        protected void end_if() {
            this.end_executable_content_block(TAG_IF);
        }

        protected void start_else_if(Attributes attr) {
            this.verify_parent_tag(TAG_ELSEIF, new String[]{TAG_IF});

            // Close parent <if> content block
            this.end_executable_content_block(TAG_IF);

            ExecutableContentBlock if_id = this.current_executable_content;

            // Start new "else" block - will contain only one "if", replacing current "if" stack element.
            this.start_executable_content_block(true, TAG_IF);
            ExecutableContentBlock else_id = this.current_executable_content;

            // Add new "if"
            If else_if = new If(this.create_source(get_required_attr(TAG_IF, ATTR_COND, attr)));
            this.add_executable_content(else_if);

            ExecutableContentBlock else_if_content_id = this.start_executable_content_block(true, TAG_ELSEIF);

            // Put together
            ExecutableContent else_if_ec = this.get_last_executable_content_entry_for_block(else_id);
            if (else_if_ec instanceof If evc_if) {
                evc_if.content = else_if_content_id;
            } else {
                com.bw.fsm.Log.panic(
                        "Internal Error: Executable Content missing in start_else_if in block #%s",
                        else_id
                );
            }

            while (if_id != null) {
                // Find matching "if" level for the new "else if"
                ExecutableContent if_ec = this.get_last_executable_content_entry_for_block(if_id);
                if (if_ec instanceof If evc_if) {
                    if (evc_if.else_content != null) {
                        // Some higher "if". Go inside else-block.
                        if_id = evc_if.else_content;
                    } else {
                        // Match, set "else-block".
                        if_id = null;
                        evc_if.else_content = else_id;
                    }
                } else {
                    com.bw.fsm.Log.panic("Internal Error: Executable Content missing in start_else_if");
                }
            }

        }

        protected void start_else(Attributes _attr) {
            this.verify_parent_tag(TAG_ELSE, new String[]{TAG_IF});

            // Close parent <if> content block
            this.end_executable_content_block(TAG_IF);

            ExecutableContentBlock if_id = this.current_executable_content;

            // Start new "else" block, replacing "If" block.
            ExecutableContentBlock else_id = this.start_executable_content_block(true, TAG_IF);

            // Put together. Set deepest else
            while (if_id != null) {
                ExecutableContent if_ec = this.get_last_executable_content_entry_for_block(if_id);
                if (if_ec instanceof If evc_if) {

                    if (evc_if.else_content != null) {
                        if_id = evc_if.else_content;
                    } else {
                        if_id = null;
                        evc_if.else_content = else_id;
                    }
                } else {
                    com.bw.fsm.Log.panic("Internal Error: Executable Content missing in start_else");
                }
            }
        }

        protected void start_send(Attributes attr) {
            this.verify_parent_tag(
                    TAG_SEND,
                    new String[]{
                            TAG_TRANSITION,
                            TAG_ON_EXIT,
                            TAG_ON_ENTRY,
                            TAG_IF,
                            TAG_FOR_EACH
                    }
            );
            var send_params = new SendParameters();

            String eventName = attr.getValue(ATTR_EVENT);
            String eventexpr = attr.getValue(ATTR_EVENTEXPR);

            if (eventName != null) {
                if (eventexpr != null) {
                    com.bw.fsm.Log.panic("%s: attributes %s and %s must not occur both",
                            TAG_SEND, ATTR_EVENT, ATTR_EVENTEXPR);
                }
                send_params.event = this.create_source(eventName);
            } else if (eventexpr != null) {
                send_params.event_expr = this.create_source(eventexpr);
            }

            String target = attr.getValue(ATTR_TARGET);
            String targetexpr = attr.getValue(ATTR_TARGETEXPR);
            if (target != null) {
                if (targetexpr != null) {
                    com.bw.fsm.Log.panic("%s: attributes %s and %s must not occur both",
                            TAG_SEND, ATTR_TARGET, ATTR_TARGETEXPR);
                }
                send_params.target = this.create_source(target);
            } else if (targetexpr != null) {
                send_params.target_expr = this.create_source(targetexpr);
            }

            String type_attr = attr.getValue(ATTR_TYPE);
            String typeexpr = attr.getValue(ATTR_TYPEEXPR);
            if (type_attr != null) {
                if (typeexpr != null) {
                    com.bw.fsm.Log.panic("%s: attributes %s and %s must not occur both",
                            TAG_SEND, ATTR_TYPE, ATTR_TYPEEXPR);
                }
                send_params.type_value = this.create_source(type_attr);
            } else if (typeexpr != null) {
                send_params.type_expr = this.create_source(typeexpr);
            }

            String id = attr.getValue(ATTR_ID);
            String idlocation = attr.getValue(ATTR_IDLOCATION);
            if (id != null) {
                if (idlocation != null) {
                    com.bw.fsm.Log.panic("%s: attributes %s and %s must not occur both", TAG_SEND, ATTR_ID, ATTR_IDLOCATION);
                }
                send_params.name = id;
            } else if (idlocation != null) {
                send_params.name_location = idlocation;
            }

            String delay_attr = attr.getValue(ATTR_DELAY);
            String delay_expr_attr = attr.getValue(ATTR_DELAYEXPR);

            if (delay_expr_attr != null) {
                if (delay_attr != null) {
                    com.bw.fsm.Log.panic("%s: attributes %s and %s must not occur both",
                            TAG_SEND, ATTR_DELAY, ATTR_DELAYEXPR);
                }
                send_params.delay_expr = this.create_source(delay_expr_attr);
            } else if (delay_attr != null) {
                if ((!delay_attr.isEmpty()) && TARGET_INTERNAL.equals(type_attr)) {
                    com.bw.fsm.Log.panic(
                            "%s: %s with %s %s is not possible",
                            TAG_SEND,
                            ATTR_DELAY,
                            ATTR_TARGET,
                            type_attr
                    );
                }
                int delayms = ExecutableContent.parse_duration_to_milliseconds(delay_attr);
                if (delayms < 0) {
                    com.bw.fsm.Log.panic(
                            "%s: %s with illegal value '%s'",
                            TAG_SEND,
                            ATTR_DELAY,
                            delay_attr
                    );
                } else {
                    send_params.delay_ms = delayms;
                }
            }

            String name_list = attr.getValue(ATTR_NAMELIST);
            if (name_list != null) {
                this.parse_location_expressions(name_list, send_params.name_list);
            }
            send_params.parent_state_name = this.get_current_state().name;
            this.add_executable_content(send_params);

        }

        private final static List<StringWriter> stringWriters = new ArrayList<>(10);

        private static StringWriter getWriter() {
            synchronized (stringWriters) {
                if (stringWriters.isEmpty())
                    stringWriters.add(new StringWriter());
                return stringWriters.remove(stringWriters.size() - 1);
            }
        }

        private static void releaseWriter(StringWriter w) {
            // reset length, keeping capacity
            w.getBuffer().setLength(0);
            synchronized (stringWriters) {
                stringWriters.add(w);
            }
        }

        /**
         * Reads the content of the current element.
         * Calls "pop" to remove the element from stack.
         */
        protected @NotNull String read_content(String tag, XMLStreamReader reader) throws IOException {

            StringWriter writer = getWriter();
            try {
                int depth = 1;
                XMLStreamWriter xmlWriter = outputFactory.createXMLStreamWriter(writer);

                outer_while:
                while (reader.hasNext()) {
                    int event = reader.next();

                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            depth++;

                            String namespaceURI = reader.getNamespaceURI();
                            String localName = reader.getLocalName();

                            if (namespaceURI != null) {
                                xmlWriter.writeStartElement(reader.getPrefix(), localName, namespaceURI);
                            } else {
                                xmlWriter.writeStartElement(localName);
                            }

                            // Namespace declarations
                            for (int i = 0; i < reader.getNamespaceCount(); i++) {
                                String nsPrefix = reader.getNamespacePrefix(i);
                                String nsURI = reader.getNamespaceURI(i);
                                xmlWriter.writeNamespace(nsPrefix, nsURI);
                            }

                            // Attribute declarations
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                String attrNamespace = reader.getAttributeNamespace(i);
                                String attrLocalName = reader.getAttributeLocalName(i);
                                String attrValue = reader.getAttributeValue(i);

                                if (attrNamespace != null) {
                                    xmlWriter.writeAttribute(reader.getAttributePrefix(i), attrNamespace, attrLocalName, attrValue);
                                } else {
                                    xmlWriter.writeAttribute(attrLocalName, attrValue);
                                }
                            }
                            break;

                        case XMLStreamConstants.CHARACTERS:
                            if (!reader.isWhiteSpace()) {
                                xmlWriter.writeCharacters(reader.getText());
                            }

                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            depth--;
                            if (depth == 0) {
                                pop();
                                break outer_while;
                            }
                            xmlWriter.writeEndElement();
                            break;
                    }
                }
                xmlWriter.flush();
                xmlWriter.close();
                reader.close();

                return writer.toString().trim();
            } catch (XMLStreamException e) {
                throw new IOException(e);
            } finally {
                releaseWriter(writer);
            }

        }

        protected void start_content(Attributes attr, XMLStreamReader reader) throws IOException {
            var parent_tag = this.verify_parent_tag(TAG_CONTENT, new String[]{TAG_SEND, TAG_INVOKE, TAG_DONEDATA});
            var expr = attr.getValue(ATTR_EXPR);
            var content = read_content(TAG_CONTENT, reader);

            // W3C:
            // A conformant SCXML document must not specify both the 'expr' attribute and child content.
            if (expr != null && !content.isEmpty()) {
                com.bw.fsm.Log.panic("<%s> shall have only %s or children, but not both.", TAG_CONTENT, ATTR_EXPR);
            }
            Data contentData;
            if (content.isEmpty())
                contentData = null;
            else {
                try {
                    double d = Double.parseDouble(content);
                    if (d == Math.floor(d)) {
                        contentData = new Data.Integer((int) d);
                    } else {
                        contentData = new Data.Double((int) d);
                    }
                } catch (NumberFormatException ne) {

                    switch (parent_tag) {
                        case TAG_DONEDATA -> contentData = new Data.String(content);
                        case TAG_SEND, TAG_INVOKE -> {
                            ScxmlReader r = new ScxmlReader().withIncludePaths(this.include_paths);
                            Fsm fsm = r.parse_from_xml(content);
                            contentData = new Data.FsmDefinition(content, fsm);
                        }
                        default -> // Can't happen
                                contentData = Data.None.NONE;
                    }
                }
            }

            switch (parent_tag) {
                case TAG_DONEDATA -> {
                    var state = get_current_state();
                    if (state.donedata != null) {
                        state.donedata.content = new CommonContent(contentData, expr);
                    } else {
                        com.bw.fsm.Log.panic("Internal Error: donedata-Option not initialized");
                    }
                }
                case TAG_INVOKE -> {
                    var state = get_current_state();
                    var invoke = state.invoke.last();

                    invoke.content = new CommonContent(contentData, expr);
                }
                case TAG_SEND -> {
                    var ec = get_last_executable_content_entry_for_block(current_executable_content);
                    if (ec != null) {
                        if (ec instanceof SendParameters send) {
                            if (expr != null || contentData != null) {
                                send.content = new CommonContent(contentData, expr);
                            }
                        }
                    }
                }
                default -> com.bw.fsm.Log.panic("Internal Error: invalid parent-tag <%s> in start_content", parent_tag);
            }
        }

        protected void start_param(Attributes attr) {
            this.verify_parent_tag(TAG_PARAM, new String[]{TAG_SEND, TAG_INVOKE, TAG_DONEDATA});

            var parent_tag = get_parent_tag();
            var param = new Parameter();

            param.name = get_required_attr(TAG_PARAM, ATTR_NAME, attr);
            param.expr = attr.getValue(ATTR_EXPR);

            param.location = attr.getValue(ATTR_LOCATION);
            if (param.location != null && param.expr != null) {
                com.bw.fsm.Log.panic("%s shall have only %s or %s, but not both.", TAG_PARAM, ATTR_LOCATION, ATTR_EXPR);
            }

            switch (parent_tag) {
                case TAG_SEND -> {
                    var ec = get_last_executable_content_entry_for_block(current_executable_content);
                    if (ec instanceof SendParameters send) {
                        send.push_param(param);
                    } else {
                        com.bw.fsm.Log.panic("Internal Error: unexpected type of executable content");
                    }
                }
                case TAG_INVOKE -> get_current_state().invoke.last().push_param(param);
                case TAG_DONEDATA -> {
                    var state = get_current_state();
                    if (state.donedata != null) {
                        state.donedata.push_param(param);
                    } else {
                        com.bw.fsm.Log.panic("Internal Error: donedata-Option not initialized");
                    }
                }
                default -> com.bw.fsm.Log.panic("Internal Error: invalid parent-tag <%s> in start_param", parent_tag);
            }
        }

        protected void start_log(Attributes attr) {
            this.verify_parent_tag(
                    TAG_LOG,
                    new String[]{
                            TAG_TRANSITION,
                            TAG_ON_EXIT,
                            TAG_ON_ENTRY,
                            TAG_IF,
                            TAG_FOR_EACH,
                            TAG_FINALIZE
                    }
            );
            String label = attr.getValue(ATTR_LABEL);
            String expr = attr.getValue(ATTR_EXPR);
            if (expr != null) {
                var expression = this.create_source(expr);
                this.add_executable_content(new Log(label, expression));
            }
        }

        protected void start_assign(Attributes attr, XMLStreamReader reader) throws IOException {
            this.verify_parent_tag(
                    TAG_ASSIGN,
                    new String[]{
                            TAG_TRANSITION,
                            TAG_ON_EXIT,
                            TAG_ON_ENTRY,
                            TAG_IF,
                            TAG_FOR_EACH,
                            TAG_FINALIZE
                    }
            );

            Assign assign = new Assign();
            assign.location = this.create_source(get_required_attr(TAG_ASSIGN, ATTR_LOCATION, attr));

            String expr = attr.getValue(ATTR_EXPR);
            if (expr != null) {
                assign.expr = this.create_source(expr);
            }

            String assign_text = this.read_content(TAG_ASSIGN, reader).trim();
            if (!assign_text.isEmpty()) {
                if (!assign.expr.is_empty()) {
                    com.bw.fsm.Log.panic("<assign> with 'expr' attribute shall not have content.");
                }
                assign.expr = new Data.String(assign_text);
            }

            this.add_executable_content(assign);
        }

        protected void start_raise(Attributes attr) {
            this.verify_parent_tag(
                    TAG_RAISE,
                    new String[]{
                            TAG_TRANSITION,
                            TAG_ON_EXIT,
                            TAG_ON_ENTRY,
                            TAG_IF,
                            TAG_FOR_EACH,
                    }
            );
            var raise = new Raise();
            raise.event = this.get_required_attr(TAG_RAISE, ATTR_EVENT, attr);
            this.add_executable_content(raise);

        }

        protected void start_scxml(Attributes attr) {

            if (this.in_scxml) {
                com.bw.fsm.Log.panic("Only one <%s> allowed", TAG_SCXML);
            }
            this.in_scxml = true;
            String name = attr.getValue(ATTR_NAME);
            if (name != null)
                this.fsm.name = name;
            else {
                // @TODO: Filename?
            }
            String datamodel = attr.getValue(ATTR_DATAMODEL);
            if (datamodel != null) {
                if (StaticOptions.debug_reader)
                    com.bw.fsm.Log.debug(" scxml.datamodel = %s", datamodel);
                this.fsm.datamodel = datamodel;
            }
            String binding = attr.getValue(ATTR_BINDING);
            if (binding != null) {
                try {
                    this.fsm.binding = BindingType.fromString(binding);
                } catch (IllegalArgumentException iae) {
                    com.bw.fsm.Log.panic("%s: unsupported value %s", ATTR_BINDING, binding);
                }
            }
            String version = attr.getValue(TAG_VERSION);
            if (version != null) {
                this.fsm.version = version;
                if (StaticOptions.debug_reader)
                    com.bw.fsm.Log.debug(" scxml.version = %s", version);

            }
            this.fsm.pseudo_root = this.get_or_create_state_with_attributes(attr, false, null);
            this.current.current_state = this.fsm.pseudo_root;
        }

        protected void end_scxml() {
            this.set_default_initial(this.fsm.pseudo_root);
        }

        protected void set_default_initial(State state) {
            if (state.initial == null) {
                //  W3C: If not specified, the default initial state is the first child state in document order.
                if (!state.states.isEmpty()) {
                    Transition t = new Transition();
                    state.initial = t;
                    t.source = state;
                    t.target.add(state.states.get(0));
                }
            }
        }

        protected void end_state() {
            //  W3C: If not specified, the default initial state is the first child state in document order.
            this.set_default_initial(this.current.current_state);
        }


        public void start_element(String name, Attributes attr, XMLStreamReader reader) throws IOException {
            this.push(name);

            switch (name) {
                case TAG_INCLUDE -> this.include(attr);
                case TAG_SCXML -> this.start_scxml(attr);
                case TAG_DATAMODEL -> this.start_datamodel();
                case TAG_DATA -> this.start_data(attr, reader);
                case TAG_STATE -> this.start_state(attr);
                case TAG_PARALLEL -> this.start_parallel(attr);
                case TAG_FINAL -> this.start_final(attr);
                case TAG_DONEDATA -> this.start_donedata();
                case TAG_HISTORY -> this.start_history(attr);
                case TAG_INITIAL -> this.start_initial();
                case TAG_INVOKE -> this.start_invoke(attr);
                case TAG_TRANSITION -> this.start_transition(attr);
                case TAG_FINALIZE -> this.start_finalize(attr);
                case TAG_ON_ENTRY -> this.start_on_entry(attr);
                case TAG_ON_EXIT -> this.start_on_exit(attr);
                case TAG_SCRIPT -> this.start_script(attr, reader);
                case TAG_RAISE -> this.start_raise(attr);
                case TAG_SEND -> this.start_send(attr);
                case TAG_PARAM -> this.start_param(attr);
                case TAG_CONTENT -> this.start_content(attr, reader);
                case TAG_LOG -> this.start_log(attr);
                case TAG_ASSIGN -> this.start_assign(attr, reader);
                case TAG_FOR_EACH -> this.start_for_each(attr);
                case TAG_CANCEL -> this.start_cancel(attr);
                case TAG_IF -> this.start_if(attr);
                case TAG_ELSE -> this.start_else(attr);
                case TAG_ELSEIF -> this.start_else_if(attr);
                default -> {
                    if (debug)
                        com.bw.fsm.Log.debug("Ignored tag %s", name);
                }
            }
        }


        /**
         * Handle a XInclude include element.
         * See <a href="https://www.w3.org/TR/xinclude/">xinclude</a>
         * Only parse="text" and "href" with a relative path are supported, also no "xpointer" etc.
         */
        protected void include(Attributes attr) {
            throw new UnsupportedOperationException();
        }

        /**
         * Called from SAX handler if some end-tag was read.
         */
        public void end_element(String name) {
            if (!this.current.current_tag.equals(name)) {
                com.bw.fsm.Log.panic(
                        "Illegal end-tag %s, expected %s", name, this.current.current_tag);
            }
            if (StaticOptions.debug_reader)
                com.bw.fsm.Log.debug("End Element %s", name);
            switch (name) {
                case TAG_SCXML -> this.end_scxml();
                case TAG_IF -> this.end_if();
                case TAG_TRANSITION -> this.end_transition();
                case TAG_ON_EXIT -> this.end_on_exit();
                case TAG_ON_ENTRY -> this.end_on_entry();
                case TAG_FOR_EACH -> this.end_for_each();
                case TAG_FINALIZE -> this.end_finalize();
                case TAG_STATE -> this.end_state();
                default -> {
                }
            }
            this.pop();
        }
    }

    public ScxmlReader() {
    }

    public ScxmlReader withIncludePaths(IncludePaths includePaths) {
        this.includePaths.add(includePaths);
        return this;
    }
}
