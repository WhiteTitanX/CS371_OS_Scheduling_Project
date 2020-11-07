public class Process {

    private int remainingCPUTime;
    private int remainingCPUBurst;
    private int elapsedCPUTime;
    private int elapsedIOTime;
    private int elapsedQueueTime;
    private int ioRequests;

    private final int pid;
    private final boolean ioBound;

    public Process(int pid, int remainingCPUTime, int remainingCPUBurst) {
        this.pid = pid;
        this.remainingCPUTime = remainingCPUTime;
        this.remainingCPUBurst = remainingCPUBurst;
        this.ioBound = remainingCPUBurst < 5000;
    }

    public int getElapsedCPUTime() {
        return elapsedCPUTime;
    }

    public int getElapsedIOTime() {
        return elapsedIOTime;
    }

    public int getElapsedQueueTime() {
        return elapsedQueueTime;
    }

    public int getIoRequests() {
        return ioRequests;
    }

    public int getPID(){
        return pid;
    }

    public int getRemainingCPUTime() {
        return remainingCPUTime;
    }

    public int getRemainingCPUBurst() {
        return remainingCPUBurst;
    }

    public boolean isIoBound() {
        return ioBound;
    }

    public void setRemainingCPUTime(int remainingCPUTime) {
        this.remainingCPUTime = remainingCPUTime;
    }

    public void setRemainingCPUBurst(int remainingCPUBurst) {
        this.remainingCPUBurst = remainingCPUBurst;
    }

    public void setElapsedCPUTime(int elapsedCPUTime) {
        this.elapsedCPUTime = elapsedCPUTime;
    }

    public void setElapsedIOTime(int elapsedIOTime) {
        this.elapsedIOTime = elapsedIOTime;
    }

    public void setElapsedQueueTime(int elapsedQueueTime) {
        this.elapsedQueueTime = elapsedQueueTime;
    }

    public void setIoRequests(int ioRequests) {
        this.ioRequests = ioRequests;
    }
}
