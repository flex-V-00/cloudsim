import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;

import java.util.*;

public class PriorityTaskScheduler {

    static class PriorityCloudlet extends Cloudlet {
        int priority;

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

        
        public void submitCloudletList(List<? extends Cloudlet> list) {
            list.sort(Comparator.comparingInt(c -> ((PriorityCloudlet) c).getPriority()));
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

            List<Vm> vmList = createVMs(broker.getId(), 2);
            List<PriorityCloudlet> cloudletList = createPriorityCloudlets(broker.getId(), 5);

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

        peList.add(new Pe(0, new PeProvisionerSimple(1000))); // one PE

        hostList.add(new Host(
                0,
                new RamProvisionerSimple(2048),
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerTimeShared(peList)
        ));

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList,
                10.0, 3.0, 0.05, 0.1, 0.1
        );

        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<Storage>(), 0);
    }


    private static List<Vm> createVMs(int userId, int numVMs) {
        List<Vm> vmList = new ArrayList<>();

        for (int i = 0; i < numVMs; i++) {
            Vm vm = new Vm(i, userId, 1000, 1, 512, 1000, 1000,
                    "Xen", new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }
        return vmList;
    }

    
    private static List<PriorityCloudlet> createPriorityCloudlets(int userId, int numCloudlets) {
        List<PriorityCloudlet> list = new ArrayList<>();
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < numCloudlets; i++) {
            int priority = (int) (Math.random() * 3); // 0 = High, 1 = Medium, 2 = Low
            PriorityCloudlet cloudlet = new PriorityCloudlet(i, 40000, 1, utilizationModel, priority);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }
        return list;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" +
                indent + "VM ID" + indent + "Time");

        for (Cloudlet cloudlet : list) {
            System.out.print(cloudlet.getCloudletId() + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.print("SUCCESS");
                System.out.print(indent + cloudlet.getResourceId());
                System.out.print(indent + cloudlet.getVmId());
                System.out.printf(indent + "%.2f\n", cloudlet.getActualCPUTime());
            } else {
                System.out.println("FAILED");
            }
        }
    }
}
