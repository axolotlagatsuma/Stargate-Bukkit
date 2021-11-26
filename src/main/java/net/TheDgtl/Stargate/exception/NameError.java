package net.TheDgtl.Stargate.exception;

import net.TheDgtl.Stargate.TranslatableMessage;

/**
 * The name error exception is thrown when a portal name is invalid for whatever reason
 */
public class NameError extends Exception {

    private static final long serialVersionUID = -9187508162071170232L;
    private final TranslatableMessage errorMessage;

    /**
     * Instantiates a new name error exception
     *
     * @param errorMessage <p>The message to display to the user</p>
     */
    public NameError(TranslatableMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the translatable error message attached to this exception
     *
     * @return <p>This exception's translatable error message</p>
     */
    public TranslatableMessage getErrorMessage() {
        return errorMessage;
    }

}