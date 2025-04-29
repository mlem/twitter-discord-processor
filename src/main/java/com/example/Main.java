package com.example;

import com.example.args.CommandLineArgs;
import com.example.config.AppConfig;
import com.example.core.ApplicationService;
import com.example.core.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Main entry point for the Twitter-Discord Processor application.
 * Initializes configuration, services, and runs the main processing cycle.
 * Delegates shutdown to the ServiceRegistry.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Application starting...");
        ServiceRegistry serviceRegistry = null; // Hold registry for shutdown access

        try {
            // 1. Parse Command Line Arguments
            CommandLineArgs cliArgs = CommandLineArgs.parse(args);

            // 2. Load and Validate Configuration
            // AppConfig.loadAndValidate handles loading properties, env vars, and validation
            Optional<AppConfig> configOpt = AppConfig.loadAndValidate();
            if (!configOpt.isPresent()) {
                // Error already logged by AppConfig's validator
                System.exit(1); // Exit if config is invalid
            }
            AppConfig appConfig = configOpt.get();

            // 3. Initialize Services via Registry
            // ServiceRegistry constructor handles directory/logging setup and service instantiation
            // It takes the validated config and parsed args
            serviceRegistry = new ServiceRegistry(appConfig, cliArgs);

            // 4. Initialize Application Service
            // Pass the config, args, and registry to the service that orchestrates the work
            ApplicationService appService = new ApplicationService(appConfig, cliArgs, serviceRegistry);

            // 5. Run the Main Application Logic Cycle
            appService.runCycle();

            logger.info("Main processing cycle completed successfully.");

        } catch (Exception e) {
            // Catch critical errors during setup (e.g., directory creation, JDA login) or execution
            logger.error("A critical unhandled error occurred: {}. Application will exit.", e.getMessage(), e);
            System.exit(1); // Exit on critical failure
        } finally {
            // --- Shutdown ---
            logger.info("Initiating shutdown sequence...");
            // Delegate shutdown logic to the ServiceRegistry
            if (serviceRegistry != null) {
                try {
                    serviceRegistry.shutdown(); // Call the registry's shutdown method
                } catch (Exception e) {
                    // Catch potential errors during the overall shutdown process
                    logger.error("Error during ServiceRegistry shutdown: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("ServiceRegistry was not initialized, cannot perform service shutdown.");
            }
            logger.info("Application finished.");
        }
    }

    // Removed isNullOrBlank helper - logic moved to validator/config
}
