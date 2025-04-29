# Twitter Timeline to Discord Processor

This Java application fetches **new** tweets (including images and author info) from a specified user's timeline on X (formerly Twitter) since the last run, fetches corresponding Twitch streamer info, saves this combined context temporarily to local **JSON files**, processes these files to post formatted embeds to a specific Discord channel, and logs its operations. It avoids reprocessing files that have already been successfully processed or have previously failed.

## Features

* Fetches **only new** tweets from a specified user's timeline using the Twitter API v2 `since_id` parameter. It reads the ID of the last successfully processed tweet from `LAST_TWEET_ID.txt` in the data directory and requests tweets newer than that ID.
* Extracts tweet text, associated image URLs, creation timestamp, and **author information**.
* Fetches **Twitch streamer info** for a configured Twitch username.
* Saves combined context into individual **JSON files** in the `input` directory.
* Processes files from the `input` directory in chronological order (by filename/tweet ID). **Skips files** if a file with the same name already exists in the `processed` or `failed` directories (duplicates found in `input` are moved to `bin`).
* Posts a formatted **Discord Embed** message for new files.
* Moves successfully processed files to `processed`.
* Moves files that failed processing to `failed`.
* Moves duplicate input files (already processed/failed) to `bin`.
* **Logs general application activity** to `application.log`.
* **Logs detailed steps for each tweet processed** to a separate file (e.g., `logs/tweet_123.log`).
* **Stores the ID of the newest tweet processed** in `LAST_TWEET_ID.txt` in the data directory for the next run.
* Configurable via environment variables, properties file, and command-line arguments.
* Uses a default `data` directory relative to the application JAR.
* Built with Java 21 and Maven.

## Directory Structure

When run, the application uses the following structure within the specified or default base data directory:
```
/
├── twitter-discord-processor-X.Y.Z.jar  # The executable JAR
├── config.properties                    # <-- REQUIRED external configuration file
└── data/                                # Base data directory (created if needed)
    ├── input/                 # New tweet context JSON files are saved here
    ├── logs/                  # Contains application.log AND per-tweet logs
    ├── processed/             # Successfully processed tweet JSON files are moved here
    ├── failed/                # Tweet JSON files that failed processing are moved here
    ├── bin/                   # Duplicate JSON files found in input are moved here
    └── LAST_TWEET_ID.txt      # Stores the ID of the newest tweet fetched+written
```

## Prerequisites

* **Java Development Kit (JDK):** Version 21 or higher.
* **Apache Maven:** To build the project.
* **Twitter API Credentials:** Bearer Token (v2 API). See [HOW-TO.md].
* **Discord Bot Token:** See [HOW-TO.md].
* **Discord Channel ID:** See [HOW-TO.md].
* **Twitch API Credentials:** Client ID & Secret. See [HOW-TO.md].

## Configuration

Configuration is handled through environment variables and an **external `config.properties` file placed next to the JAR**. **Environment variables override values in the properties file where applicable (e.g., `TWITTER_USERNAME`).**

1.  **Environment Variables (Required):**
    * `TWITTER_BEARER_TOKEN`
    * `DISCORD_BOT_TOKEN`
    * `TWITCH_CLIENT_ID`
    * `TWITCH_CLIENT_SECRET`

2.  **Environment Variable (Optional - Overrides Properties):**
    * `TWITTER_USERNAME`

3.  **External Properties File (`config.properties`):**
    * **This file MUST be created and placed in the same directory as the JAR file.**
    * The `config.properties` file inside `src/main/resources` is **NOT packaged** into the JAR and serves only as a template during development.
    * **Required Contents:**
        ```properties
        # Discord Configuration
        discord.channel.id=YOUR_DISCORD_CHANNEL_ID_HERE

        # Twitch Configuration
        # Username of the Twitch streamer whose logo should be used
        twitch.username=TARGET_TWITCH_USERNAME_HERE

        # Twitter Configuration (Fallback if TWITTER_USERNAME ENV var not set)
        twitter.username=YOUR_DEFAULT_TWITTER_USERNAME_HERE
        ```
    * Replace placeholders with your actual values.

## Building

Clone: 
```bash
git clone <your-repository-url>
cd <repository-directory>
```

2.  Build: (Creates `target/twitter-discord-processor-1.0-SNAPSHOT-jar-with-dependencies.jar`)
```bash
mvn clean package
```

## Running

Execute the JAR file from your terminal. Ensure environment variables are set.

**Command Format:**

```bash
java -jar target/<jar-file-name>.jar [base_data_directory_path] [max_tweets_to_fetch]
```
* [base_data_directory_path] (Optional): Path for the data folder. Defaults to ./data next to JAR.
* [max_tweets_to_fetch] (Optional): Max new tweets per run. Defaults to 10. Max effective is 100 (API limit).

**Option 2: Using the default data directory**

If you run the JAR without any arguments, it will automatically create and use a directory named data in the same location as the JAR file itself:
```bash
java -jar target/twitter-discord-processor-1.0-SNAPSHOT-jar-with-dependencies.jar 
```

## Deployment Steps:

* Place the built JAR file (e.g., twitter-discord-processor-1.0.123.jar) in your desired application directory on the server.
* Create a config.properties file in the SAME directory as the JAR, filling it with your required discord.channel.id, twitch.username, and fallback twitter.username.
* Set the required environment variables (TWITTER_BEARER_TOKEN, DISCORD_BOT_TOKEN, etc.).
* Run the JAR using the command format above (or via the run_processor.sh script if using cron).

## Workflow:

* Reads LAST_TWEET_ID.txt (if it exists) to get the since_id.
* Fetches tweets newer than since_id for the configured Twitter user.
* Fetches Twitch info.
* Writes context for each new tweet to a .json file in input/.
* If new tweets were written, updates LAST_TWEET_ID.txt with the ID of the newest one.
* Scans input/, sorts files by name. 
  For each .json file:
  * Skips if already in processed/ or failed/ (moves duplicate from input/ to bin/).
  * If new, sends Discord embed, logs details to logs/tweet_ID.log.
  * Moves file from input/ to processed/ or failed/.
* Logs general activity to logs/application.log.


## Server installation

If you want to install this program as a service on your linux server, follow the steps in [how to setup on linux](HOW-TO-SETUP-ON-LINUX.md).

## Contributing
Contributions are welcome! Please follow the steps in [CONTRIBUTING](CONTRIBUTING).

## License
This project is licensed under the MIT License. See the details in [LICENSE](LICENSE).
