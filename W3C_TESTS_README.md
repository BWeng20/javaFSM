# Tests from W3C

The project contains an application to execute the tests described in [SCXML 1.0 Implementation Report](https://www.w3.org/Voice/2013/scxml-irp/).

See [class W3CTest](src/test/java/com/bw/fsm/W3CTest.java).

## Download and Transform

See [class TestDownloader](src/test/java/com/bw/fsm/TestDownloader.java).

The Identifiers of the test are extracted from [https://www.w3.org/Voice/2013/scxml-irp/manifest.xml](https://www.w3.org/Voice/2013/scxml-irp/manifest.xml).
The script select all _mandatory_ and _automated_ txml-tests. Optional or manual tests are ignored.
The test files itself are downloaded from [https://www.w3.org/Voice/2013/scxml-irp/](https://www.w3.org/Voice/2013/scxml-irp/).

The original w3c-tests are written in a data-model-agnostic way and
need a xsl transformation to (in this case) the ECMA-data-model.<br/>
W3C delivers a xsl-transformation for this case. 
The [class TestDownloader](src/test/java/com/bw/fsm/TestDownloader.java) calls [class XmlTransformer](src/test/java/com/bw/fsm/XmlTransformer.java)
to apply the xsl transformation for each file.

The xsl transformation need XSLT 2.0 support, the default jdk Xalan implementation can handle only XSLT 1.0.
For this reason we use Saxon as maven dependency.

To spare time and costs, all needed w3c files are added to the repository with their version dated 2025-08-01.
`TestDownloader` never downloads an existing file, to update to newer versions, someone can delete the folder `w3ctest` and execute the w3c-test.

The transformed test are placed in the sub-folder `w3ctest/scxml`.

## Execution of Tests

All transformed test-files in the folder `w3ctest/scxml` are executed by [class Tester](src/test/java/com/bw/fsm/Tester.java).


## Running the tests

The run the test application, use the gradle task `w3cTest`.

It will print the progress to the console.<br>
Output of the tests is also redirected to a file in folder "w3ctest/logs".

The application writes also the Report-file, that is linked below.

## Test status

The following table gives the current test result for javaFSM:

[REPORT.MD](W3C_TEST_REPORT.MD)

The latest version is available in the repository.