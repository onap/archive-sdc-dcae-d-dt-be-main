package utilities;

public interface IReport {
    void addCreatedMessage(String message);
    void addUpdatedMessage(String message);
    void addNotUpdatedMessage(String message);
    void addErrorMessage(String message);
    void setStatusCode(int statusCode);
    void reportAndExit();
}
