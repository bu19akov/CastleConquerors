package testResult;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.extension.BeforeAllCallback;

import java.util.HashMap;
import java.util.Map;

public class TestResultLogger implements TestWatcher, AfterAllCallback, BeforeAllCallback {

    private Map<String, Integer> successTests = new HashMap<>();
    private Map<String, Integer> failedTests = new HashMap<>();
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        System.out.println(context.getDisplayName() + ":");
    }
    
    @Override
    public void testSuccessful(ExtensionContext context) {
        String className = context.getRequiredTestClass().getName();
        successTests.put(className, successTests.getOrDefault(className, 0) + 1);
        System.out.println("\u001B[32m" + "success" + "\u001B[0m" + " " + context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String className = context.getRequiredTestClass().getName();
        failedTests.put(className, failedTests.getOrDefault(className, 0) + 1);
        System.out.println("\u001B[31m" + "failed" + "\u001B[0m" + " " + context.getDisplayName());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        String className = context.getRequiredTestClass().getName();
        int successCount = successTests.getOrDefault(className, 0);
        int failureCount = failedTests.getOrDefault(className, 0);
        int totalCount = successCount + failureCount;

        if (failureCount == 0) {
        	System.out.println("\u001B[42m\u001B[30m" + "OK: all " + totalCount + " tests passed" + "\u001B[0m" + "\n");
        } else {
        	System.out.println("\u001B[41m\u001B[30m" + "FAILURE: " + successCount + " out of " + totalCount + " tests passed" + "\u001B[0m");

        }
    }
}
