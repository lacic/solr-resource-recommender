Resource Recommender Framework using Apache Solr
=========================

This project aims at providing a simple to use and extend resource recommendation system.

## Description
The aim of this work is to provide the community with a simple to use, generic resource-recommender framework to evaluate different resource-recommender algorithms, based on marketplace and social data, with a set of well-known std. IR metrics such as MAP, MRR, P@k, R@k, F1@k, nDCG, Diversity, User Coverage.



This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please cite [the papers](https://github.com/lacic/solr-resource-recommender#references) if you use this software in one of your publications.

## Download

The source-code can be directly checked-out through this repository. The code/solr-resource-recommender-framework/ folder contains a maven project to edit. The built and deployed solr-resource-recommender-framework-1.0-SNAPSHOT.jar file can be downloaded from the root project folder. 

## Installation

### Using Maven

The framework is built and deployed via [Maven](http://maven.apache.org/). To install it, check out this repository and run mvn clean install in the code/solr-resource-recommender-framework/ folder. 

```
$ git clone https://github.com/lacic/solr-resource-recommender.git solr-resource-recommender
$ cd solr-resource-recommender/code/solr-resource-recommender-framework
$ mvn clean install
```

After that, it is available to other projects by depending directly on it (consult pom.xml for the version to use). The source code can also be checked out and used in most Java IDEs.

### Using the provided .jar file

If you wish, you can download the solr-resource-recommender-framework-1.0-SNAPSHOT.jar and add it to your projects build path. Take note of the depended libraries which are needed for the framework and which can be found in the [pom.xml](https://github.com/lacic/solr-resource-recommender/blob/master/code/solr-resource-recommender-framework/pom.xml) file.

## How to use

Once you have set up the framework inside your project use the provided SolrServiceContainer to find needed Solr services to upload the data needed for making recommendations:

```
...
SolrServiceContainer.getInstance().getUserService().updateDocument(new Customer());
SolrServiceContainer.getInstance().getItemService().updateDocument(new Item());
```

After you have initially imported your data into Solr, only thing needed is to call the RecommenderEngine to generate recommendations based on a user-ID and/or product-ID. Additional parameters are n (the number of returned recommendations) and a content filter (used for narrowing down the wanted recommendation results, e.g., recommendations suited only for users of 18 years or older).

```
RecommenderOperations engine = new RecommenderEngine();

String userID = "my_unique_user";
String productID = null;
Integer recCount = 10;

List<String> recommendedResourceIds = engine.getRecommendations(userID, productID, recCount);
```


**Example:**


**Example:**

Currently, the available recommendation algorithms that could be passed to the ```getRecommendations``` method are:

* ```CollaborativeFiltering``` Item-Item Collaborative Filtering.

* ```ContentBased``` Content Based approach.

* ```MostPopular``` Most popular purchases. 

* ```CF_Social``` User-User Collaborative Filtering user interactions. 

* ```SocialStream``` User-User Collaborative Filtering using the social stream content. 

## References
* Lacic, E., Kowald, D., Parra, D., Kahr, M., & Trattner, C. (2014). [Towards a Scalable Social Recommender Engine for Online Marketplaces: The Case of Apache Solr](http://www.christophtrattner.info/pubs/ws12srs11.pdf), In Proceedings of the ACM World Wide Web Conference companion (WWW 2014). ACM.

## Contact
* Emanuel Lacić, Know-Center, Graz University of Technology, elacic@know-center.at
* Christoph Trattner, Know-Center, Graz University of Technology, ctrattner@know-center.at

