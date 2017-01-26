# Web Crawler
Serena Chan (2017)
I wrote a web crawler in Java that uses jsoup and mysql-connector to download lyrics from metrolyrics
Build following this tutorial: http://www.programcreek.com/2012/12/how-to-make-a-web-crawler-using-java/

## Setup
1. Download <a href="http://jsoup.org/download">JSoup Core Library</a>
2. Download <a href="http://dev.mysql.com/downloads/connector/j/">mysql-connector-java</a>
3. Create a mysql database `Crawler`
4. Create two tables:
	* `Record` with columns: `RecordID`, `URL`
	* `Song` with columns: `Artist`, `Title`, `Filename`
5. Change mysql username and password in `DB.java`œ