public class Process {

    private long remainingCPUTime;
    private long remainingCPUBurst;
    private long elapsedCPUTime;
    private long elapsedIOTime;
    private long elapsedQueueTime;
    private int ioRequests;

    private final int pid;
    private final boolean ioBound;
    private long startTime;

    public Process(int pid, long remainingCPUTime, int remainingCPUBurst) {
        this.pid = pid;
        this.remainingCPUTime = remainingCPUTime;
        this.remainingCPUBurst = remainingCPUBurst;
        this.ioBound = remainingCPUBurst < 5000;
    }

    public long getElapsedCPUTime() {
        return elapsedCPUTime;
    }

    public long getElapsedIOTime() {
        return elapsedIOTime;
    }

    public long getElapsedQueueTime() {
        return elapsedQueueTime;
    }

    public int getIoRequests() {
        return ioRequests;
    }

    public int getPID(){
        return pid;
    }

    public long getRemainingCPUTime() {
        return remainingCPUTime;
    }

    public long getRemainingCPUBurst() {
        return remainingCPUBurst;
    }

    public boolean isIoBound() {
        return ioBound;
    }

    public void setRemainingCPUTime(long remainingCPUTime) {
        this.remainingCPUTime = remainingCPUTime;
    }

    public void setRemainingCPUBurst(long remainingCPUBurst) {
        this.remainingCPUBurst = remainingCPUBurst;
    }

    public void setElapsedCPUTime(long elapsedCPUTime) {
        this.elapsedCPUTime = elapsedCPUTime;
    }

    public void setElapsedIOTime(long elapsedIOTime) {
        this.elapsedIOTime = elapsedIOTime;
    }

    public void setElapsedQueueTime(long elapsedQueueTime) {
        this.elapsedQueueTime = elapsedQueueTime;
    }

    public void setIoRequests(int ioRequests) {
        this.ioRequests = ioRequests;
    }
}
