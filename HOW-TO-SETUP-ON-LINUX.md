# Setting Up the Twitter-Discord Processor as a Cron Job

This guide explains how to install your Java application on a Linux server using artifacts built by GitHub Actions and configure it to run automatically every 20 minutes using cron.

## Prerequisites

1.  **Linux Server Access:** You need SSH access (or direct terminal access) to the Linux server where you want to run the application.
2.  **Java Installation:** Java Development Kit (JDK) or Java Runtime Environment (JRE) version 21 or higher must be installed on the server. Check with `java -version`. Install if necessary (e.g., `sudo apt update && sudo apt install openjdk-21-jre` or `sudo yum install java-21-openjdk`).
3.  **GitHub Repository Access:** You need access to your GitHub repository to download build artifacts.
4.  **`unzip` Utility:** The `unzip` command might be needed on the server to extract the downloaded artifact. Install if necessary (e.g., `sudo apt install unzip` or `sudo yum install unzip`).

## Installation Steps

1.  **Create an Application Directory:**
    Choose a location on your server.
    ```bash
    # Example: Create a directory in /opt
    sudo mkdir -p /opt/twitter-discord-processor
    sudo chown your_linux_user:your_linux_group /opt/twitter-discord-processor # Optional: Change ownership
    cd /opt/twitter-discord-processor
    ```
    Replace `your_linux_user:your_linux_group` if needed.

2.  **Download the Build Artifact:**
    * Go to your repository on GitHub.
    * Click on the "**Actions**" tab.
    * Find the latest successful run of the "**Java CI Build and Package**" workflow (usually from a push to the `main` branch).
    * Click on the workflow run title to view its summary page.
    * Scroll down to the "**Artifacts**" section.
    * You should see an artifact named `twitter-discord-processor-jar`. Click on it to download the ZIP file.

3.  **Transfer and Extract the Artifact:**
    * Transfer the downloaded ZIP file (e.g., `twitter-discord-processor-jar.zip`) to your server, placing it in the application directory (`/opt/twitter-discord-processor/`). You can use `scp` or other methods.
    * SSH into your server and navigate to the application directory:
      ```bash
      cd /opt/twitter-discord-processor
      ```
    * **Important:** Remove any older versions of the JAR file first to avoid conflicts:
      ```bash
      rm -f twitter-discord-processor-*.jar
      ```
    * Unzip the artifact archive:
      ```bash
      unzip twitter-discord-processor-jar.zip
      ```
      This should extract the versioned JAR file (e.g., `twitter-discord-processor-1.0.123.jar`).
    * You can remove the downloaded ZIP file now:
      ```bash
      rm twitter-discord-processor-jar.zip
      ```

## Configuration Steps

1.  **Create/Update the Wrapper Script:**
    In your application directory (`/opt/twitter-discord-processor`), create or edit the script `run_processor.sh`:
    ```bash
    nano run_processor.sh
    ```
    Paste/update the following content, **replacing placeholders** and ensuring the JAR file finding logic is correct:

    ```bash
    #!/bin/bash

    # --- Configuration ---
    # !! IMPORTANT: Set your actual secrets here !!
    export TWITTER_BEARER_TOKEN="YOUR_ACTUAL_TWITTER_BEARER_TOKEN"
    export DISCORD_BOT_TOKEN="YOUR_ACTUAL_DISCORD_BOT_TOKEN"
    export TWITCH_CLIENT_ID="YOUR_ACTUAL_TWITCH_CLIENT_ID"
    export TWITCH_CLIENT_SECRET="YOUR_ACTUAL_TWITCH_CLIENT_SECRET"
    # Optional: Set TWITTER_USERNAME here if you don't want to rely on config.properties
    # export TWITTER_USERNAME="your_twitter_username"

    # --- Application Execution ---
    # Get the directory where the script is located
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

    # Find the application JAR file (handles changing version numbers)
    # This assumes only ONE such JAR exists in the directory after cleanup/unzip
    APP_JAR=$(ls "$SCRIPT_DIR"/twitter-discord-processor-*.jar | head -n 1)

    # Check if JAR file was found
    if [ -z "$APP_JAR" ] || [ ! -f "$APP_JAR" ]; then
        echo "ERROR: Application JAR file not found in $SCRIPT_DIR matching twitter-discord-processor-*.jar at $(date)"
        exit 1
    fi

    # Define the data directory (can be relative to script or absolute)
    DATA_DIR="$SCRIPT_DIR/data"
    # Optional: Specify max tweets (e.g., 20)
    # MAX_TWEETS=20

    echo "Starting Twitter-Discord Processor (JAR: $APP_JAR) at $(date)"
    # Run the Java application using the found JAR
    # Pass the data directory path and optionally max tweets
    java -jar "$APP_JAR" "$DATA_DIR" # Add $MAX_TWEETS here if using it

    # Optional: Check exit code
    EXIT_CODE=$?
    if [ $EXIT_CODE -ne 0 ]; then
        echo "Application finished with error code: $EXIT_CODE at $(date)"
    else
        echo "Application finished successfully at $(date)"
    fi

    exit $EXIT_CODE
    ```

2.  **Make the Script Executable:**
    ```bash
    chmod +x run_processor.sh
    ```

## Cron Job Setup

1.  **Edit Crontab:**
    ```bash
    crontab -e
    ```

2.  **Add the Cron Job Line:**
    Ensure the path to `run_processor.sh` is correct.

    ```crontab
    # Run Twitter-Discord Processor every 20 minutes
    */20 * * * * /opt/twitter-discord-processor/run_processor.sh >> /opt/twitter-discord-processor/cron.log 2>&1
    ```

3.  **Save and Exit.**

4.  **Verify (Optional):**
    ```bash
    crontab -l
    ```

## Logging and Monitoring

* **Application Logs:** Check the `logs` subdirectory within your data directory (e.g., `/opt/twitter-discord-processor/data/logs/`).
* **Cron Output Log:** Check the `cron.log` file specified in the crontab line (e.g., `/opt/twitter-discord-processor/cron.log`).
* **System Cron Logs:** Check system logs like `/var/log/syslog` or `/var/log/cron`.

Now your setup process involves downloading the latest build artifact from GitHub Actions instead of manually copying a locally built JAR. Remember to repeat the download/transfer/extract steps whenever you want to deploy a new version built by the Actions workflow.
