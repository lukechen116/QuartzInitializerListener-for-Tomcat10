package quartz;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzInitializerListener implements ServletContextListener {

    public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";

    private boolean performShutdown = true;
    private boolean waitOnShutdown = false;

    private Scheduler scheduler = null;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public void contextInitialized(ServletContextEvent sce) {

        log.info("Quartz Initializer Servlet loaded, initializing Scheduler...");

        ServletContext servletContext = sce.getServletContext();
        StdSchedulerFactory factory;
        try {

            String configFile = servletContext.getInitParameter("quartz:config-file");
            if(configFile == null)
                configFile = servletContext.getInitParameter("config-file"); // older name, for backward compatibility
            String shutdownPref = servletContext.getInitParameter("quartz:shutdown-on-unload");
            if(shutdownPref == null)
                shutdownPref = servletContext.getInitParameter("shutdown-on-unload");
            if (shutdownPref != null) {
                performShutdown = Boolean.valueOf(shutdownPref).booleanValue();
            }
            String shutdownWaitPref = servletContext.getInitParameter("quartz:wait-on-shutdown");
            if (shutdownWaitPref != null) {
                waitOnShutdown = Boolean.valueOf(shutdownWaitPref).booleanValue();
            }

            factory = getSchedulerFactory(configFile);

            // Always want to get the scheduler, even if it isn't starting, 
            // to make sure it is both initialized and registered.
            scheduler = factory.getScheduler();

            // Should the Scheduler being started now or later
            String startOnLoad = servletContext.getInitParameter("quartz:start-on-load");
            if(startOnLoad == null)
                startOnLoad = servletContext.getInitParameter("start-scheduler-on-load");

            int startDelay = 0;
            String startDelayS = servletContext.getInitParameter("quartz:start-delay-seconds");
            if(startDelayS == null)
                startDelayS = servletContext.getInitParameter("start-delay-seconds");
            try {
                if(startDelayS != null && startDelayS.trim().length() > 0)
                    startDelay = Integer.parseInt(startDelayS);
            } catch(Exception e) {
                log.error("Cannot parse value of 'start-delay-seconds' to an integer: " + startDelayS + ", defaulting to 5 seconds.");
                startDelay = 5;
            }

            /*
             * If the "quartz:start-on-load" init-parameter is not specified,
             * the scheduler will be started. This is to maintain backwards
             * compatability.
             */
            if (startOnLoad == null || (Boolean.valueOf(startOnLoad).booleanValue())) {
                if(startDelay <= 0) {
                    // Start now
                    scheduler.start();
                    log.info("Scheduler has been started...");
                }
                else {
                    // Start delayed
                    scheduler.startDelayed(startDelay);
                    log.info("Scheduler will start in " + startDelay + " seconds.");
                }
            } else {
                log.info("Scheduler has not been started. Use scheduler.start()");
            }

            String factoryKey = servletContext.getInitParameter("quartz:servlet-context-factory-key");
            if(factoryKey == null)
                factoryKey = servletContext.getInitParameter("servlet-context-factory-key");
            if (factoryKey == null) {
                factoryKey = QUARTZ_FACTORY_KEY;
            }

            log.info("Storing the Quartz Scheduler Factory in the servlet context at key: "
                    + factoryKey);
            servletContext.setAttribute(factoryKey, factory);
            
            
            String servletCtxtKey = servletContext.getInitParameter("quartz:scheduler-context-servlet-context-key");
            if(servletCtxtKey == null)
                servletCtxtKey = servletContext.getInitParameter("scheduler-context-servlet-context-key");
            if (servletCtxtKey != null) {
                log.info("Storing the ServletContext in the scheduler context at key: "
                        + servletCtxtKey);
                scheduler.getContext().put(servletCtxtKey, servletContext);
            }

        } catch (Exception e) {
            log.error("Quartz Scheduler failed to initialize: " + e.toString());
            e.printStackTrace();
        }
    }

    protected StdSchedulerFactory getSchedulerFactory(String configFile)
            throws SchedulerException {
        StdSchedulerFactory factory;
        // get Properties
        if (configFile != null) {
            factory = new StdSchedulerFactory(configFile);
        } else {
            factory = new StdSchedulerFactory();
        }
        return factory;
    }

    public void contextDestroyed(ServletContextEvent sce) {

        if (!performShutdown) {
            return;
        }

        try {
            if (scheduler != null) {
                scheduler.shutdown(waitOnShutdown);
            }
        } catch (Exception e) {
            log.error("Quartz Scheduler failed to shutdown cleanly: " + e.toString());
            e.printStackTrace();
        }

        log.info("Quartz Scheduler successful shutdown.");
    }


}
