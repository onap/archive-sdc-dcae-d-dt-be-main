package utilities;

import java.util.ArrayList;
import java.util.List;

public class Report implements IReport {

    private List<String> created = new ArrayList<>();
    private List<String> updated = new ArrayList<>();
    private List<String> notUpdated = new ArrayList<>();
    private List<String> error = new ArrayList<>();
	private int statusCode = 0;

    @Override
    public void addCreatedMessage(String message) {
        created.add(message);
    }

    @Override
    public void addUpdatedMessage(String message) {
        updated.add(message);
    }

    @Override
    public void addNotUpdatedMessage(String message) {
        notUpdated.add(message);
    }

    @Override
    public void addErrorMessage(String message) {
        error.add(message);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!created.isEmpty()) {
            stringBuilder.append("Created:").append(System.lineSeparator());
            created.forEach(msg -> stringBuilder.append(msg).append(System.lineSeparator()));
        }
        if (!updated.isEmpty()) {
            stringBuilder.append("Updated:").append(System.lineSeparator());
            updated.forEach(msg -> stringBuilder.append(msg).append(System.lineSeparator()));
        }
        if (!notUpdated.isEmpty()) {
            stringBuilder.append("Not updated:").append(System.lineSeparator());
            notUpdated.forEach(msg -> stringBuilder.append(msg).append(System.lineSeparator()));
        }
        if (!error.isEmpty()) {
            stringBuilder.append("Error:").append(System.lineSeparator());
            error.forEach(msg -> stringBuilder.append(msg).append(System.lineSeparator()));
        }
        return stringBuilder.toString();
    }


	public void reportAndExit() {
		System.exit(statusCode);
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

}
