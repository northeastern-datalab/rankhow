# RankHow Project

This repository provides an implementation of the framework for RankHow problems.
Both the Java code for formal method based solutions and Python code for linear regression and AdaRank is included to reproduce the experiments for the RankHow paper.

## Latest Update to Include the experimental code for numerical imprecision

The code for the experiments in Sections 3.2 and 4.4 in the paper is `Experiment_numerical.java`.

## Programming Language and Dependencies

The source code of RankHow is written in Java, tested on JDK 18 and 11. 

This project uses [Maven](https://maven.apache.org/index.html) to manage libraries and compile the code. All library dependencies are specified in the `pom.xml` file.

The Python code for the experiments is tested on Python 3.8.10, 3.10.4.

## Required Libraries

The required external libraries, together with the version tested, are:
- Javatuples 1.2
- gurobi 9.5.2

Note that the maven repository does not provide Gurobi, therefore the corresponding jar file needs to be obtained and provided locally.
Also, using Gurobi requires a license. (Free options are generally available, but this may change over time.)

## Compilation

To compile, navigate to the root directory of the project and run:
```
mvn package
```
Successful comilation will produce a jar file in `/target/` from which classes that implement a `main` function can be executed, e.g.
```
java -cp target/wny-1.0.jar wny.Experiment
```

## Code (src/main/java/wny)

The `data` folder is used for the synthetic data generation.

The `entities` folder contains all entity classes.

The `solver` folder contains the Gurobi solver and the Sampling approach.

The `util` folder contains three classes for parsing the database, measuring the ranking error and finding the most promising cell.

`Experiment.java` and `models` in the root folder run all experiments for the paper.

## Data (data)

The NBA stats datasets and synthetic datasets representing 3 different data distributions used in our experiments are provided here.
The CSRankings dataset we used is not provided here due to its license.

## Acknowledgement

Some code for the database parser and the tuple and relation data structure are originally from the [any-k repository](https://github.com/northeastern-datalab/anyk-code) by [Nikos Tziavelis](https://ntzia.github.io/).
Thank you Nikos for sharing your code!

The NBA data were obtained on February 17, 2024, from the great baseketball stats and history website [Basketball Reference](https://www.basketball-reference.com/).
We appreciate and like the website a lot for its detailed stats.

The CSRanking data were obtained on July 15, 2024, from [CSRankings](https://csrankings.org/).
We appreciate Prof. Emery Berger for establishing the website and allowing us to use the data.

## Contact

Zixuan Chen (chen.zixu@northeastern.edu)