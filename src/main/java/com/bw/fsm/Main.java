package com.bw.fsm;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws XMLStreamException, IOException {
        System.out.println("42");

        if (args.length > 0) {
            ScxmlReader reader = new ScxmlReader();

            reader.read(Paths.get(args[0]));
        }
    }
}