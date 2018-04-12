package tools;

public class LoggerDebug {
    private static LoggerDebug instance = new LoggerDebug();

    public static LoggerDebug getInstance() {
        return instance;
    }

    public void log(String logLine) {
        System.out.println(logLine);
    }
}
