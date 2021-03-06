import java.io.*;
import java.util.*;

public class Main {

    private static Random r;
    private static FileWriter fr;
    private static BufferedWriter br;
    private static final int DEBUG_LEVEL = 1;

    public static void main(String[] args){
        if(DEBUG_LEVEL == 0){
            try{
                fr = new FileWriter("log.txt", false);
                br = new BufferedWriter(fr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        r = new Random();
        long total_simulation_time = 0; // in seconds
        int quantum_size = 0; // in microseconds
        int context_switch_time = 0; // in microseconds
        int avg_process_length = 0; // in microseconds
        int avg_process_creation = 0; // in microseconds
        int percent_io_jobs = 0; // percentage
        int avg_io_interrupt = 0; // microseconds

        try{
            FileReader fr = new FileReader("params3.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            int counter = 0;

            while((line = br.readLine()) != null){
                if(!line.startsWith("#")){
                    int part = Integer.parseInt(line.split(" ")[0]);
                    switch (counter) {
                        case 0 -> total_simulation_time = (long) part * 1000000;
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

        //Stat variables
        long cpuTimeSum = 0;
        int contextSwitches = 0;

        PriorityQueue<Event> eventQueue = new PriorityQueue<>(50,
                Comparator.comparingLong(Event::getTime));
        Queue<Process> readyQueue = new LinkedList<>();
        Queue<Process> ioQueue = new LinkedList<>();
        Queue<Process> cpu = new LinkedList<>();
        LinkedList<Process> completedProcesses = new LinkedList<>();
        Process currentProcess;

        eventQueue.add(new Event("NEW_PROCESS", 0));

        while(simulatorTime < total_simulation_time && eventQueue.peek() != null){
            Event currentEvent = eventQueue.poll();
            simulatorTime = currentEvent.getTime();

            switch (currentEvent.getType()) {
                case "NEW_PROCESS" -> {
                    long new_process_creation_time = (long) (randomExponentialNumber(avg_process_creation) + simulatorTime);
                    long total_cpu_time = (long) randomExponentialNumber(avg_process_length);
                    cpuTimeSum+=total_cpu_time;
                    int current_cpu_burst;
                    if (randomNumber(1, 100) < percent_io_jobs)
                        current_cpu_burst = randomNumber(1000, 2000);
                    else
                        current_cpu_burst = randomNumber(10000, 20000);
                    readyQueue.add(new Process(pid++, total_cpu_time, current_cpu_burst, simulatorTime));
                    eventQueue.add(new Event("NEW_PROCESS", new_process_creation_time));
                    if (cpu.size() == 0)
                        eventQueue.add(new Event("READY_TO_CPU", simulatorTime + context_switch_time));
                    debug("NEW_PROCESS", null, "PID: " + (pid - 1) + " total_cpu_time: " +
                            total_cpu_time + " " + (current_cpu_burst < 5000 ? "IO-Bound" : "CPU-Bound")
                            + " nextProcessAt: " + new_process_creation_time / 1000000.0, simulatorTime);
                }
                case "READY_TO_CPU" -> {
                    if(cpu.size() > 0 || readyQueue.size() == 0)
                        break;
                    ++contextSwitches;
                    cpu.add(currentProcess = readyQueue.remove());
                    eventQueue.add(new Event("CPU_SCHEDULER", simulatorTime + context_switch_time));
                    debug("READY_TO_CPU", currentProcess, "", simulatorTime);
                }
                case "CPU_SCHEDULER" -> {
                    currentProcess = cpu.peek();
                    if (currentProcess == null)
                        return;

                    long timeSpent = 0;
                    long remainingBurst = currentProcess.getRemainingCPUBurst();
                    long remainingTime = currentProcess.getRemainingCPUTime();

                    if(remainingBurst > quantum_size && remainingTime > quantum_size){
                        remainingBurst -= quantum_size;
                        remainingTime -= quantum_size;
                        timeSpent += quantum_size;
                    }else{
                        if(remainingBurst > remainingTime){
                            remainingBurst -= remainingTime;
                            timeSpent += remainingTime;
                            remainingTime = 0;
                        }else{
                            remainingTime -= remainingBurst;
                            timeSpent += remainingBurst;
                            remainingBurst = 0;
                        }
                    }

                    currentProcess.setRemainingCPUBurst(remainingBurst);
                    currentProcess.setRemainingCPUTime(remainingTime);
                    currentProcess.setElapsedCPUTime(currentProcess.getElapsedCPUTime() + timeSpent);

                    if(currentProcess.getRemainingCPUTime() <= 0)
                        eventQueue.add(new Event("PROCESS_DONE", simulatorTime + context_switch_time + timeSpent));
                    else if (currentProcess.getRemainingCPUBurst() <= 0)
                        eventQueue.add(new Event("IO_INTERRUPT", simulatorTime + timeSpent));
                    else
                        eventQueue.add(new Event("QUANTUM_EXP", simulatorTime + quantum_size));
                    debug("CPU_SCHEDULER", currentProcess, "", simulatorTime);
                }
                case "QUANTUM_EXP" -> {
                    currentProcess = cpu.remove();
                    readyQueue.add(currentProcess);
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    debug("QUANTUM_EXP", currentProcess, "", simulatorTime);
                }
                case "IO_INTERRUPT" -> {
                    currentProcess = cpu.remove();
                    ioQueue.add(currentProcess);
                    int interruptTime = (int) randomExponentialNumber(avg_io_interrupt);
                    currentProcess.setElapsedIOTime(currentProcess.getElapsedIOTime() + interruptTime);
                    currentProcess.setIoRequests(currentProcess.getIoRequests() + 1);
                    eventQueue.add(new Event("IO_COMPLETE", simulatorTime + interruptTime));
                    debug("IO_INTERRUPT", currentProcess, "" + interruptTime, simulatorTime);
                }
                case "IO_COMPLETE" -> {
                    currentProcess = ioQueue.remove();
                    readyQueue.add(currentProcess);
                    if (currentProcess.isIoBound())
                        currentProcess.setRemainingCPUBurst(randomNumber(1000, 2000));
                    else
                        currentProcess.setRemainingCPUBurst(randomNumber(10000, 20000));
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    debug("IO_COMPLETE", currentProcess, "", simulatorTime);
                }
                case "PROCESS_DONE" -> {
                    ++contextSwitches;
                    currentProcess = cpu.remove();
                    completedProcesses.push(currentProcess);
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    currentProcess.getElapsedQueueTime(simulatorTime);
                    debug("PROCESS_DONE", currentProcess, "", simulatorTime);
                }
            }
        }

        if(DEBUG_LEVEL == 0){
            try{
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Statistics Start
        System.out.println("Finished Simulation!\n\nStatistics:");
        System.out.println("Simulation Time: " + simulatorTime/1000000.0 + " seconds");
        System.out.println("Created " + pid + " processes");
        System.out.println("Average CPU Time: " + cpuTimeSum/pid/1000000.0 + " seconds");
        System.out.println("CPU Utilization: " + Math.round((double)cpuTimeSum/simulatorTime*100.0) + "% ("
                + cpuTimeSum/1000000.0 + " seconds)");
        System.out.println("Total Time in Context Switches: " +
                contextSwitches*context_switch_time/1000000.0 + " seconds"); // NEED TO CHECK THIS

        //Overall Stats
        System.out.println("\nTotal Number of processes completed: " + completedProcesses.size());
        int ioProcesses = 0;
        long cpuTime = 0, readyWait = 0, turnaround = 0;
        for (Process p : completedProcesses) {
            if (p.isIoBound())
                ++ioProcesses;
            cpuTime += p.getElapsedCPUTime();
            readyWait += p.getElapsedQueueTime();
            turnaround += p.getTotalTime();
        }
        System.out.println("Ratio of IO-Bound Completed: " + Math.round((double)ioProcesses/completedProcesses.size()*100.0) + "%");
        System.out.println("Average CPU Time: " + cpuTime/completedProcesses.size()/1000000.0 + " seconds");
        System.out.println("Average Ready Waiting Time: " + readyWait/completedProcesses.size()/1000000.0 + " seconds");
        System.out.println("Average Turnaround Time: " + turnaround/completedProcesses.size()/1000000.0 + " seconds");

        cpuTime = 0;
        readyWait = 0;
        turnaround = 0;
        long ioWait = 0;
        int io = 0;
        for (Process p : completedProcesses) {
            if (p.isIoBound()){
                cpuTime += p.getElapsedCPUTime();
                readyWait += p.getElapsedQueueTime();
                ioWait += p.getElapsedIOTime();
                io += p.getIoRequests();
                turnaround += p.getTotalTime();
            }
        }
        //IO Statistics
        if (ioProcesses != 0) {
            System.out.println("\nNumber of IO-bounded processes completed: " + ioProcesses);
            System.out.println("Average CPU Time: " + cpuTime/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average Ready Waiting Time: " + readyWait/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average I/O Interrupt Time: " + ioWait/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average Turnaround Time: " + turnaround/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average I/O Operations per process: " + io/ioProcesses);
        } else {
            System.out.println("\nNumber of IO-bounded processes completed: 0");
            System.out.println("Average CPU Time: 0 seconds");
            System.out.println("Average Ready Waiting Time: 0 seconds");
            System.out.println("Average I/O Interrupt Time: 0 seconds");
            System.out.println("Average Turnaround Time: 0 seconds");
            System.out.println("Average I/O Operations per process: 0");
        }
        


        cpuTime = 0;
        readyWait = 0;
        turnaround = 0;
        ioWait = 0;
        io = 0;
        for (Process p : completedProcesses) {
            if (!p.isIoBound()){
                cpuTime += p.getElapsedCPUTime();
                readyWait += p.getElapsedQueueTime();
                ioWait += p.getElapsedIOTime();
                io += p.getIoRequests();
                turnaround += p.getTotalTime();
            }
        }

        //CPU Statistics
        System.out.println("\nNumber of CPU-bounded processes completed: " + Math.abs(completedProcesses.size() - ioProcesses));
        if (ioProcesses != 0) {
            System.out.println("Average CPU Time: " + cpuTime/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average Ready Waiting Time: " + readyWait/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average I/O Interrupt Time: " + ioWait/ioProcesses/1000000.0 + " seconds");
            System.out.println("Average Turnaround Time: " + turnaround/Math.abs(completedProcesses.size() - ioProcesses)/1000000.0 + " seconds");
            System.out.println("Average I/O Operations per process: " + io/ioProcesses);
        } else {
            System.out.println("Average CPU Time: " + cpuTime/1000000.0 + " seconds");
            System.out.println("Average Ready Waiting Time: " + readyWait/1000000.0 + " seconds");
            System.out.println("Average I/O Interrupt Time: " + ioWait/1000000.0 + " seconds");
            System.out.println("Average Turnaround Time: " + turnaround/Math.abs(completedProcesses.size() - ioProcesses)/1000000.0 + " seconds");
            System.out.println("Average I/O Operations per process: 0");
        }
        


        System.out.println("\nFinished Statistics Section!\nEnd of program");
    }

    private static int randomNumber(int startRange, int endRange){
        return r.nextInt((endRange - startRange) + 1) + startRange;
    }

    private static double randomExponentialNumber(int expected){
        return -(double)expected * Math.log(Math.random());
    }

    private static void debug(String event, Process p, String additional, long simulatorTime){
        String message = "";
        if(DEBUG_LEVEL == 0){
            message += "TIME: " + simulatorTime/1000000.0 + " (" + simulatorTime + ") EVENT: " + event + " ";
            switch (event) {
                case "READY_TO_CPU" -> message += "PID: "
                        + p.getPID() + " currentBurst: " + p.getRemainingCPUBurst() + " totalCPURem: "
                        + p.getRemainingCPUTime();
                case "CPU_SCHEDULER", "QUANTUM_EXP", "IO_COMPLETE" -> message += "PID: " + p.getPID();
                case "IO_INTERRUPT" -> message += "PID: " + p.getPID() + " IO-Interrupt: " + additional;
                case "PROCESS_DONE" -> message += "PID: " + p.getPID()
                        + " " + (p.isIoBound() ? "IO-Bound" : "CPU-Bound") + " totalCPU: " +
                        p.getElapsedCPUTime() + " waitReady: " + p.getElapsedQueueTime(simulatorTime) +
                        " IOTime: " + p.getElapsedIOTime() + " IORequests: " + p.getIoRequests();
                default -> message += additional;
            }
            System.out.println(message);
            try{
                br.write(message + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(DEBUG_LEVEL == 1){
            message += "TIME: " + simulatorTime/1000000.0 + " (" + simulatorTime + ") EVENT: " + event + " ";
            switch(event){
                case "NEW_PROCESS" -> {
                    message += additional;
                    System.out.println(message);
                }
                case "PROCESS_DONE" -> {
                    message += "PID: " + p.getPID()
                        + " " + (p.isIoBound() ? "IO-Bound" : "CPU-Bound") + " totalCPU: " +
                        p.getElapsedCPUTime() + " waitReady: " + p.getElapsedQueueTime() +
                        " IOTime: " + p.getElapsedIOTime() + " IORequests: " + p.getIoRequests();
                    System.out.println(message);
                }
            }

        }
    }

}
