---
title: Roc software - Everything ROC and Precision-Recall curves.
---
All About Roc
=============


Description
-----------

Roc is software for generating and working with ROC ([Receiver Operating
Characteristic](http://en.wikipedia.org/wiki/Receiver_operating_characteristic))
and PR
([Precision-Recall](http://en.wikipedia.org/wiki/Precision_and_recall))
curves.  These curves are typically used to evaluate classification
approaches in areas such as Machine Learning, Statistics, Medicine, and
Epidemiology.  Students, scientists, and researchers are the target
audience of this software.

The goal of this project is to provide software for evaluating ROC and
PR curves that correctly implements the traditional and recent
approaches using languages allowing for flexibility in each
investigator's environment.

Roc, the name of the software, is pronounced "rock" like its namesake,
roc, an enormous, legendary [bird of
prey](http://en.wikipedia.org/wiki/Roc_(mythology\)).


Downloads
---------

* [**Download Roc**](http://kboyd.github.com/Roc/releases/) from the
  releases page.
* Download the latest code as a [ZIP
  file](https://github.com/kboyd/Roc/zipball/master) or a [TAR
  ball](https://github.com/kboyd/Roc/tarball/master).
* [View the project](https://github.com/kboyd/Roc) on GitHub.


License
-------

Roc is free, open source software.  It is released under the BSD
2-Clause License (also known as the FreeBSD License).  See the file
`LICENSE.txt` in your distribution (or [on
GitHub](https://github.com/kboyd/Roc/blob/master/LICENSE.txt)) for
details.


Features and Project Maturity
-----------------------------

Roc is version 0.1.0.

This software is in the early design and development stages.  It is
planned to be released as a library and as a command-line interface
(CLI) front-end for the library.  The table below contains a summary of
features.

Features are NP="not planned", P="planned", I="implemented", T="tested",
S="stable".  Languages are J="Java", P2="Python 2.x".

    Feature Description           Library Status  CLI Status (P2)
    -------------------           --------------  ---------------
    ROC curves
    . Points                      J:T  P2:P       P
    . Area                        J:T  P2:P       P
    . Maximum area (convex hull)  J:T  P2:P       P
    . Aggregation (averaging)     J:P  P2:P       P
    . Confidence bounds           J:P  P2:P       P
    . Clipping                    J:P  P2:P       P
    PR curves
    . Points                      J:T  P2:P       P
    . Area                        J:T  P2:P       P
    . Maximum area (convex hull)  J:I  P2:P       P
    . Aggregation (averaging)     J:P  P2:P       P
    . Confidence bounds           J:P  P2:P       P
    . Clipping                    J:P  P2:P       P
    . Minimum awareness           J:P  P2:P       P
    Plotting                      J:NP P2:P       P
    Inputs
    . Ranking                     J:T  P2:P       P
    . Predicteds, actuals         J:T  P2:P       P
    . Predicted-actual pairs      J:P  P2:P       P
    . Example weights             J:P  P2:P       P
    Convenience
    . File I/O                    J:P  P2:P       P
    Ranking Statistics
    . Mann-Whitney-U              J:T  P2:P       P


Requirements
------------

* Java 5 (or later) if using the Java library
* Python 2.5 (or later, but not 3.x) if using the command line interface
  or Python library


Java Library and JAR
--------------------

The Java library provides an API for working with ROC and PR curves in
your Java programs.  It is distributed as a Java archive (JAR)
containing source code, bytecode, and documentation.  The JAR can be
obtained on the [releases page](http://kboyd.github.com/Roc/releases/).
To include the library in your Java project, just place the JAR in a
convenient location and include it in your classpath.  You can browse
the documentation by extracting it from the JAR or by viewing the
[latest version on GitHub](http://kboyd.github.com/Roc/javadoc/).


Contact
-------

* [Kendrick Boyd](https://github.com/kboyd)
* [Aubrey Barnard](https://github.com/afbarnard)

Please search the existing documentation before contacting us.  There is
this README, the [Javadoc](http://kboyd.github.com/Roc/javadoc/), the
[wiki](https://github.com/kboyd/Roc/wiki), and [existing
issues](https://github.com/kboyd/Roc/issues).  Then, [open an
issue](https://github.com/kboyd/Roc/issues/new) to report a bug or ask a
question.


Copyright (c) 2013 Roc Project.  This is free software.  See LICENSE.txt
for details.