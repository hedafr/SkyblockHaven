package de.onyxp.onyxpannel.command.exception;

import de.onyxp.onyxpannel.Onyxpannel;

public class ApiContactException extends OnyxCommandException {
    public ApiContactException(String api, String failedAction) {
        super("Sorry, couldn't contact the " + api + " API and thus " + failedAction);
        if (api.equals("Hypixel") && failedAction.contains("Invalid API key")) {
            Onyxpannel.getInstance().getOnyx().setOnyxValidity(false);
        }
    }
}
