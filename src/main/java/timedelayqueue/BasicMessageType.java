package timedelayqueue;

public enum BasicMessageType implements MessageType {
    SIMPLEMSG("A simple message from a sender to one or more recipients"),
    TWEET("A tweet from Twitter with all the metadata");

    private String description;

    /**
     * Create a message type with a description
     * @param description a simple description of the message type
     */
    BasicMessageType(String description) {
        this.description = description;
    }

    /**
     * Obtain the description for the message type
     * @return the description for the message type
     */
    public String getDescription() {
        return description;
    }
}
