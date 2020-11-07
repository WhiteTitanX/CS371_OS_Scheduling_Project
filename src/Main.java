import java.io.*;
import java.util.*;

public class Main {

    private static Random r;
    private static FileWriter fr;
    private static BufferedWriter br;

    public static void main(String[] args){
        try{
            fr = new FileWriter("log.txt", true);
            br = new BufferedWriter(fr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        r = new Random();
        int total_simulation_time = 0; // in seconds
        int quantum_size = 0; // in microseconds
        int context_switch_time = 0; // in microseconds
        int avg_process_length = 0; // in microseconds
        int avg_process_creation = 0; // in microseconds
        int percent_io_jobs = 0; // percentage
        int avg_io_interrupt = 0; // microseconds

        try{
            FileReader fr = new FileReader("params1.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            int counter = 0;

            while((line = br.readLine()) != null){
                if(!line.startsWith("#")){
                    int part = Integer.parseInt(line.split(" ")[0]);
                    switch (counter) {
                        case 0 -> total_simulation_time = part * 1000000;
                        case 1 -> quantum_size = part;
                        case 2 -> context_switch_time = part;
                        case 3 -> avg_process_length = part;
                        case 4 -> avg_process_creation = part;
                        case 5 -> percent_io_jobs = part;
                        case 6 -> avg_io_interrupt = part;
                    }
                    ++counter;
                }
            }

            br.close();
            fr.close();
        }catch (FileNotFoundException e){
            System.err.println("File not found!");
            return;
        }catch (IOException e){
            e.printStackTrace();
            return;
        }

        long simulatorTime = 0;
        int pid = 0;
        PriorityQueue<Event> eventQueue = new PriorityQueue<>(50,
                Comparator.comparingLong(Event::getTime));
        Queue<Process> readyQueue = new LinkedList<>();
        Queue<Process> ioQueue = new LinkedList<>();
        Queue<Process> cpu = new LinkedList<>();
        Process currentProcess;

        eventQueue.add(new Event("NEW_PROCESS", 0));

        while(simulatorTime < total_simulation_time && eventQueue.peek() != null){
            Event currentEvent = eventQueue.poll();
            simulatorTime = currentEvent.getTime();

            switch (currentEvent.getType()) {
                case "NEW_PROCESS" -> {
                    long new_process_creation_time = (long) (randomExponentialNumber(avg_process_creation) + simulatorTime);
                    long total_cpu_time = (long) randomExponentialNumber(avg_process_length);
                    int current_cpu_burst;
                    if (randomNumber(1, 100) < percent_io_jobs)
                        current_cpu_burst = randomNumber(1000, 2000);
                    else
                        current_cpu_burst = randomNumber(10000, 20000);
                    readyQueue.add(new Process(pid++, total_cpu_time, current_cpu_burst, simulatorTime));
                    eventQueue.add(new Event("NEW_PROCESS", new_process_creation_time));
                    if (cpu.size() == 0)
                        eventQueue.add(new Event("READY_TO_CPU", simulatorTime + context_switch_time));
                    debug(0, "NEW_PROCESS", null, (pid - 1) + " total_cpu_time: " +
                            total_cpu_time/1000000.0 + " " + (current_cpu_burst < 5000 ? "IO-Bound" : "CPU-Bound")
                            + " nextProcessAt: " + new_process_creation_time / 1000000.0, simulatorTime);
                }
                case "READY_TO_CPU" -> {
                    cpu.add(currentProcess = readyQueue.remove());
                    eventQueue.add(new Event("CPU_SCHEDULER", simulatorTime + context_switch_time));
                    debug(0, "READY_TO_CPU", currentProcess, "", simulatorTime);
                }
                case "CPU_SCHEDULER" -> {
                    currentProcess = cpu.peek();
                    if (currentProcess == null)
                        return;

                    long timeSpent = 0;
                    long remainingBurst = currentProcess.getRemainingCPUBurst();
                    long remainingTime = currentProcess.getRemainingCPUTime();
                    if(remainingBurst - quantum_size < 0){
                        remainingTime -= remainingBurst;

                        if(remainingTime > 0){
                            timeSpent += remainingBurst;
                            currentProcess.setElapsedCPUTime(currentProcess.getElapsedCPUTime() + remainingBurst);
                        }else{
                            timeSpent = remainingTime + remainingBurst;
                            currentProcess.setElapsedCPUTime(currentProcess.getElapsedCPUTime() + remainingTime + remainingBurst);
                        }
                        remainingBurst = 0;
                    }else{
                        remainingTime -= quantum_size;
                        remainingBurst -= quantum_size;
                        timeSpent += quantum_size;
                        if(remainingTime > 0){
                            currentProcess.setElapsedCPUTime(currentProcess.getElapsedCPUTime() + quantum_size);
                        }else{
                            currentProcess.setElapsedCPUTime(currentProcess.getElapsedCPUTime() + remainingTime + quantum_size);
                        }
                    }
                    currentProcess.setRemainingCPUBurst(remainingBurst);
                    currentProcess.setRemainingCPUTime(remainingTime);
                    if(currentProcess.getRemainingCPUTime() <= 0)
                        eventQueue.add(new Event("PROCESS_DONE", simulatorTime + context_switch_time + timeSpent));
                    else if (currentProcess.getRemainingCPUBurst() <= 0)
                        eventQueue.add(new Event("IO_INTERRUPT", simulatorTime + timeSpent));
                    else
                        eventQueue.add(new Event("QUANTUM_EXP", simulatorTime + quantum_size));
                    debug(0, "CPU_SCHEDULER", currentProcess, "", simulatorTime);
                }
                case "QUANTUM_EXP" -> {
                    currentProcess = cpu.remove();
                    readyQueue.add(currentProcess);
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    debug(0, "QUANTUM_EXP", currentProcess, "", simulatorTime);
                }
                case "IO_INTERRUPT" -> {
                    currentProcess = cpu.remove();
                    ioQueue.add(currentProcess);
                    int interruptTime = (int) randomExponentialNumber(avg_io_interrupt);
                    currentProcess.setElapsedIOTime(currentProcess.getElapsedIOTime() + interruptTime);
                    currentProcess.setIoRequests(currentProcess.getIoRequests() + 1);
                    eventQueue.add(new Event("IO_COMPLETE", simulatorTime + interruptTime));
                    debug(0, "IO_INTERRUPT", currentProcess, "" + (interruptTime/1000000.0), simulatorTime);
                }
                case "IO_COMPLETE" -> {
                    currentProcess = ioQueue.remove();
                    readyQueue.add(currentProcess);
                    if (currentProcess.isIoBound())
                        currentProcess.setRemainingCPUBurst(randomNumber(1000, 2000));
                    else
                        currentProcess.setRemainingCPUBurst(randomNumber(10000, 20000));
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    debug(0, "IO_COMPLETE", currentProcess, "", simulatorTime);
                }
                case "PROCESS_DONE" -> {
                    currentProcess = cpu.remove();
                    debug(0, "PROCESS_DONE", currentProcess, "", simulatorTime);
                }
            }
        }

        try{
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Finished Simulation!");
    }

    private static int randomNumber(int startRange, int endRange){
        return r.nextInt((endRange - startRange) + 1) + startRange;
    }

    private static double randomExponentialNumber(int expected){
        return -expected * Math.log(Math.random());
    }

    private static void debug(int level, String event, Process p, String additional, long simulatorTime){
        String message = "";
        if(level == 0){
            message += "TIME: " + simulatorTime/1000000.0 + " EVENT: " + event + " ";
            switch (event) {
                case "NEW_PROCESS" -> message += additional;
                case "READY_TO_CPU" -> message += "PID: "
                        + p.getPID() + " currentBurst: " + p.getRemainingCPUBurst()/1000000.0 + " totalCPURem: "
                        + p.getRemainingCPUTime()/1000000.0;
                case "CPU_SCHEDULER", "QUANTUM_EXP", "IO_COMPLETE" -> message += "PID: " + p.getPID();
                case "IO_INTERRUPT" -> message += "PID: " + p.getPID() + " IO-Interrupt: " + additional;
                case "PROCESS_DONE" -> message += "PID: " + p.getPID()
                        + " " + (p.isIoBound() ? "IO-Bound" : "CPU-Bound") + " totalCPU: " +
                        p.getElapsedCPUTime()/1000000.0 + " waitReady: " + p.getElapsedQueueTime(simulatorTime)/1000000.0 +
                        " IOTime: " + p.getElapsedIOTime()/1000000.0 + " IORequests: " + p.getIoRequests();
            }
            System.out.println(message);
            try{
                br.write(message + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
