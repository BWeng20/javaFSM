package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;
import com.bw.fsm.executable_content.*;
import com.bw.fsm.executable_content.Log;

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
    static AtomicInteger DOC_ID_COUNTER = new AtomicInteger(1);
    static AtomicInteger SOURCE_ID_COUNTER = new AtomicInteger(1);
    static Pattern split_whitespace = Pattern.compile("\\s");

    static XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    static XMLInputFactory factory = XMLInputFactory.newInstance();

    List<Path> includePaths = new ArrayList<>();

    /**
     * Read and parse the FSM from an XML file
     */
    public Fsm parse_from_xml_file(Path path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            return parse(input);
        }
    }

    /**
     * Read and parse the FSM from a URL
     */
    public Fsm parse_from_url(URL url) throws IOException {
        try (InputStream input = url.openStream()) {
            return parse(input);
        }
    }

    /**
     * Reads the FSM from an XML String
     */
    public Fsm parse_from_xml(String xml) throws IOException {
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return parse(input);
        }
    }

    public Fsm parse(InputStream input) throws IOException {
        try {
            StatefulReader statefulReader = new StatefulReader();
            XMLStreamReader reader = factory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        statefulReader.start_element(reader.getName().getLocalPart(),
                                new Attributes(reader), reader, true);
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
             * the pushed region
             */
            ExecutableContentRegion region;

            ExecutableContentStackItem(String tag, ExecutableContentRegion region) {
                this.for_tag = tag;
                this.region = region;
            }

            @Override
            public String toString() {
                return "#" + for_tag + " " + region;
            }
        }

        Stack<ExecutableContentStackItem> executable_content_stack;
        ExecutableContentRegion current_executable_content;
        List<Path> include_paths;

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
            include_paths = new ArrayList<>();
        }

        /**
         * Process an XML file.<br>
         * For technical reasons (to handle user content) the file is read in a temporary buffer.
         */
        public void process_file(Path file) {
            throw new UnsupportedOperationException();
        }

        /// Process all events from current content
        public void process() throws IOException {
            throw new UnsupportedOperationException();
        }

        protected void push(String tag) {
            this.stack.push(new ReaderStackItem(this.current));
            this.current.current_tag = tag;
        }

        protected void pop() {
            if (!this.stack.empty())
                this.current = this.stack.pop();
        }

        protected String generate_name() {
            ++this.id_count;
            return String.format("__id%d", this.id_count);
        }

        protected Data create_source(String src) {
            return new Data.Source(new SourceCode(
                    src,
                    SOURCE_ID_COUNTER.incrementAndGet()));
        }

        protected Data create_source_moved(String src) {
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
         * Starts a new region of executable content.<br>
         * A stack is used to handle nested executable content.
         * This stack works independent of the main element stack, but should be
         * considered as synchronized with it.<br>
         * <b>Arguments</b>
         * <table><caption>Arguments</caption>
         * <tr><td>stack</td><td>If true, the current region is put on stack,
         *     continued after the matching {@link #end_executable_content_region(String)}.
         *     If false, the current stack is discarded.</td></tr>
         * <tr><td>tag</td><td>Tag for which this region was started. Used to mark the region for later clean-up.
         * </td></tr></table>
         */
        protected ExecutableContentRegion start_executable_content_region(boolean stack, String tag) {
            if (stack) {
                if (debug_option)
                    com.bw.fsm.Log.debug(" push executable content region [%s]", this.current_executable_content);
                this.executable_content_stack.push(new ExecutableContentStackItem(tag, this.current_executable_content));
            } else {
                this.executable_content_stack.clear();
            }
            this.current_executable_content = new ExecutableContentRegion(null, tag);
            if (debug_option)
                com.bw.fsm.Log.debug(" start executable content region [%s]", this.current_executable_content);
            return this.current_executable_content;
        }

        /// Get the last entry for the current content region.
        protected ExecutableContent get_last_executable_content_entry_for_region(ExecutableContentRegion ec_id) {
            if (ec_id == null || ec_id.content.isEmpty())
                return null;
            return ec_id.content.get(ec_id.content.size() - 1);
        }

        /**
         * Ends the current executable content region and returns the old region.
         * The current id is reset to 0 or popped from stack if the stack is not empty.
         * See {@link #start_executable_content_region(boolean, String)}.
         */
        protected ExecutableContentRegion end_executable_content_region(String tag) {
            if (this.current_executable_content == null) {
                com.bw.fsm.Log.panic("Try to get executable content in unsupported document part.");
                return null;
            } else {
                if (debug_option)
                    com.bw.fsm.Log.debug(" end executable content region [%s]", this.current_executable_content);
                ExecutableContentRegion ec = this.current_executable_content;
                ExecutableContentStackItem item = this.executable_content_stack.isEmpty() ? null : this.executable_content_stack.pop();
                if (item != null) {
                    this.current_executable_content = item.region;
                    if (debug_option)
                        com.bw.fsm.Log.debug(" pop executable content region [%s]", item);
                    if (this.current_executable_content != null) {
                        if (tag != null && !tag.equals(item.for_tag)) {
                            this.end_executable_content_region(tag);
                        }
                    }
                } else {
                    this.current_executable_content = null;
                }
                return ec;
            }
        }

        /**
         * Adds content to the current executable content region.
         */
        protected void add_executable_content(ExecutableContent ec) {
            if (this.current_executable_content == null) {
                com.bw.fsm.Log.panic("Try to add executable content to unsupported document part.");
            } else {
                if (StaticOptions.debug_option)
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
                initial.doc_id = DOC_ID_COUNTER.incrementAndGet();
                initial.transition_type = TransitionType.Internal;
                initial.source = state;
                this.parse_state_specification(initialName, initial.target);
                if (StaticOptions.debug_option)
                    com.bw.fsm.Log.debug(
                            " %s.initial = %s -> %s",
                            sname, initialName,
                            initial);
                state.initial = initial;
            }

            state.doc_id = DOC_ID_COUNTER.incrementAndGet();

            if (parent != null) {
                state.parent = parent;
                if (debug_option)
                    com.bw.fsm.Log.debug(
                            " state #%s %s%s parent %s",
                            state, (parallel ? "(parallel) " : ""), sname, parent.name
                    );
                if (!parent.states.contains(state)) {
                    parent.states.add(state);
                }
            } else {
                if (debug_option)
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
                    return this.read_from_relative_path(url_result.getPath());
                }
                if (debug_option)
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
                if (debug_option)
                    com.bw.fsm.Log.debug(
                            "%s is not a URI (%s). Try loading as relative path...",
                            uri, syntaxException.getMessage()
                    );
                return this.read_from_relative_path(uri);
            }
        }

        protected String read_from_relative_path(String path) throws IOException {
            Path file_src = this.get_resolved_path(path);
            this.file = file_src;
            return Files.readString(file_src, StandardCharsets.UTF_8);
        }

        /// A new "parallel" element started
        protected State start_parallel(Attributes attr) {
            this.verify_parent_tag(TAG_PARALLEL, new String[]{TAG_SCXML, TAG_STATE, TAG_PARALLEL});
            State state = this.get_or_create_state_with_attributes(attr, true, this.current.current_state);
            this.current.current_state = state;
            return state;
        }

        /// A new "final" element started
        protected State start_final(Attributes attr) {
            this.verify_parent_tag(TAG_FINAL, new String[]{TAG_SCXML, TAG_STATE});
            State state = this.get_or_create_state_with_attributes(attr, false, this.current.current_state);
            state.is_final = true;
            this.current.current_state = state;
            return state;
        }

        /// A new "donedata" element started
        protected void start_donedata() {
            throw new UnsupportedOperationException();
        }

        /// A new "history" element started
        protected State start_history(Attributes attr) {
            throw new UnsupportedOperationException();
        }

        // A new "state" element started
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

        protected void start_data(Attributes attr, XMLStreamReader reader, boolean has_content) throws IOException {
            this.verify_parent_tag(TAG_DATA, new String[]{TAG_DATAMODEL});

            String id = get_required_attr(TAG_DATA, ATTR_ID, attr);
            String src = attr.getValue(ATTR_SRC);

            String expr = attr.getValue(ATTR_EXPR);

            String content = (has_content) ? this.read_content(TAG_DATA, reader) : "";

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
                    if (StaticOptions.debug_option)
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
            throw new UnsupportedOperationException();
        }

        protected void start_invoke(Attributes attr) {
            throw new UnsupportedOperationException();
        }

        protected void start_finalize(Attributes attr) {
            throw new UnsupportedOperationException();
        }

        protected void end_finalize() {
            throw new UnsupportedOperationException();
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
            this.start_executable_content_region(false, TAG_TRANSITION);

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
                if (StaticOptions.debug_option)
                    com.bw.fsm.Log.debug(" %s#%s.initial = %s", state.name, state.id, t);
                state.initial = t;
            } else {
                state.transitions.push(t);
            }
            t.source = state;
            this.current.current_transition = t;
        }

        protected void end_transition() {
            var ec = this.end_executable_content_region(TAG_TRANSITION);
            Transition trans = this.get_current_transition();
            // Assign the collected content to the transition.
            trans.content = ec;
        }

        protected void start_script(Attributes attr, XMLStreamReader reader, boolean has_content) throws IOException {
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
                this.start_executable_content_region(false, TAG_SCRIPT);
            }

            Expression s = new Expression();

            String file_src = attr.getValue(ATTR_SRC);
            if (file_src != null) {
                // W3C:
                // If the script can not be downloaded within a platform-specific timeout interval,
                // the document is considered non-conformant, and the platform must reject it.
                String source = this.read_from_uri(file_src);
                if (source != null) {
                    if (StaticOptions.debug_option)
                        com.bw.fsm.Log.debug("src='%s':\n%s", file_src, source);
                    s.content = this.create_source_moved(source);
                } else {
                    com.bw.fsm.Log.panic("Can't read script '%s'", file_src);
                }
            }

            String script_text = (has_content) ?
                    this.read_content(TAG_SCRIPT, reader).trim() : "";

            if (!script_text.isEmpty()) {
                if (!s.content.is_empty()) {
                    com.bw.fsm.Log.panic("<script> with 'src' attribute shall not have content.");
                }
                s.content = this.create_source_moved(file_src);
            }

            this.add_executable_content(s);
            if (at_root) {
                this.fsm.script = this.end_executable_content_region(TAG_SCRIPT);
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
            fe.content = this.start_executable_content_region(true, TAG_FOR_EACH);
        }

        protected void end_for_each() {
            this.end_executable_content_region(TAG_FOR_EACH);
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
            throw new UnsupportedOperationException();

        }

        protected void start_on_entry(Attributes _attr) {
            this.verify_parent_tag(TAG_ON_ENTRY, new String[]{TAG_STATE, TAG_PARALLEL, TAG_FINAL});
            this.start_executable_content_region(false, TAG_ON_ENTRY);
        }

        protected void end_on_entry() {
            ExecutableContentRegion ec_id = this.end_executable_content_region(TAG_ON_ENTRY);
            State state = this.get_current_state();
            // Add the collected content to on-entry.
            state.onentry.addAll(ec_id.content);
        }

        protected void start_on_exit(Attributes _attr) {
            this.verify_parent_tag(TAG_ON_EXIT, new String[]{TAG_STATE, TAG_PARALLEL, TAG_FINAL});
            this.start_executable_content_region(false, TAG_ON_EXIT);
        }

        protected void end_on_exit() {
            ExecutableContentRegion ec = this.end_executable_content_region(TAG_ON_EXIT);
            State state = this.get_current_state();
            // Add the collected content to the on-exit.
            state.onexit.addAll(ec.content);
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
            ExecutableContentRegion parent_content_id = this.current_executable_content;

            this.start_executable_content_region(true, TAG_IF);
            ExecutableContentRegion if_cid = this.current_executable_content;

            ExecutableContent if_ec = this.get_last_executable_content_entry_for_region(parent_content_id);
            if (if_ec instanceof If evc_if) {
                evc_if.content = if_cid;
            } else {
                com.bw.fsm.Log.panic(
                        "Internal Error: Executable Content missing in start_if in region #%s",
                        parent_content_id
                );
            }
        }

        protected void end_if() {
            this.end_executable_content_region(TAG_IF);
        }

        protected void start_else_if(Attributes attr) {
            this.verify_parent_tag(TAG_ELSEIF, new String[]{TAG_IF});

            // Close parent <if> content region
            this.end_executable_content_region(TAG_IF);

            ExecutableContentRegion if_id = this.current_executable_content;

            // Start new "else" region - will contain only one "if", replacing current "if" stack element.
            this.start_executable_content_region(true, TAG_IF);
            ExecutableContentRegion else_id = this.current_executable_content;

            // Add new "if"
            If else_if = new If(this.create_source(get_required_attr(TAG_IF, ATTR_COND, attr)));
            this.add_executable_content(else_if);

            ExecutableContentRegion else_if_content_id = this.start_executable_content_region(true, TAG_ELSEIF);

            // Put together
            ExecutableContent else_if_ec = this.get_last_executable_content_entry_for_region(else_id);
            if (else_if_ec instanceof If evc_if) {
                evc_if.content = else_if_content_id;
            } else {
                com.bw.fsm.Log.panic(
                        "Internal Error: Executable Content missing in start_else_if in region #%s",
                        else_id
                );
            }

            while (if_id != null) {
                // Find matching "if" level for the new "else if"
                ExecutableContent if_ec = this.get_last_executable_content_entry_for_region(if_id);
                if (if_ec instanceof If evc_if) {
                    if (evc_if.else_content != null) {
                        // Some higher "if". Go inside else-region.
                        if_id = evc_if.else_content;
                    } else {
                        // Match, set "else-region".
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

            // Close parent <if> content region
            this.end_executable_content_region(TAG_IF);

            ExecutableContentRegion if_id = this.current_executable_content;

            // Start new "else" region, replacing "If" region.
            ExecutableContentRegion else_id = this.start_executable_content_region(true, TAG_IF);

            // Put together. Set deepest else
            while (if_id != null) {
                ExecutableContent if_ec = this.get_last_executable_content_entry_for_region(if_id);
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
            throw new UnsupportedOperationException();
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

        /// Reads the content of the current element.
        /// Calles "pop" to remove the element from stack.
        protected String read_content(String tag, XMLStreamReader reader) throws IOException {

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

                return writer.toString();
            } catch (XMLStreamException e) {
                throw new IOException(e);
            } finally {
                releaseWriter(writer);
            }

        }

        protected void start_content(Attributes attr, XMLStreamReader reader, boolean has_content) {
            this.verify_parent_tag(TAG_CONTENT, new String[]{TAG_SEND, TAG_INVOKE, TAG_DONEDATA});
            throw new UnsupportedOperationException();
        }

        protected void start_param(Attributes attr) {
            this.verify_parent_tag(TAG_PARAM, new String[]{TAG_SEND, TAG_INVOKE, TAG_DONEDATA});
            throw new UnsupportedOperationException();
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

        protected void start_assign(Attributes attr, XMLStreamReader reader, boolean has_content) throws IOException {
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

            String assign_text;
            if (has_content) {
                assign_text = this.read_content(TAG_ASSIGN, reader);
                if (!assign_text.isEmpty())
                    assign_text = String.format("\"%s\"", assign_text);
            } else {
                assign_text = "";
            }
            ;

            String assign_src = assign_text.trim();

            if (!assign_src.isEmpty()) {
                if (!assign.expr.is_empty()) {
                    com.bw.fsm.Log.panic("<assign> with 'expr' attribute shall not have content.");
                }
                assign.expr = create_source(assign_src);
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
                if (StaticOptions.debug_option)
                    com.bw.fsm.Log.debug(" scxml.datamodel = %s", datamodel);
                this.fsm.datamodel = datamodel;
            }
            String binding = attr.getValue(ATTR_BINDING);
            if (binding != null) {
                try {
                    this.fsm.binding = BindingType.valueOf(binding);
                } catch (IllegalArgumentException iae) {
                    com.bw.fsm.Log.panic("%s: unsupported value %s", ATTR_BINDING, binding);
                }
            }
            String version = attr.getValue(TAG_VERSION);
            if (version != null) {
                this.fsm.version = version;
                if (StaticOptions.debug_option)
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
                if (state.states.isEmpty()) {
                    // No states at all
                } else {
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


        public void start_element(String name, Attributes attr, XMLStreamReader reader, boolean has_content) throws IOException {
            this.push(name);

            switch (name) {
                case TAG_INCLUDE -> this.include(attr);
                case TAG_SCXML -> this.start_scxml(attr);
                case TAG_DATAMODEL -> this.start_datamodel();
                case TAG_DATA -> this.start_data(attr, reader, has_content);
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
                case TAG_SCRIPT -> this.start_script(attr, reader, has_content);
                case TAG_RAISE -> this.start_raise(attr);
                case TAG_SEND -> this.start_send(attr);
                case TAG_PARAM -> this.start_param(attr);
                case TAG_CONTENT -> this.start_content(attr, reader, has_content);
                case TAG_LOG -> this.start_log(attr);
                case TAG_ASSIGN -> this.start_assign(attr, reader, has_content);
                case TAG_FOR_EACH -> this.start_for_each(attr);
                case TAG_CANCEL -> this.start_cancel(attr);
                case TAG_IF -> this.start_if(attr);
                case TAG_ELSE -> this.start_else(attr);
                case TAG_ELSEIF -> this.start_else_if(attr);
                default -> {
                    if (debug_option)
                        com.bw.fsm.Log.debug("Ignored tag %s", name);
                }
            }
        }

        /// Try to resolve the file name relative to the current file or include paths.
        protected Path get_resolved_path(String ps) throws FileNotFoundException {
            while (ps.startsWith("\\") || ps.startsWith("/")) {
                ps = ps.substring(1);
            }
            Path src = Paths.get(ps);

            Path parent = this.file.getParent();

            Path to_current = (parent != null) ? parent.resolve(src) : src;

            if (Files.exists(to_current)) {
                return to_current;
            } else {
                for (Path ip : this.include_paths) {
                    if (Files.exists(ip)) {
                        return ip;
                    }
                }
            }
            throw new FileNotFoundException(ps);
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
            if (StaticOptions.debug_option)
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

    public ScxmlReader withIncludePaths(List<Path> includePaths) {
        this.includePaths.addAll(includePaths);
        return this;
    }
}
