package com.apigateway.cleanup;

import com.apigateway.metrics.MetricsService;
import com.apigateway.repository.ClientStateRepository;
import com.apigateway.repository.ClientState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Periodically removes inactive clients from the repository and 
 * updates the active active client
 */
public class CleanupScheduler {
	
	private final ClientStateRepository<? extends ClientState> repository;	// Repository containing client state information.
    private final MetricsService metricsService;	// Service user to update metrics
    private final long inactivityThresholdMs;		// Maximum allowed inactive duration before client is removed
    private final ScheduledExecutorService scheduler;	// Single threaded scheduler responsible for running cleanup tasks.
    
    /**
     * Creates and starts the cleanup scheduler.
     *
     * @param repository Repository containing client states
     * @param metricsService Service used to update metrics
     * @param inactivityThresholdMs Time in milliseconds after which a client
     *                              is considered inactive
     * @param intervalMs Cleanup execution interval in milliseconds
     */
    public CleanupScheduler(ClientStateRepository<? extends ClientState> repository, MetricsService metricsService, long inactivityThresholdMs, long intervalMs) {
    	
    	 this.repository            = repository;
         this.metricsService        = metricsService;
         this.inactivityThresholdMs = inactivityThresholdMs;
         
         this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable ->{
        	Thread thread = new Thread(runnable, "cleanup-scheduler");
        	thread.setDaemon(true);
        	return thread;
         });
         
      // Schedule the cleanup task to run repeatedly at a fixed interval.
         this.scheduler.scheduleAtFixedRate(
                 this::cleanup,
                 intervalMs,   
                 intervalMs,   
                 TimeUnit.MILLISECONDS
             );
    }
    
    /**
     * Remove inactive client and updates the active client count metrics.
     */
    private void cleanup() {
    	long cutoff = System.currentTimeMillis() - inactivityThresholdMs;	// Calculate the cutoff timestamp.
    	repository.removeInactiveClient(cutoff);							// Remove inactive clients from the repository
        metricsService.setActiveClients(repository.Size());    	    		// Update metrics with the current number of active clients.
    }
    
    /*
     * Gracefully shuts down the scheduler.
     * Waits up to 5 seconds for any running task to complete.
     * If the scheduler does not terminate in time, it is forcefully stopped.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();	// Force shutdown if tasks are still running.
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();		// Force shutdown and restore interrupt status.
            Thread.currentThread().interrupt();
        }
    }

}
