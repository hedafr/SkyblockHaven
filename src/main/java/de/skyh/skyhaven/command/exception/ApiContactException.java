package de.skyh.skyhaven.command.exception;

import de.skyh.skyhaven.Skyhaven;

public class ApiContactException extends SkyhCommandException {
    public ApiContactException(String api, String failedAction) {
        super("Sorry, couldn't contact the " + api + " API and thus " + failedAction);
        if (api.equals("Hypixel") && failedAction.contains("Invalid API key")) {
            Skyhaven.getInstance().getSkyh().setSkyhValidity(false);
        }
    }
}
