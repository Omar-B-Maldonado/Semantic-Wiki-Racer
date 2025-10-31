# <p align="center">Semantic Wiki Racer</p>

### What is this?
> A Web crawler that plays the Wiki Race game! It takes a linear path to get from the given starting page to the given end page.

### How does it work?
> It leverages an API from Oracle that allows the caller to compare a list of text against a target text. Each text in the list is given a rating from 0 to 1 in terms of how semantically-similar it is to the target, with 0 being the farthest in meaning from the target and 1 being the closest. Behind the scenes, Oracle uses a trained AI model to decide this rating.

### Why?
> While going down the backend path on roadmap.sh, I wanted to learn more about HTTP and how to handle HTTP requests/responses in Java. Having seen a youtube video about how to make a web crawler with JSoup, I got the idea to make a web crawler that crawls Wiki and basically plays the wiki game by itself.

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
