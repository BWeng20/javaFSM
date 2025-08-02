package com.bw.fsm;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Downloads and transforms the W3C test files.<br>
 * URLs are currently hard coded. See statics below.
 */
public class TestDownloader {

    static String TEST_SOURCE_URL = "https://www.w3.org/Voice/2013/scxml-irp/";
    static String XSL_FILE = "confEcma.xsl";
    static String MANIFEST_URL = "https://www.w3.org/Voice/2013/scxml-irp/manifest.xml";

    static int downloaded = 0;

    static DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    static XPathFactory xPathFactory = XPathFactory.newInstance();


    static boolean downloadIfMissing(Path local, String urlSource) throws IOException {
        if (!Files.exists(local)) {
            Log.info(" downloading " + urlSource + " (-> " + local + ")");

            URL url = new URL(urlSource);

            try (InputStream in = url.openStream()) {
                Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
            }
            ++downloaded;
            return true;
        }
        return false;
    }

    /**
     * If missing, downloads the file.
     * if file name ends with ".txml" it is transformed, otherwise copied to scxml directory.
     */
    static int downloadAndTransform(Path manifest, String xpathQuery, Path txml, Path scxml, Path xslPath) throws Exception {
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document manifestDoc = builder.parse(manifest.toFile());
        XPath xpath = xPathFactory.newXPath();
        int testCount = 0;

        //  Select all mandatory, not-manual txml-test-files.
        XPathExpression xPathExpression = xpath.compile(xpathQuery);
        NodeList nodeList = (NodeList) xPathExpression.evaluate(manifestDoc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node bookNode = nodeList.item(i);
            String testUri = bookNode.getTextContent();
            String fileName = testUri.substring(testUri.indexOf('/') + 1);
            Path testFile = txml.resolve(fileName);
            Path transformedFile;
            Path copiedFile;
            if (fileName.endsWith(".txml")) {
                transformedFile = scxml.resolve(fileName + ".scxml");
                copiedFile = null;
            } else {
                transformedFile = null;
                copiedFile = scxml.resolve(fileName);
            }
            Log.info("Test file: " + testFile);
            ++testCount;

            if (downloadIfMissing(testFile, TEST_SOURCE_URL + testUri)) {
                if (transformedFile != null)
                    Files.deleteIfExists(transformedFile);
                if (copiedFile != null)
                    Files.deleteIfExists(copiedFile);
            }

            if (transformedFile != null) {
                if (Files.exists(transformedFile)) {
                    if (Files.size(transformedFile) == 0) {
                        // Possible a previous transformer error
                        Log.info(" transformed file empty: " + transformedFile);
                        Files.deleteIfExists(transformedFile);
                    }
                }

                if (!Files.exists(transformedFile)) {
                    try {
                        XmlTransformer.transform(testFile, xslPath, transformedFile);
                        Log.info(" transformed to file: " + transformedFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(" transformation failed: " + e.getMessage());
                    }
                }
            }
            if (copiedFile != null && !Files.exists(copiedFile)) {
                Files.copy(testFile, copiedFile);
            }
        }
        return testCount;
    }

    public static void downloadAndTransform(Path scriptDir) {

        try {

            Log.info("Working in " + scriptDir);

            Path manualTxml = scriptDir.resolve("manual_txml");
            Path manualScxml = scriptDir.resolve("manual_scxml");
            Path optionalTxml = scriptDir.resolve("optional_txml");
            Path optionalScxml = scriptDir.resolve("optional_scxml");
            Path txml = scriptDir.resolve("txml");
            Path scxml = scriptDir.resolve("scxml");
            Path dependencies = scriptDir.resolve("dependencies");
            Path dependenciesScxml = dependencies.resolve("scxml");

            int testCount = 0;
            int depCount = 0;

            Files.createDirectories(manualTxml);
            Files.createDirectories(manualScxml);
            Files.createDirectories(optionalTxml);
            Files.createDirectories(optionalScxml);
            Files.createDirectories(txml);
            Files.createDirectories(scxml);
            Files.createDirectories(dependenciesScxml);

            Path xslPath = txml.resolve(XSL_FILE);
            downloadIfMissing(xslPath, TEST_SOURCE_URL + XSL_FILE);

            Path manifest = txml.resolve("manifest.xml");
            downloadIfMissing(manifest, MANIFEST_URL);

            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document manifestDoc = builder.parse(manifest.toFile());

                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xpath = xPathFactory.newXPath();

                //  Select all mandatory, not-manual txml-test-files.
                testCount += downloadAndTransform(manifest, "//assert/test[@conformance='mandatory' and @manual='false']/start[contains(@uri,'.txml')]/@uri",
                        txml, scxml, xslPath);

                // Select all mandatory, manual txml-test-files.
                testCount += downloadAndTransform(manifest, "//assert/test[@conformance='mandatory' and @manual='true']/start[contains(@uri,'.txml')]/@uri",
                        manualTxml, manualScxml, xslPath);

                // Select all optional
                testCount += downloadAndTransform(manifest, "//assert/test[@conformance='optional']/start[contains(@uri,'.txml')]/@uri",
                        optionalTxml, optionalScxml, xslPath);

                // Get all dependencies
                depCount +=
                        downloadAndTransform(manifest, "//assert/test[@conformance='mandatory']/dep/@uri",
                                dependencies, dependenciesScxml, xslPath);

            }

            Log.info("Statistics:");
            Log.info(" tests detected: " + testCount);
            Log.info(" downloaded files: " + downloaded);
            Log.info(" transformed files: " + XmlTransformer.fileCount);
            Log.info(" dependencies: " + depCount);


        } catch (Exception e) {
            e.printStackTrace();
            Log.error("Abort due to error");
            System.exit(1);
        }

    }
}
