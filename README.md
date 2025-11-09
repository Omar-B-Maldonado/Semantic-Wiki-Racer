# <p align="center">Semantic Wiki Racer</p>

### What is this?
> A Web crawler that plays the Wiki Race game! It takes a linear path to get from the given starting page to the given end page.

> If you're unfamiliar with the Wiki Race game, it's when two people decide on a starting Wikipedia page and target Wikipedia page. Then, they race to get from the starting page to the target Wiki page, either in less clicks or less time. The Semantic Wiki Racer simulates this behaviour, but autonomously (you can try to race it, but good luck... it's pretty fast).

### From Teletubbies to Noodle in seconds
>![racing3](https://github.com/user-attachments/assets/e4afc195-82e7-4f80-a2a2-952ea6b9d515)

### How does it work?
> The program: 
> 1. Prompts the user for a Wikipedia page link to serve as the starting point, as well as one to serve as the end-goal.
> 2. Uses Jsoup to scrape and store the page's hyperlinks in a list, filtering out any urls that aren't Wiki pages.
> 3. Uses HttpClient to connect to the Oracle Cognitive Text API, sending the list of hyperlinks and receiving them back sorted in order of semantic-similarity to the target
> 4. Crawls into the link that is most semantically-similar to the target.
> 5. Recursively jumps to step 2 until the target page is reached.
>
> For context, the Oracle API compares a list of text against a target text. Each text in the list is given a rating from 0 to 1 in terms of how semantically-similar it is to the target, with 0 being the farthest in meaning from the target and 1 being the closest. Behind the scenes, Oracle uses a trained AI model to decide this rating.

### Why?
> While going down the backend path on [roadmap.sh](https://roadmap.sh/), I wanted to learn more about HTTP and how to handle HTTP requests/responses in Java. Having seen [a YouTube video about how to make a web crawler with Jsoup](https://www.youtube.com/watch?v=wrFXBV4MwvI), I got the idea to make a web crawler that crawls Wiki and basically plays the wiki game by itself.

### Technologies Used
> - [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)                                                        (because Java is actually awesome)
> - [Maven](https://maven.apache.org/)                                                                                                             (for dependency management)
> - [Oracle Cognitive Text API](https://docs.oracle.com/en/database/oracle/machine-learning/omlss/omlss/op-v1-cognitive-text-similarity-post.html) (for semantically scoring links)
> - [Jackson](https://en.wikipedia.org/wiki/Jackson_(API))                                                                                         (for JSON parsing)
> - [Jsoup](https://jsoup.org/)                                                                                                                    (for scraping the pages' hyperlinks)
> - [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)                                   (for connecting to the Oracle API)
> - [Dotenv](https://github.com/cdimascio/dotenv-java)                                                                                             (for environment management)

### How to try it yourself
> The setup is extensive due to relying on Oracle's Semantic Similarities API. To use their API, you need to:
> 1. Make an [Oracle Cloud Infrastructure](https://www.oracle.com/cloud/) account and [provision yourself an Oracle Autonomous AI Database](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/autonomous-provision.html#GUID-0B230036-0A05-4CA3-AF9D-97A255AE0C08) (it's free).
> 2. You'll need to get your own URLs for both authenticating to and using Oracle' Machine Learning Services
> - From your database's information page, select ***Database actions > View all database actions > Related Services > Oracle Machine Learning RESTful Services***
> - Copy the authentication link and the OML services common base URL
> - You'll need to create a file called .eml in the repo's parent folder with the following two lines:
>      -       OML_AUTH_BASE_URL=INSERT_URL_1_HERE
>      -       OML_SERVICE_BASE_URL=INSERT_URL_2_HERE
> 3. You should be able to run the code from there!
> - In your IDE's ***terminal***, run the command ***mvn clean verify*** to build the project
> - run the command ***java -jar target/SemanticWikiRacer-1.0-SNAPSHOT.jar***
> - when prompted for the Admin password, provide the password to your OCI account (it's used to create the API token)
