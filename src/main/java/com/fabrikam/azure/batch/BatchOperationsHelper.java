package com.fabrikam.azure.batch;

/**
 * Sample class demonstrating the use of the BatchClient object to interface with Azure
 * batch pools, jobs, and task definitions.
 *
 */


import com.microsoft.azure.batch.BatchClient;
import com.microsoft.azure.batch.auth.BatchSharedKeyCredentials;
import com.microsoft.azure.batch.protocol.models.*;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.joda.time.Period;

public class BatchOperationsHelper {

    BatchClient batchClient;

    // batchclient expects a https:// URI to the batch account , the name of the batch acccount, 
    // and a key (either primary or secondary) obtained from either the portal or the BatchAccount API
    BatchOperationsHelper(String accountUri, String batchAcctName, String batchAccountKey) {
        BatchSharedKeyCredentials credentials = new BatchSharedKeyCredentials(accountUri, batchAcctName,
                batchAccountKey);
        batchClient = BatchClient.open(credentials);
    }

    // create a compute pool using a CloudServiceConfiguration object to define the makeup of the pool members.
    // 
    public CloudPool createPool(String poolId, String virtualMachineSize, int dedicatedVMs, String osFamily,
            String osVersion) {
        // create a pool using the strings and a CloudServiceConfiguration object
        try {

            // create a cloudservices object representing a VM in this pool
            CloudServiceConfiguration csc = new CloudServiceConfiguration().withOsFamily(osFamily)
                    .withTargetOSVersion(osVersion);

            // create the pool
            batchClient.poolOperations().createPool(poolId, virtualMachineSize, csc, dedicatedVMs);

            // Wait for the pool to be ready before returning the pool for use
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            boolean steady = false;

            while (elapsedTime < 300000) {
                CloudPool pool = batchClient.poolOperations().getPool(poolId);
                if (pool.allocationState() == AllocationState.STEADY) {
                    steady = true;
                    break;
                }
                System.out.println("wait 30 seconds for pool steady...");
                Thread.sleep(30 * 1000);
                elapsedTime = (new Date()).getTime() - startTime;
            }
            if (!steady) {
                throw new TimeoutException("The pool did not reach a steady state in the allotted time");
            }

            // verify pool is created, then get and return pool
            if (batchClient.poolOperations().existsPool(poolId)) {
                return batchClient.poolOperations().getPool(poolId);
            }

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // create a pool using a virtual machine configuration object to define the make up of the VMs
    // in the pool.
    public CloudPool createPool(String poolId, String virtualMachineSize, String offer, String publisher,
            String imageSku, String version, String nodeAgentSKU, int poolSize) {
        // create a pool using the simple strings and a VirtualMachineConfiguration and ImageReference object
        try {

            // get values from `az vm image list` using azure CLI 
            ImageReference batchPoolImgReference = new ImageReference().withOffer(offer).withPublisher(publisher)
                    .withSku(imageSku).withVersion(version);

            // create a virtual machine object representing the structure of the VMs in this pool
            VirtualMachineConfiguration vmc = new VirtualMachineConfiguration()
                    .withImageReference(batchPoolImgReference).withNodeAgentSKUId(nodeAgentSKU);

            // use the BatchClient.poolOperations().createPool method to set up the pool in the account
            batchClient.poolOperations().createPool(poolId, virtualMachineSize, vmc, poolSize);

            // Wait for the pool to be ready before returning the pool for use
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            boolean steady = false;

            while (elapsedTime < 300000) {
                CloudPool pool = batchClient.poolOperations().getPool(poolId);
                if (pool.allocationState() == AllocationState.STEADY) {
                    steady = true;
                    break;
                }
                System.out.println("wait 30 seconds for pool steady...");
                Thread.sleep(30 * 1000);
                elapsedTime = (new Date()).getTime() - startTime;
            }

            if (!steady) {
                throw new TimeoutException("The pool did not reach a steady state in the allotted time");
            }

            // verify pool is created, then get and return pool
            if (batchClient.poolOperations().existsPool(poolId)) {
                return batchClient.poolOperations().getPool(poolId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
        return null;
    }

    // get a batch pool by name
    public CloudPool getPool(String poolName) {
        try {
            List<CloudPool> batchPoolList = batchClient.poolOperations().listPools();
            for (CloudPool pool : batchPoolList) {
                System.out.println("Found cloud pool with ID: " + pool.id() + " and name " + pool.displayName());
                if (pool.id().equalsIgnoreCase(poolName)) {
                    return pool;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("No pool found!");
        return null;
    }

    // add or remove dedicated nodes to a batch pool
    public void resizePool(String poolId, int dedicatedNodes) {
        try {
            batchClient.poolOperations().resizePool(poolId, dedicatedNodes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // delete a batch pool
    public void deletePool(String poolId) {
        try {
            batchClient.poolOperations().deletePool(poolId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // create an empty job-you can define a task into the definition later, or attach one using the 
    // methods out of  JobAddParameter such as withJobManagerTask()
    public void createJob(String jobId, String poolId) {
        try {

            // The JobAddParameter object fluently defines the job, and the associated PoolInformation object defines which pool
            // the job runs in  
            PoolInformation jobPoolInfo = new PoolInformation().withPoolId(poolId);
            JobAddParameter jobParams = new JobAddParameter()
                .withId(jobId)
                .withPoolInfo(jobPoolInfo);
            batchClient.jobOperations().createJob(jobParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // add a task to a job definition
    public void addTask(String jobId, String commandLine, String displayName, String taskId) {
        try {

            // create a TaskAddParameter object to define the properties of the task. You can create additional objects
            // to customize the task behavoir, such as runtime constraints and conditions and attach them to the definition
            // using the appropriate .with method call on the TaskAddParameter object 
            TaskAddParameter taskParams = new TaskAddParameter().withCommandLine(commandLine)
                    .withDisplayName(displayName)
                    .withId(taskId);

            // Use the taskOperations() entry point from the BatchClient class to add the task to the job.
            batchClient.taskOperations().createTask(jobId, taskParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // get a list of all defined jobs in a batch account
    public List<CloudJob> getJobs() {
        try {
            return batchClient.jobOperations().listJobs();
        } catch (Exception e) {

            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
    }
     

    // delete a job by ID in a batch account
    public void deleteJob(String jobId) {
        try {
            batchClient.jobOperations().deleteJob(jobId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    // create a job that runs on a schedule. Create a JobScheduleAddParamter object to configure the scheduled job,
    // then use the jobScheduleOperations() endpoint from the BatchClient object to create it.
    public void createJobSchedule(String scheduleDisplayName, String jobSchedId, String poolId, String jobId,
            String jobDisplayName, String commandLine, String taskDisplayName, String taskId) {
        // create an interval for the job to run. Scheduled tasks by time and date are also available as options for the Period constructor.
        Period recurrenceInterval = new Period(1, 30, 0, 0); // hours, minutes, seconds, milliseconds
        Schedule schedule = new Schedule().withRecurrenceInterval(recurrenceInterval);

        // define the job specification and attach a task, similar to the job creation without a schedule
        PoolInformation jobPoolInfo = new PoolInformation().withPoolId(poolId);
        JobManagerTask scheduledTask = new JobManagerTask().withCommandLine(commandLine)
                .withDisplayName(taskDisplayName).withId(taskId);

        JobSpecification jobSpec = new JobSpecification().withDisplayName(jobDisplayName).withPoolInfo(jobPoolInfo)
                .withJobManagerTask(scheduledTask);

        // create the scheduled job
        JobScheduleAddParameter jobSchedule = new JobScheduleAddParameter().withDisplayName(scheduleDisplayName)
                .withId(jobSchedId).withSchedule(schedule).withJobSpecification(jobSpec);
        try {
            batchClient.jobScheduleOperations().createJobSchedule(jobSchedule);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
