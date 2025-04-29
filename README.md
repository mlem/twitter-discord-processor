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
* **Twitter API Credentials:** Bearer Token (v2 API). See [HOW-TO-GET-TWITTER-TOKEN.md].
* **Discord Bot Token:** See [HOW-TO-GET-DISCORD-TOKEN.md].
* **Discord Channel ID:** See [HOW-TO-GET-DISCORD-CHANNEL-ID.md].
* **Twitch API Credentials:** Client ID & Secret. See [HOW-TO-GET-TWITCH-CREDS.md].

## Configuration

Configuration is handled through environment variables, a properties file, and command-line arguments. 

**Environment variables override properties file values.**

1.  **Environment Variables (Required):**
    * `TWITTER_BEARER_TOKEN`
    * `DISCORD_BOT_TOKEN`
    * `TWITCH_CLIENT_ID`
    * `TWITCH_CLIENT_SECRET`

2.  **Environment Variable (Optional - Overrides Properties):**
    * `TWITTER_USERNAME`

3.  **Properties File (`src/main/resources/config.properties`):**
    * **Required:**
        ```properties
        discord.channel.id=YOUR_DISCORD_CHANNEL_ID_HERE
        twitch.username=TARGET_TWITCH_USERNAME_HERE
        ```
    * **Required (if `TWITTER_USERNAME` env var not set):**
        ```properties
        twitter.username=YOUR_DEFAULT_TWITTER_USERNAME_HERE
        ```

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
java -jar target/twitter-discord-processor-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/your/data/directory
```

**Option 2: Using the default data directory**

If you run the JAR without any arguments, it will automatically create and use a directory named data in the same location as the JAR file itself:
```bash
java -jar target/twitter-discord-processor-1.0-SNAPSHOT-jar-with-dependencies.jar 
```

The application will then:

* Fetch the latest tweets (up to 10 by default) for the configured TWITTER_USERNAME.
* Write each tweet's data into a .json file inside the input sub-directory of your chosen base path.
* Read each file from input, send the formatted content to the configured Discord channel.
* Move the file to processed on success or failed on failure.

## Contributing
Contributions are welcome! Please follow the steps in [CONTRIBUTING](CONTRIBUTING).

## License
This project is licensed under the MIT License. See the details in [LICENSE](LICENSE).
