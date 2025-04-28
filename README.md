# Twitter Timeline to Discord Processor

This Java application fetches tweets (including images) from a specified user's timeline on X (formerly Twitter), saves them temporarily to local files, and then processes these files to post formatted messages to a specific Discord channel.

## Features

* Fetches recent tweets from a specified user's timeline using the Twitter API v2.
* Extracts tweet text and associated image URLs.
* Saves tweet data (text, original URL, image URLs) into individual files in a local `input` directory.
* Processes files from the `input` directory.
* Posts a formatted message to a configured Discord channel, including:
    * The original tweet text.
    * Attached image URLs (which Discord usually embeds).
    * A link back to the original tweet on X.com.
* Moves processed files to a `processed` directory and failed files to a `failed` directory.
* Configurable via environment variables and a properties file.
* Uses a default `data` directory relative to the application JAR if no path is specified.
* Built with Java 21 and Maven.

## Prerequisites

* **Java Development Kit (JDK):** Version 21 or higher.
* **Apache Maven:** To build the project.
* **Twitter API Credentials:** You need a **Bearer Token** from a Twitter Developer App (v2 API access required).
* **Discord Bot Token:** You need a token for a Discord Bot that has permissions to send messages in the target channel.
* **Discord Channel ID:** The ID of the text channel where the bot should post messages.
  
For more detailed instructions, please refer to the [How-To Guide](HOW-TO.md).

## Configuration

1.  **Environment Variables:** Set the following environment variables before running the application:
    * `TWITTER_BEARER_TOKEN`: Your Twitter API v2 Bearer Token.
    * `TWITTER_USERNAME`: The X/Twitter username whose timeline you want to fetch (without the `@`).
    * `DISCORD_BOT_TOKEN`: Your Discord Bot Token.

2.  **Properties File:** Create a file named `config.properties` inside the `src/main/resources` directory (Maven will include this in the JAR). Add the following line, replacing the placeholder with your actual channel ID:
    ```properties
    # Discord Configuration
    discord.channel.id=YOUR_DISCORD_CHANNEL_ID_HERE

    # Twitter Configuration (Fallback if ENV var not set)
    twitter.username=YOUR_DEFAULT_TWITTER_USERNAME_HERE
    ```

## Building

1.  Clone the repository:
    ```bash
    git clone https://github.com/mlem/twitter-discord-processor.git
    cd twitter-discord-processor
    ```
2.  Compile and package the application using Maven:
    ```bash
    mvn clean package
    ```
    This command will clean previous builds, compile the code, run tests (if any), and create an executable JAR file with all dependencies included in the `target/` directory. The file will typically be named something like `twitter-discord-processor-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Running

Execute the JAR file from your terminal.

**Option 1: Using a specific data directory**

Provide the desired absolute or relative path for the data directory (where `input`, `processed`, `failed` folders will be created) as a command-line argument:

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
* Write each tweet's data into a .txt file inside the input sub-directory of your chosen base path.
* Read each file from input, send the formatted content to the configured Discord channel.
* Move the file to processed on success or failed on failure.

## Contributing
Contributions are welcome! Please follow the steps in [CONTRIBUTING](CONTRIBUTING).

## License
This project is licensed under the MIT License. See the details in [LICENSE](LICENSE).
