SocRec - Towards a Scalable Social Resource Recommender Framework using Apache Solr
=========================

## Description
Recent research has unveiled the importance of online social networks for improving the quality of recommenders in several domains, what has encouraged the research community to investigate ways to better exploit the social information for recommendations. However, there is a lack of work that offers details of frameworks that allow an easy integration of social data with traditional recommendation algorithms in order to yield a straight-forward and scalable implementation of new and existing systems. With SocRec we intend to bridge this gap. In particular with SocRec we introduce a novel social recommender engine for online marketplaces that is built upon the well-know search engine Apache Solr. The framework offers a set of content and collaborative filtering approaches hybrid approaches to recommend items (e.g., products) to user not only in a personalized manner but also utilizing social data from the users social networks such as Facebook, Google+, or Twitter. To the best of our knowledge SocRec is the first open source recommender engine for online marketplaces that relies on
std. search software such as Apache Solr and is able to utilize social data from user to increase the recommender accuracy.


This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please cite [the papers](https://github.com/lacic/solr-resource-recommender#references) if you use this software in one of your publications.

## Download

The source-code can be directly checked-out through this repository. The code/solr-resource-recommender-framework/ folder contains a maven project to edit. The built and deployed solr-resource-recommender-framework-1.0-SNAPSHOT.jar file can be downloaded from the root project folder. 

## Installation

### Apache Solr

First things first, SocRec depends on [Apache Solr](http://lucene.apache.org/solr/) as its datasource. Download it, extract everything in any prefered location and set up the different collection configuration with the provided [schema files](https://github.com/lacic/solr-resource-recommender/tree/master/resources/solr-schemas). For first time users, please conduct Solr's [tutorial site](http://lucene.apache.org/solr/4_7_2/tutorial.html).


### Using Maven

The framework is built and deployed via [Maven](http://maven.apache.org/). To install it, check out this repository and run mvn clean install in the code/solr-resource-recommender-framework/ folder. 

```
$ git clone https://github.com/learning-layers/SocRec.git soc-rec
$ cd soc-rec/code/solr-resource-recommender-framework
$ mvn clean install
```

After that, it is available to other projects by depending directly on it (consult pom.xml for the version to use). The source code can also be checked out and used in most Java IDEs.

### Using the provided .jar file

If you wish, you can download the solr-resource-recommender-framework-1.0-SNAPSHOT.jar and add it to your projects build path. Take note of the depended libraries which are needed for the framework and which can be found in the [pom.xml](https://github.com/learning-layers/SocRec/blob/master/code/solr-resource-recommender-framework/pom.xml) file.

## How to use

Once you have set up the framework inside your project use the provided SolrServiceContainer to find needed Solr services to upload the data needed for making recommendations:

```
...
SolrServiceContainer.getInstance().getUserService().updateDocument(new Customer());
SolrServiceContainer.getInstance().getResourceService().updateDocument(new Resource());
```

After you have initially imported your data into Solr, only thing needed is to call the RecommenderEngine to generate recommendations based on a user-ID and/or product-ID. Additional parameters are n (the number of returned recommendations) and a content filter (used for narrowing down the wanted recommendation results, e.g., recommendations suited only for users of 18 years or older).

```
RecommenderOperations engine = new RecommenderEngine();

String userID = "my_unique_user";
String productID = null;
Integer recCount = 10;

List<String> recommendedResourceIds = engine.getRecommendations(userID, productID, recCount);
```


=======


## References
* Lacic, E., Kowald, D., Parra, D., Kahr, M., & Trattner, C. (2014). [Towards a Scalable Social Recommender Engine for Online Marketplaces: The Case of Apache Solr](http://www.christophtrattner.info/pubs/ws12srs11.pdf), In Proceedings of the ACM World Wide Web Conference companion (WWW 2014). ACM.

## Contact
* Emanuel Lacic, Graz University of Technology, elacic@know-center.at
* Christoph Trattner, Know-Center, Graz University of Technology, ctrattner@know-center.at

