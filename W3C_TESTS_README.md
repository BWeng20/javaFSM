# Tests from W3C

The project contains an application to execute the tests described in [SCXML 1.0 Implementation Report](https://www.w3.org/Voice/2013/scxml-irp/).

See [class W3CTest](src/test/java/com/bw/fsm/W3CTest.java).

## Usage

The run the default test application, use the gradle task `w3cTest`.

It will print the progress to the console.<br> 
Log output of each test is also redirected to a file in folder "w3ctest/logs".<br>

This task also writes the report-file, see the link below _Current status_.

### Manually call

During development, it is useful to manually call these tests.

``` java com.bw.fsm.W3CTest <testDirectory> [-report <file>] [-logOnlyFailure] [-stopOnError] [-dry] [-parallel] [-help]```

| Option            | Comment                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-logOnlyFailure` | 	Create logs only if a test fails.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `-stopOnError`	   | Stops tests on first error.<br> Remind that the stop may be delayed if also -parallel is given.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `-dry`             | Tests the existing files in scxml without writing any files.<br> No download, no transformation, no report file.<br> All log output is written to console.<br> Useful during development to speed up test cycles.                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `-parallel`        | Use a parallel stream to process the tests.<br> Without this option the tests waits for each FSM to terminate.<br> The order of execution of different tests is not deterministic.<br>Useful for pipelines.                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `-optionals`       | Run also the tests in "optional_scxml".                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `-help`            | Prints usage message and exists.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `-report <file>`   | Creates a report file (not yet).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `testDirectory`   | Folder to put all tests files.<br> Must contain at least the test-config "test_config.json".<br>Created sub-folders:<br><ul><li>`logs`: main and test logs. One for each test file</li><li>`txml`: downloaded manifest, transformation and test files</li><li>`scxml`: transformed scxml test files</li><li>`dependencies`: downloaded includes</li><li>`dependencies/scxml`: transformed includes</li><li>`manual_txml`: downloaded manual tests</li><li>`manual_scxml`: transformed manual tests (not used)</li><li>`optional_txml`: downloaded optional tests</li><li>`optional_scxml`: transformed optional tests (not used)</li></ul> |

Very useful if fixing broken w3c tests after some "minor" change in your data model implementation:<br>
Call once ```java com.bw.fsm.W3CTest w3cTests``` to download the tests.<br>
Then for each iteration:<br>```java com.bw.fsm.W3CTest w3cTests -dry -logOnlyFailure -stopOnError```


## Implementation

### Download and Transform

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

### Execution of Tests

All transformed test-files in the folder `w3ctest/scxml` and (with `-optionals`) `w3ctest/optional_scxml` are executed by [class Tester](src/test/java/com/bw/fsm/Tester.java).


## Current status

The following table gives the current test result for javaFSM:

[REPORT.MD](W3C_TEST_REPORT.MD)
