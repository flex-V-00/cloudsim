import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;

import java.util.*;

public class PriorityTaskScheduler {

    
    static class PriorityCloudlet extends Cloudlet {
        int priority; // ***0 = High***, ***1 = Medium***, ***2 = Low***

        public PriorityCloudlet(int cloudletId, long length, int pesNumber, UtilizationModel utilizationModel, int priority) {
            super(cloudletId, length, pesNumber, 300, 300, utilizationModel, utilizationModel, utilizationModel);
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }


    static class PriorityDatacenterBroker extends DatacenterBroker {

        public PriorityDatacenterBroker(String name) throws Exception {
            super(name);
        }

        @Override
        public void submitCloudletList(List<? extends Cloudlet> list) {
            System.out.println("\n--- Cloudlets BEFORE sorting (by priority) ---");
            for (Cloudlet cl : list) {
                System.out.println("Cloudlet ID: " + cl.getCloudletId() + " | Priority: " + ((PriorityCloudlet) cl).getPriority());
            }

            list.sort(Comparator.comparingInt(c -> ((PriorityCloudlet) c).getPriority())); // sort by priority

            System.out.println("\n--- Cloudlets AFTER sorting (by priority) ---");
            for (Cloudlet cl : list) {
                System.out.println("Cloudlet ID: " + cl.getCloudletId() + " | Priority: " + ((PriorityCloudlet) cl).getPriority());
            }

            super.submitCloudletList(list);
        }
    }

    public static void main(String[] args) {
        try {
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            CloudSim.init(numUsers, calendar, traceFlag);

            Datacenter datacenter = createDatacenter("Datacenter_1");
            PriorityDatacenterBroker broker = new PriorityDatacenterBroker("Broker");

            List<Vm> vmList = createVMs(broker.getId(), 3); // 3 VMs
            List<PriorityCloudlet> cloudletList = createPriorityCloudlets(broker.getId(), 8); // 8 tasks

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);

            CloudSim.stopSimulation();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000))); // 1 CPU core

        hostList.add(new Host(
                0,
                new RamProvisionerSimple(4096), // 4GB RAM
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerTimeShared(peList)
        ));

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList,
                10.0, 3.0, 0.05, 0.1, 0.1
        );

        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }

    private static List<Vm> createVMs(int userId, int numVMs) {
        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < numVMs; i++) {
            Vm vm = new Vm(i, userId, 1000, 1, 1024, 1000, 1000,
                    "Xen", new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }
        return vmList;
    }

    private static List<PriorityCloudlet> createPriorityCloudlets(int userId, int numCloudlets) {
        List<PriorityCloudlet> list = new ArrayList<>();
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Random random = new Random(System.nanoTime()); // true randomness

        for (int i = 0; i < numCloudlets; i++) {
            int priority = random.nextInt(3); // 0 = High, 1 = Medium, 2 = Low
            long length = 40000 + random.nextInt(20000); // task duration varies
            PriorityCloudlet cloudlet = new PriorityCloudlet(i, length, 1, utilizationModel, priority);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }

        return list;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        System.out.println("\n========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Priority" + indent + "Data center ID" +
                indent + "VM ID" + indent + "Time");

        for (Cloudlet cloudlet : list) {
            PriorityCloudlet pcl = (PriorityCloudlet) cloudlet;
            System.out.print(pcl.getCloudletId() + indent);

            if (pcl.getStatus() == Cloudlet.SUCCESS) {
                System.out.print("SUCCESS" + indent);
                System.out.print(pcl.getPriority() + indent);
                System.out.print(pcl.getResourceId() + indent);
                System.out.print(pcl.getVmId() + indent);
                System.out.printf("%.2f\n", pcl.getActualCPUTime());
                Log.printLine("CI/CD done successfully");
                Log.printLine("CI/CD done successfully");
            } else {
                System.out.println("FAILED");
            }
        }
    }
}
