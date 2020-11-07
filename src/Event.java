public class Event {

    private final String type;
    private final long time;

    public Event(String type, long time) {
        this.type = type;
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public long getTime() {
        return time;
    }
}
