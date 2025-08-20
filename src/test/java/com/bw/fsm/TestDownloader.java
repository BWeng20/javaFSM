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
    public boolean dry = false;

    private int downloaded = 0;

    static DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    static XPathFactory xPathFactory = XPathFactory.newInstance();

    public TestDownloader(Path scriptDir) {
        this.scriptDir = scriptDir;
    }

    private boolean downloadIfMissing(Path local, String urlSource) throws IOException {
        return downloadRequiredIfMissing(local, urlSource, false);
    }

    private boolean downloadRequiredIfMissing(Path local, String urlSource, boolean throwIfMissing) throws IOException {

        if (!Files.exists(local)) {

            if (dry) {
                if (throwIfMissing)
                    throw new IOException("File missing (dry run): " + urlSource);
                return false;
            }

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
    private int downloadAndTransform(Path manifest, String xpathQuery, Path txml, Path scxml, Path xslPath) throws Exception {
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
                transformedFile = scxml.resolve(fileName.substring(0, fileName.length()-5) + ".scxml");
                copiedFile = null;
            } else {
                transformedFile = null;
                copiedFile = scxml.resolve(fileName);
            }
            if ( StaticOptions.debug_option)
                 Log.debug("Test file: " + testFile);
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
                        if (!dry) {
                            Files.deleteIfExists(transformedFile);
                        }
                    }
                }

                if (!Files.exists(transformedFile)) {
                    if (!dry) {
                        try {
                            XmlTransformer.transform(testFile, xslPath, transformedFile);
                            Log.info(" transformed to file: " + transformedFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println(" transformation failed: " + e.getMessage());
                        }
                    }
                }
            }
            if ((!dry) && copiedFile != null && !Files.exists(copiedFile)) {
                Files.copy(testFile, copiedFile);
            }
        }
        return testCount;
    }

    public Path manualTxml;
    public Path manualScxml;
    public Path optionalTxml;
    public Path optionalScxml;
    public Path txml;
    public Path scxml;
    public Path dependencies;
    public Path dependenciesScxml;

    private Path scriptDir;

    public void downloadAndTransform() {

        try {

            Log.info("Working in " + scriptDir);

            manualTxml = scriptDir.resolve("manual_txml");
            manualScxml = scriptDir.resolve("manual_scxml");
            optionalTxml = scriptDir.resolve("optional_txml");
            optionalScxml = scriptDir.resolve("optional_scxml");
            txml = scriptDir.resolve("txml");
            scxml = scriptDir.resolve("scxml");
            dependencies = scriptDir.resolve("dependencies");
            dependenciesScxml = dependencies.resolve("scxml");

            int testCount = 0;
            int depCount = 0;

            if (!dry) {
                Files.createDirectories(manualTxml);
                Files.createDirectories(manualScxml);
                Files.createDirectories(optionalTxml);
                Files.createDirectories(optionalScxml);
                Files.createDirectories(txml);
                Files.createDirectories(scxml);
                Files.createDirectories(dependenciesScxml);
            }
            Path xslPath = txml.resolve(XSL_FILE);
            downloadRequiredIfMissing(xslPath, TEST_SOURCE_URL + XSL_FILE, true);

            Path manifest = txml.resolve("manifest.xml");
            downloadRequiredIfMissing(manifest, MANIFEST_URL, true);

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
                depCount += downloadAndTransform(manifest, "//assert/test[@conformance='optional']/dep/@uri",
                        dependencies, dependenciesScxml, xslPath);

            }

            Log.info((dry) ? "Statistics (dry run):" : "Statistics:");
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
