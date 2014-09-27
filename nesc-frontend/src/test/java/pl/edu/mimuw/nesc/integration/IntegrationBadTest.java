package pl.edu.mimuw.nesc.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.ProjectData;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.issue.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.Parameters;

/**
 * <p>Tests for detecting errors in programs. Each test is associated with
 * a test program. The test program shall be present in a subdirectory of
 * directory:
 *
 * <code>nesc-frontend/src/test/resources/integration/programs/bad</code>
 *
 * and contain exactly one error. Only tests that are acknowledged in
 * <code>testData</code> method are performed.</p>
 * <p>A test is passed if and only if only one issue with a proper code is
 * reported for a proper file and all other files and the project have no
 * issues.</p>
 * <p>A program for a bad test shall have exactly one issue and otherwise it
 * shall be correct and valid. The main configuration shall have the name that
 * is the value of {@link IntegrationTestBase#MAIN_ENTITY_NAME} static variable.
 * </p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
@RunWith(Parameterized.class)
public class IntegrationBadTest extends IntegrationTestBase {
    /**
     * Absolute path to the directory with the whole program that will be
     * tested.
     */
    private final String absoluteDirPath;

    /**
     * Name of the file in the absolute directory that contains the error.
     * It shall be only the name (without path).
     */
    private final String fileWithError;

    /**
     * Unique and only instance of the issue code that is expected in the tested
     * program.
     */
    private final Issue.Code expectedIssue;

    @Parameters
    public static List<Object[]> testData() {
        final List<Object[]> result = new ArrayList<>();

        // Add all test programs
        result.add(new Object[] { "bad01", "C.nc", AttributeUsageAsTypeError.CODE });
        result.add(new Object[] { "bad02", "C.nc", RedefinitionError.CODE });
        result.add(new Object[] { "bad03", "C.nc", InvalidArrayElementTypeError.CODE });
        result.add(new Object[] { "bad04", "C.nc", InvalidFunctionReturnTypeError.CODE });
        result.add(new Object[] { "bad05", "C.nc", InvalidFieldTypeError.CODE });
        result.add(new Object[] { "bad06", "C.nc", ConflictingTagKindError.CODE });
        result.add(new Object[] { "bad07", "C.nc", TypeSpecifierRepetitionError.CODE });
        result.add(new Object[] { "bad08", "C.nc", RedefinitionError.CODE });
        result.add(new Object[] { "bad09", "C.nc", InvalidTypeSpecifiersMixError.CODE });
        result.add(new Object[] { "bad10", "C.nc", ConflictingStorageSpecifierError.CODE });

        // Make the directory absolute
        final String testsDir = getResourceDirectory("integration/programs/bad");
        for (Object[] testArgs : result) {
            testArgs[0] = testsDir + "/" + testArgs[0];
        }

        return result;
    }

    public IntegrationBadTest(String absoluteDirPath, String fileWithError, Issue.Code expectedIssue) {
        checkNotNull(absoluteDirPath, "absolute directory path cannot be null");
        checkNotNull(fileWithError, "name of the file with the error cannot be null");
        checkNotNull(expectedIssue, "code of the expected issue cannot be null");

        this.absoluteDirPath = absoluteDirPath;
        this.fileWithError = fileWithError;
        this.expectedIssue = expectedIssue;
    }

    @Test
    public void errorDetected() throws FileNotFoundException, InvalidOptionsException {
        final ProjectData projectData = getProjectData(frontend, absoluteDirPath, MAIN_ENTITY_NAME);
        assertTrue("issues present for the project", projectData.getIssues().isEmpty());
        boolean errorDetected = false;

        for (Map.Entry<String, FileData> entry : projectData.getFileDatas().entrySet()) {
            final String fileName = new File(entry.getKey()).getName();
            final FileData fileData = entry.getValue();

            if (fileWithError.equals(fileName)) {
                assertEquals(1, fileData.getIssues().size());
                for (Map.Entry<Integer, NescIssue> issueEntry : fileData.getIssues().entries()) {
                    final NescIssue issue = issueEntry.getValue();
                    assertTrue("code is absent in the issue", issue.getCode().isPresent());
                    assertTrue("issue with invalid code", issue.getCode().get() == expectedIssue);
                    errorDetected = true;
                }
            } else {
                assertTrue(format("issue in file '%s'", fileName), fileData.getIssues().isEmpty());
            }
        }

        assertTrue("the error has not been detected", errorDetected);
    }
}
