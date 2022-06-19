package net.TheDgtl.Stargate.exception;

/**
 * An exception thrown when Stargate fails its initialization
 */
public class StargateInitializationException extends Exception {

    /**
     * Instantiates a new Stargate Initialization exception
     *
     * @param message <p>The message describing the cause of the exception</p>
     */
    public StargateInitializationException(String message) {
        super(message);
    }

}
