package com.bw.fsm;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.nio.file.Path;

public class XmlTransformer {

    public static int fileCount = 0;

    public static void transform(Path inputXmlPath, Path xsltPath, Path outputXmlPath) throws TransformerException, IOException {
        Source xmlSource = new StreamSource(inputXmlPath.toFile());
        Source xsltSource = new StreamSource(xsltPath.toFile());

        // We need XSLT 2.0 support, jdk Xalan can handle only XSLT 1.0
        TransformerFactory factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        Transformer transformer = factory.newTransformer(xsltSource);

        StreamResult result = new StreamResult(outputXmlPath.toFile());

        transformer.transform(xmlSource, result);
        ++fileCount;
    }
}
