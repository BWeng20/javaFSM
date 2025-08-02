# Tests from W3C

The project contains an application to execute the tests described in [SCXML 1.0 Implementation Report](https://www.w3.org/Voice/2013/scxml-irp/).
See [class W3CTest](src/test/java/com/bw/fsm/W3CTest.java).

## Download and Transform

The original w3c-tests are written in a data-model-agnostic way and
need a xsl transformation to (in this case) the ECMA-data-model.<br/>
W3C delivers a xsl-transformation for this case.

The Identifiers of the test are extracted from [https://www.w3.org/Voice/2013/scxml-irp/manifest.xml](https://www.w3.org/Voice/2013/scxml-irp/manifest.xml).
The script select all _mandatory_ and _automated_ txml-tests. Optional or manual tests are ignored.
The test files itself are downloaded from [https://www.w3.org/Voice/2013/scxml-irp/](https://www.w3.org/Voice/2013/scxml-irp/).

See [class TestDownloader](src/test/java/com/bw/fsm/TestDownloader.java) and [class XmlTransformer](src/test/java/com/bw/fsm/XmlTransformer.java).

`TestDownloader` never downloads a file twice, to update to newer versions,
delete the folder `w3ctest/txml` and call the tests again.

The transformed test are placed in the sub-folder `w3ctest/scxml`.

## Execution of Tests

TODO


### Requirements

The xsl transformation need XSLT 2.0 support, the default jdk Xalan implementation can handle only XSLT 1.0.
For this reason we use Saxon as maven dependency.

## Running the tests

Run the gradle task `w3cTest`.

It will print the progress to the console.<br>
Output of the tests is also redirected to files in folder "w3ctest/logs".

The script writes also the Report-file, that is linked below.

## Current status

The following table gives the current test result for javaFSM:

[REPORT.MD](W3C_TEST_REPORT.MD)

