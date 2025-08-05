package com.bw.fsm;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("42");

        if (args.length > 0) {
            ScxmlReader reader = new ScxmlReader();

            reader.parse_from_xml_file(Paths.get(args[0]));
        }
    }
}