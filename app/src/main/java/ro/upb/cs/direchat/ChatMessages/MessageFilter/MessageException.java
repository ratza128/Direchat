package ro.upb.cs.direchat.ChatMessages.MessageFilter;

/**
 * Exceptii pentru mesaje
 */
public class MessageException extends Exception{

    public static enum Reason {NULLMESSAGE, MESSAGETOSHORT, MESSAGEBLACKLISTED};

    private Reason reason;

    public Reason getReason() {
        return reason;
    }

    public MessageException() { super(); }

    /**
     * Constructor
     * @param message String mesaj
     * @param cause obiectul Throwable
     */
    public MessageException(String message, Throwable cause) { super(message, cause); }

    /**
     * Constructor
     * @param message  mesaj
     */
    public MessageException(String message) { super(message); }

    /**
     * Constructor
     * @param cause obiect Throwable
     */
    public MessageException(Throwable cause) { super(cause); }

    /**
     * Constructor
     */
    public MessageException(Reason reason) { this.reason = reason; }
}
