package com.bw.fsm;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tool to maintain include path, used for resolving files, loaded by FSMs.
 */
public class IncludePaths {
    private final List<Path> includePaths = new ArrayList<>();

    public IncludePaths() {
    }

    public void add(IncludePaths other) {
        add(other.includePaths);
    }

    public IncludePaths(Collection<Path> paths) {
        add(paths);
    }

    public void add(Collection<Path> paths) {
        for (Path p : paths)
            this.add(p);
    }

    public void add(Path path) {
        if (path != null)
            includePaths.add(path);
    }

    /**
     * Try to resolve the file name relative to the current file or include paths.
     */
    public Path resolvePath(String ps) throws FileNotFoundException {
        while (ps.startsWith("\\") || ps.startsWith("/")) {
            ps = ps.substring(1);
        }
        Path src = Paths.get(ps);
        if (Files.exists(src)) {
            return src;
        }

        for (Path ip : this.includePaths) {
            Path rp = ip.resolve(src);
            if (Files.exists(rp)) {
                return rp;
            }
        }
        throw new FileNotFoundException(ps);
    }

    /**
     * Adds path fdrom a system dependent path list.
     *
     * @param paths The path values, separated by system path separator char.
     */
    public void addPaths(String paths) {
        add(IOTool.splitPaths(paths));
    }
}
