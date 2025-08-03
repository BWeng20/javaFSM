package com.bw.fsm;

import com.bw.fsm.tracer.TraceMode;

import java.nio.file.Path;
import java.util.ArrayList;

public class TestUseCase {
    public String name;
    public TestSpecification specification;
    public Fsm fsm;
    public TraceMode trace_mode;
    public final java.util.List<Path> include_paths = new ArrayList<>();
}
