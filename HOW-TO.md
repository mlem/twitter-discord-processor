# How to

## How to Get a Discord Bot Token

Follow these steps to create a Discord application and retrieve the token needed for your bot:

1.  **Go to the Discord Developer Portal:**
    * Open your web browser and navigate to: [https://discord.com/developers/applications](https://discord.com/developers/applications)
    * Log in with your Discord account if prompted.

2.  **Create a New Application:**
    * Click the "**New Application**" button (usually in the top right corner).
    * Enter a **Name** for your application (e.g., "My Twitter Bot", "Timeline Processor"). This name is primarily for your reference in the portal.
    * Agree to the Discord Developer Terms of Service and Developer Policy if prompted.
    * Click "**Create**".

3.  **Navigate to the Bot Settings:**
    * Once your application is created, you'll land on its settings page.
    * In the left-hand sidebar menu, click on "**Bot**".

4.  **Add a Bot User:**
    * Click the "**Add Bot**" button.
    * A confirmation pop-up will appear. Click "**Yes, do it!**". This creates the actual bot user associated with your application.

5.  **Reveal and Copy the Token:**
    * Under the bot's username (which you can customize), you will see a section labeled "**TOKEN**".
    * Click the "**Reset Token**" button. (Even if it's the first time, resetting ensures you get a fresh token).
    * Confirm the reset if asked (you might need to enter a 2FA code if enabled on your account).
    * **Important:** Discord will display the token **only once** right after resetting. Click the "**Copy**" button immediately to copy the token to your clipboard.

6.  **Save Your Token Securely:**
    * Paste the copied token into a secure, private location (like a password manager or a secure note).
    * **Treat this token like a password!** Do not share it publicly, commit it to version control (like Git), or embed it directly in your code. Use secure methods like environment variables (as configured in the Java project) to provide the token to your application at runtime.

You now have the Discord Bot Token. You will use this value for the `DISCORD_BOT_TOKEN` environment variable when running your Java application.

## How to Invite Your Discord Bot to a Server

After creating your bot and getting its token (as described in the previous guide), you need to invite it to the Discord server(s) where you want it to operate. This is done by generating a special invite URL using the Discord Developer Portal.

**Prerequisites:**

* You must have the **Manage Server** permission (or be the owner) on the Discord server you want to add the bot to.
* You need the **Client ID** of your Discord application (found on the "General Information" page of your application in the Developer Portal).

**Steps:**

1.  **Go to the Discord Developer Portal:**
    * Navigate back to your application: [https://discord.com/developers/applications](https://discord.com/developers/applications)
    * Select the application corresponding to your bot.

2.  **Navigate to the OAuth2 URL Generator:**
    * In the left-hand sidebar menu, click on "**OAuth2**", then select "**URL Generator**".

3.  **Select Scopes:**
    * Under the "SCOPES" section, check the box next to **`bot`**. This indicates you are generating an invite URL for a bot user.

4.  **Select Bot Permissions:**
    * After selecting the `bot` scope, a new section called "BOT PERMISSIONS" will appear below.
    * Check the permissions your bot needs to function. For your current application (posting tweets), you absolutely need:
        * **`Send Messages`**: Allows the bot to send messages in text channels.
        * *(Recommended)* **`Embed Links`**: Allows Discord to automatically show previews for the image and tweet URLs your bot posts.
        * *(Recommended)* **`Attach Files`**: Might be useful if you later want the bot to upload images directly instead of just posting URLs.
    * *Avoid granting `Administrator` permission unless absolutely necessary, as it gives the bot full control over the server.* Grant only the permissions required for its tasks.

5.  **Copy the Generated URL:**
    * Scroll down to the bottom of the "BOT PERMISSIONS" section. Discord generates an invite URL based on your selected scopes and permissions.
    * Click the "**Copy**" button next to the generated URL.

6.  **Use the Invite URL:**
    * Paste the copied URL into your web browser's address bar and press Enter.
    * If you are logged into Discord in that browser, you will see an authorization screen.
    * Use the dropdown menu "**Add to Server**" to select the server you want to invite the bot to.
    * Click "**Continue**".
    * Review the list of permissions you are granting the bot.
    * Click "**Authorize**".
    * You might need to complete a CAPTCHA.

7.  **Confirmation:**
    * You should see an "Authorized" message. The bot will now appear in the member list of the selected server.

**Channel Specific Permissions:**

* The permissions granted in Step 4 apply to the bot across the entire server by default.
* The bot will be able to send messages in any channel where the `@everyone` role (or a specific role the bot gets) has the "Send Messages" permission.
* Your Java application uses a specific `discord.channel.id`. Ensure the bot has permission to send messages *in that specific channel*. You can manage channel-specific permissions in Discord itself (Server Settings -> Roles, or Channel Settings -> Permissions) if you need finer control, but the server-wide permissions granted during the invite are usually sufficient if the channel doesn't have unusual restrictions.


## How to Find a Discord Channel ID

To configure your bot to post in a specific channel, you need that channel's unique ID. Here's how to find it:

**Step 1: Enable Developer Mode**

You first need to enable Developer Mode in your Discord settings. This unlocks extra context menu options, including copying IDs.

1.  Open your Discord client (desktop app or web browser).
2.  Click the **User Settings** icon (the gear/cogwheel ⚙️), usually located near your username in the bottom-left corner.
3.  In the settings menu, scroll down the left sidebar and click on **Advanced**.
4.  Find the toggle switch for **Developer Mode** and make sure it is **enabled** (it should be green or blue).
5.  You can now close the User Settings (click the Esc key or the 'X' button).

**Step 2: Copy the Channel ID**

With Developer Mode enabled, you can now copy the ID of any channel.

1.  Navigate to the server containing the text channel you want the bot to post in.
2.  Locate the specific text channel in the channel list on the left side of the server view.
3.  **Right-click** on the name of the text channel.
4.  In the context menu that appears, click on **Copy Channel ID**.

**Step 3: Use the ID**

The channel ID (a long string of numbers) is now copied to your clipboard.

* Paste this ID into your `config.properties` file for the `discord.channel.id` property:

    ```properties
    # Discord Configuration
    discord.channel.id=PASTE_THE_COPIED_ID_HERE
    ```

Your Java application will now use this ID to identify the correct channel to send messages to.

## How to Get a Twitter API v2 Bearer Token

To use the Twitter API v2, you need credentials from a Twitter Developer App. The Bearer Token is commonly used for read-only access, like fetching timelines. Here's how to get one:

**Step 1: Apply for a Twitter Developer Account**

1.  Go to the **Twitter Developer Portal**: [https://developer.twitter.com/](https://developer.twitter.com/)
2.  Click on "**Sign up**" or "**Apply**". You will need to log in with your regular Twitter account.
3.  Follow the application process. You'll need to:
    * Verify your phone number and email address associated with your Twitter account.
    * Explain your intended use case for the Twitter API (e.g., "To fetch my own timeline for display on a personal project", "To analyze tweet trends for academic research"). Be clear and honest.
    * Agree to the Twitter Developer Agreement and Policy.
4.  Submit your application. Approval might be instant or take some time for review, depending on your use case and account status.

**Step 2: Create a New Project and App**

Once your Developer Account is approved:

1.  Navigate to your **Developer Portal Dashboard**: [https://developer.twitter.com/en/portal/dashboard](https://developer.twitter.com/en/portal/dashboard)
2.  You might be prompted to create a **Project**. Give it a name (e.g., "My Bots", "Timeline Fetcher Project"). Select a use case that matches your application. Provide a description.
3.  Within your Project, you need to create an **App**. Click "**+ Add App**" or similar.
4.  Choose an **App environment** (usually "Development" to start).
5.  Give your **App a unique name**.
6.  Click "**Next**" or "Create".

**Step 3: Generate Your Keys and Tokens**

1.  After creating the App, Twitter will display your **API Key**, **API Key Secret**, and **Bearer Token**.
    * **API Key:** Sometimes called Consumer Key.
    * **API Key Secret:** Sometimes called Consumer Secret.
    * **Bearer Token:** This is the token your current Java application needs.
2.  **Important:** Copy all three of these values **immediately** and store them securely (like in a password manager). The API Key Secret and Bearer Token are often shown only once upon generation.
3.  If you navigate away or lose them, you might need to regenerate them (which will invalidate the old ones). You can usually do this from the "Keys and Tokens" tab within your App's settings in the Developer Portal.

**Step 4: Use the Bearer Token**

* Take the **Bearer Token** you copied.
* Set it as the value for the `TWITTER_BEARER_TOKEN` environment variable when running your Java application.

**Note on Access Levels:**

* The default access level for new developer accounts and apps is usually "Essential" access (v2 API). This level is sufficient for fetching timelines and many other read-only tasks.
* If you need higher rate limits or access to more advanced endpoints, you might need to apply for "Elevated" access through the Developer Portal dashboard.
