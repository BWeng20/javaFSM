package com.bw.fsm;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public final class IOTool {


    public static String getFileExtension(Path path) {
        return getFileExtension(path.getFileName().toString());
    }

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    /**
     * Split an OS path list.
     */
    public static java.util.List<Path> splitPaths(String paths) {
        if (paths != null) {
            return Arrays.stream(paths.split(File.pathSeparator)).map(Paths::get).toList();
        } else {
            return Collections.emptyList();
        }
    }
}
