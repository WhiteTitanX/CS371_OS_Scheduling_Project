public class Process {

    private long remainingCPUTime;
    private long remainingCPUBurst;
    private long elapsedCPUTime;
    private long elapsedIOTime;
    private long elapsedQueueTime;
    private int ioRequests;

    private final int pid;
    private final boolean ioBound;
    private final long startTime;

    public Process(int pid, long remainingCPUTime, int remainingCPUBurst, long startTime) {
        this.pid = pid;
        this.remainingCPUTime = remainingCPUTime;
        this.remainingCPUBurst = remainingCPUBurst;
        this.ioBound = remainingCPUBurst < 5000;
        this.elapsedCPUTime = 0;
        this.elapsedIOTime = 0;
        this.ioRequests = 0;
        this.startTime = startTime;
    }

    public long getElapsedCPUTime() {
        return elapsedCPUTime;
    }

    public long getElapsedIOTime() {
        return elapsedIOTime;
    }

    public long getElapsedQueueTime(long endTime) {
        elapsedQueueTime = endTime - startTime - elapsedIOTime - elapsedCPUTime;
        return endTime - startTime - elapsedIOTime - elapsedCPUTime;
    }

    public long getElapsedQueueTime(){
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

    public void setIoRequests(int ioRequests) {
        this.ioRequests = ioRequests;
    }
}
