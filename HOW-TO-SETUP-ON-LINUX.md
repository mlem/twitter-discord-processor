# Setting Up the Twitter-Discord Processor as a Cron Job

This guide explains how to install your Java application on a Linux server using artifacts built by GitHub Actions (or a locally built JAR) and configure it to run automatically every 20 minutes using cron.

## Prerequisites

1.  **Linux Server Access:** You need SSH access (or direct terminal access) to the Linux server where you want to run the application.
2.  **Java Installation:** Java Development Kit (JDK) or Java Runtime Environment (JRE) version 21 or higher must be installed on the server. Check with `java -version`. Install if necessary (e.g., `sudo apt update && sudo apt install openjdk-21-jre` or `sudo yum install java-21-openjdk`).
3.  **Application JAR:** The executable JAR file (e.g., `twitter-discord-processor-1.0.123.jar`). You can get this from the GitHub Actions artifacts (see README) or by building locally (`mvn clean package`).
4.  **`unzip` Utility (If using GitHub Artifacts):** The `unzip` command might be needed on the server. Install if necessary (e.g., `sudo apt install unzip` or `sudo yum install unzip`).

## Installation Steps

1.  **Create an Application Directory:**
    Choose a location on your server to store the application files.
    ```bash
    # Example: Create a directory in /opt
    sudo mkdir -p /opt/twitter-discord-processor
    sudo chown your_linux_user:your_linux_group /opt/twitter-discord-processor # Optional: Change ownership
    cd /opt/twitter-discord-processor
    ```
    Replace `your_linux_user:your_linux_group` if needed.

2.  **Place the JAR File:**
    * **If using GitHub Actions artifact:** Download the artifact ZIP, transfer it to `/opt/twitter-discord-processor/`, remove any old JARs (`rm -f twitter-discord-processor-*.jar`), and unzip the new one (`unzip twitter-discord-processor-jar.zip`). Remove the zip file afterwards (`rm twitter-discord-processor-jar.zip`).
    * **If using locally built JAR:** Transfer the JAR file (e.g., `target/twitter-discord-processor-X.Y.Z.jar`) to `/opt/twitter-discord-processor/`.

3.  **Create the External `config.properties` File:**
    In the *same directory* as the JAR file (e.g., `/opt/twitter-discord-processor/`), create the `config.properties` file:
    ```bash
    nano config.properties
    ```
    Paste the required content and fill in your values:
    ```properties
    # Discord Configuration
    discord.channel.id=YOUR_DISCORD_CHANNEL_ID_HERE

    # Twitch Configuration
    # Username of the Twitch streamer whose logo should be used
    twitch.username=TARGET_TWITCH_USERNAME_HERE

    # Twitter Configuration (Fallback if TWITTER_USERNAME ENV var not set)
    twitter.username=YOUR_DEFAULT_TWITTER_USERNAME_HERE
    ```
    Save and close the file (Ctrl+X, Y, Enter in nano).

## Configuration Steps (Wrapper Script)

Using a wrapper script is highly recommended for managing environment variables with cron.

1.  **Create the Wrapper Script:**
    In your application directory (`/opt/twitter-discord-processor`), create a script file named `run_processor.sh`:
    ```bash
    nano run_processor.sh
    ```
    
    Paste/update the following content and ensuring the JAR file finding logic is correct:
    ```bash
    #!/bin/bash


    # --- Application Execution ---
    # Get the directory where the script is located
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

    # Find the application JAR file (handles changing version numbers)
    # Assumes only ONE such JAR exists in the directory
    APP_JAR=$(ls "$SCRIPT_DIR"/twitter-discord-processor-*.jar | head -n 1)

    # Check if JAR file was found
    if [ -z "$APP_JAR" ] || [ ! -f "$APP_JAR" ]; then
        echo "ERROR: Application JAR file not found in $SCRIPT_DIR matching twitter-discord-processor-*.jar at $(date)"
        exit 1
    fi

    # Define the data directory (relative to script dir, will be created by app if needed)
    DATA_DIR="$SCRIPT_DIR/data"
    # Optional: Specify max tweets (e.g., 20) - uncomment and set value
    # MAX_TWEETS=20
    # Optional: Build arguments array
    # declare -a JAVA_ARGS=("$DATA_DIR") # Start with data dir
    # if [ -n "$MAX_TWEETS" ]; then
    #   JAVA_ARGS+=("$MAX_TWEETS") # Add max tweets if set
    # fi

    echo "Starting Twitter-Discord Processor (JAR: $APP_JAR) at $(date)"
    # Run the Java application using the found JAR
    # Pass the data directory path. Add max tweets argument if needed.
    # Using default args (data dir relative to JAR, 10 max tweets):
    java -jar "$APP_JAR"
    # Or specifying data dir:
    # java -jar "$APP_JAR" "$DATA_DIR"
    # Or specifying data dir and max tweets:
    # java -jar "$APP_JAR" "$DATA_DIR" $MAX_TWEETS
    # Or using the args array:
    # java -jar "$APP_JAR" "${JAVA_ARGS[@]}"


    # Optional: Check exit code
    EXIT_CODE=$?
    if [ $EXIT_CODE -ne 0 ]; then
        echo "Application finished with error code: $EXIT_CODE at $(date)"
    else
        echo "Application finished successfully at $(date)"
    fi

    exit $EXIT_CODE
    ```
    *(Choose the appropriate `java -jar` command line based on whether you want to specify the data directory or max tweets, or use the application defaults).*

2.  **Make the Script Executable:**
    ```bash
    chmod +x run_processor.sh
    ```
    
3. **Update your bashrc**

   Paste/update the following content to your `.bashrc`, **replacing placeholders**:
    ```bash 
    
        # --- Configuration ---
        # !! IMPORTANT: Set your actual secrets here !!
        export TWITTER_BEARER_TOKEN="YOUR_ACTUAL_TWITTER_BEARER_TOKEN"
        export DISCORD_BOT_TOKEN="YOUR_ACTUAL_DISCORD_BOT_TOKEN"
        export TWITCH_CLIENT_ID="YOUR_ACTUAL_TWITCH_CLIENT_ID"
        export TWITCH_CLIENT_SECRET="YOUR_ACTUAL_TWITCH_CLIENT_SECRET"
        # Optional: Set TWITTER_USERNAME here if you don't want to rely on config.properties
        # export TWITTER_USERNAME="your_twitter_username"
    ```

## Cron Job Setup

1.  **Edit Crontab:**
    Open the crontab editor for the user that will run the job:
    ```bash
    crontab -e
    ```

2.  **Add the Cron Job Line:**
    Ensure the path to `run_processor.sh` is correct.

    ```crontab
    # Run Twitter-Discord Processor every 20 minutes
    */20 * * * * /opt/twitter-discord-processor/run_processor.sh >> /opt/twitter-discord-processor/cron.log 2>&1
    ```
    * `*/20 * * * *`: Runs at minutes 0, 20, and 40 of every hour.
    * `/opt/twitter-discord-processor/run_processor.sh`: **Replace with the absolute path** to your wrapper script.
    * `>> /opt/twitter-discord-processor/cron.log 2>&1`: Appends all output (stdout and stderr) from the script to `cron.log` in the application directory. **Ensure the user running cron has write permissions** to this directory or change the log path (e.g., `/var/log/twitter-processor.log`).

3.  **Save and Exit:** (e.g., `Ctrl+X`, `Y`, `Enter` in nano).

4.  **Verify (Optional):**
    ```bash
    crontab -l
    ```

## Logging and Monitoring

* **Application Logs:** Check the `logs` subdirectory within your data directory (e.g., `/opt/twitter-discord-processor/data/logs/`). This contains `application.log` and the per-tweet logs.
* **Cron Output Log:** Check the `cron.log` file specified in the crontab line (e.g., `/opt/twitter-discord-processor/cron.log`). This captures output from the wrapper script and any direct Java errors not caught by Logback.
* **System Cron Logs:** Check system logs like `/var/log/syslog` or `/var/log/cron` for confirmation that cron attempted to run the job.

Your application should now be set up to run automatically via cron.
