package tools;

public class LoggerError {
    private static LoggerError instance = new LoggerError();

    public static LoggerError getInstance() {
        return instance;
    }

    public void log(String logLine) {
    	//System.err.println(logLine);
    }

    public void log(String logLine, Exception e) {
        //System.err.println(logLine);
        e.printStackTrace();
    }
}
