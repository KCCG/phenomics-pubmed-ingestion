Introduction 
============
Phenmomic-Pubmed-Ingestion is a service written in Java8. The core module is an AWS Lambda function which is responsible to fetch latest pubmed-medline articles and after parsing the relevance content, store them in S3 bucket.

Following are key features for this service:

**Lambda**
* Scheduled to run everyday in morning (AEST)
* Fetches previous and current day's articles (relDate=2)
* Deduplicate (Filter based on already fetched articles using SOLR Indexing) and store them in S3 and Solr instance

**Aache SOLR**
* Deployed for real time Article Exploration ( This is a dev setup to query articles efficiently)
* It is one instance (Self Zookeeper)

Setup
=====
1. First of all to run the code locally, SOLR setup is required. This has been facilitated with dockerized SOLR.
    a. Run build_run_solr.sh from dockerized-solr folder. This will take time while running first time. However next time it should be lightening fast.
    b. SOLR can be accessed at http://localhost:8983/solr
    c. Basic Lucene queries are used to filter articles. This can help providing documents for annotation pipeline research.

2. Main Java code can be run using IDE and that will fetch articles and store them in Solr. **NOTE** Please disable S3 persistence while running locally to avoid storing articles on AWS S3.


Deployment
==========
A. Ops-Code folder contains cloud formation templates for both SOLR and Lambda functions.
B. SOLR cloud formation is straightforward as it deploy SOLR stack with blank Articles collection.
   However, template can be changed for instance size, EBS Volume and other infrastructural properties.
C. To deploy Lambda following command sequence is recommended.


Step 1
```
./gradlew buildZip
```
This will build a zip file, copy the name of zip file and then use it for 2nd and 3rd Step.

Step 2
```
./build.sh name_of_zip_file
```
This will copy the file to AWS S3 bucket for lambda source code.

Step3:
In lambda-cfn.json file assign copied file name to S3Key variable. Then use this template to deploy/update lambda stack.


Install in Intellij
===================
This project uses Lombok and you need to install the following to get it to work:
* Lombok plugin
* Enable Annotation processing by going to:
Preferences -> Build, execution, deployment, compiler, Annotation Processors
Creating a profile for the project and enabling it

https://www.jetbrains.com/help/idea/2016.1/configuring-annotation-processing.html

Rebuild and then you should be good to go. 

