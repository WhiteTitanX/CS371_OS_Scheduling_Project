import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class Main {

    private static Random r;

    public static void main(String[] args){
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
        PriorityQueue<Event> eventQueue = new PriorityQueue<Event>(50, (o1, o2) -> {
            return Long.compare(o1.getTime(), o2.getTime());
        });
        Queue<Process> readyQueue = new LinkedList<>();
        Queue<Process> ioQueue = new LinkedList<>();

        Queue<Process> cpu = new LinkedList<>();

        eventQueue.add(new Event("NEW_PROCESS", 0));

        while(simulatorTime < total_simulation_time && eventQueue.peek() != null){
            Event currentEvent = eventQueue.poll();
            simulatorTime = currentEvent.getTime();

            switch(currentEvent.getType()){
                case "NEW_PROCESS":
                    long new_process_creation_time = (long) (randomExponentialNumber(avg_process_creation) + simulatorTime);
                    int total_cpu_time = (int) randomExponentialNumber(avg_process_length);
                    int current_cpu_burst;
                    if(randomNumber(1, 100) < percent_io_jobs)
                        current_cpu_burst = randomNumber(1000, 2000);
                    else
                        current_cpu_burst = randomNumber(10000, 20000);
                    readyQueue.add(new Process(pid++, total_cpu_time, current_cpu_burst));
                    eventQueue.add(new Event("NEW_PROCESS", new_process_creation_time));
                    if(cpu.size() == 0)
                        eventQueue.add(new Event("READY_TO_CPU", simulatorTime + context_switch_time));
                    debug(0, "NEW_PROCESS", null, (pid - 1) + " total_cpu_time: " +
                            total_cpu_time + " " + (current_cpu_burst < 5000 ? "IO-Bound" : "CPU-Bound")
                            + " nextProcessAt: " + new_process_creation_time/1000000.0, simulatorTime);
                    break;
                case "READY_TO_CPU":
                    Process p;
                    cpu.add(p = readyQueue.remove());
                    eventQueue.add(new Event("CPU_SCHEDULER", simulatorTime + context_switch_time));
                    debug(0, "READY_TO_CPU", p, "", simulatorTime);
                    break;
                case "CPU_SCHEDULER":
                    Process working = cpu.peek();
                    if(working == null)
                        return;
                    working.setRemainingCPUBurst(working.getRemainingCPUBurst() - quantum_size);
                    working.setRemainingCPUTime(working.getRemainingCPUTime() - quantum_size);
                    working.setElapsedCPUTime(working.getElapsedCPUTime() + quantum_size);

                    if(working.getRemainingCPUTime() <= 0)
                        eventQueue.add(new Event("PROCESS_DONE", simulatorTime + context_switch_time));
                    else if(working.getRemainingCPUBurst() <= 0)
                        eventQueue.add(new Event("IO_INTERRUPT", simulatorTime));
                    else
                        eventQueue.add(new Event("QUANTUM_EXP", simulatorTime));
                    debug(0, "CPU_SCHEDULER", working, "", simulatorTime);
                    break;
                case "QUANTUM_EXP":
                    Process cpuProcess = cpu.remove();
                    readyQueue.add(cpuProcess);
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    debug(0, "QUANTUM_EXP", cpuProcess, "", simulatorTime);
                    break;
                case "IO_INTERRUPT":
                    Process cpuProcess2 = cpu.remove();
                    ioQueue.add(cpuProcess2);
                    int interruptTime = (int) randomExponentialNumber(avg_io_interrupt);
                    cpuProcess2.setElapsedIOTime(cpuProcess2.getElapsedIOTime() + interruptTime);
                    cpuProcess2.setIoRequests(cpuProcess2.getIoRequests() + 1);
                    eventQueue.add(new Event("IO_COMPLETE", simulatorTime + interruptTime));
                    debug(0, "IO_INTERRUPT", cpuProcess2, ""+interruptTime, simulatorTime);
                    break;
                case "IO_COMPLETE":
                    Process ioProcess = ioQueue.remove();
                    readyQueue.add(ioProcess);
                    if(ioProcess.isIoBound())
                        ioProcess.setRemainingCPUBurst(randomNumber(1000, 2000));
                    else
                        ioProcess.setRemainingCPUBurst(randomNumber(10000, 20000));
                    eventQueue.add(new Event("READY_TO_CPU", simulatorTime));
                    debug(0, "IO_COMPLETE", ioProcess, "", simulatorTime);
                    break;
                case "PROCESS_DONE":
                    Process finished = cpu.remove();
                    debug(0, "PROCESS_DONE", finished, "", simulatorTime);
                    break;
            }
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
        if(level == 0){
            switch(event){
                case "NEW_PROCESS":
                    System.out.println("TIME: " + simulatorTime + " EVENT: NEW_PROCESS " + additional);
                    break;
                case "READY_TO_CPU":
                    System.out.println("TIME: " + simulatorTime + " EVENT: Ready -> CPU PID: "
                            + p.getPID() + " currentBurst: " + p.getRemainingCPUBurst()/1000000.0 + " totalCPURem: "
                            + p.getRemainingCPUTime()/1000000.0);
                    break;
                case "CPU_SCHEDULER":
                    System.out.println("TIME: " + simulatorTime + " EVENT: CPU_SCHEDULER PID: " + p.getPID());
                    break;
                case "QUANTUM_EXP":
                    System.out.println("TIME: " + simulatorTime + " EVENT: QUANTUM_EXP PID: "
                            + p.getPID());
                    break;
                case "IO_INTERRUPT":
                    System.out.println("TIME: " + simulatorTime + " EVENT: IO_INTERRUPT PID: " + p.getPID()
                            + " IO-Interrupt: " + additional);
                    break;
                case "IO_COMPLETE":
                    System.out.println("TIME: " + simulatorTime + " EVENT: IO_COMPLETE PID: " + p.getPID());
                    break;
                case "PROCESS_DONE":
                    System.out.println("TIME: " + simulatorTime + " EVENT: PROCESS_DONE PID: " + p.getPID()
                            + " " + (p.isIoBound() ? "IO-Bound" : "CPU-Bound") + " totalCPU: " +
                            p.getElapsedCPUTime() + " waitReady: " + p.getElapsedQueueTime() +
                            " IOTime: " + p.getElapsedIOTime() + " IORequests: " + p.getIoRequests());
                    break;
            }
        }
    }

}
