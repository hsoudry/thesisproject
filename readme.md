# Thesis project

This project was created as part of my master's thesis. The goal of the thesis was to answer the [DEBS Grand Challenge of 2015](http://www.debs2015.org/call-grand-challenge.html "DEBS Grand Challenge 2015") by using a cloud based solution.
I used Hadoop to process the data, it was hosted on Amazon EMR (Elastic MapReduce), and the companion web application, created using Laravel 5, was hosted on Amazon Elastic Beanstalk. The application and the computing nodes were linked using the AWS PHP SDK.

The web application was designed to make it easier for examiners to request queries and obtain their results. It has a user authentification modules, allows users to create queries and stores the results which can then be downloaded from the user's account. It also provides the query computation status, its starting and ending time if applicable, and its total computation time.
