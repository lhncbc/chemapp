# chemrecog

FIXME: description

## Installation

Download from http://example.com/FIXME.

Get MetaMap API from http://metamap.nlm.nih.gov/

Install public_mm/src/javaapi/dist/prologbeans.jar into Maven repository

mvn install:install-file -Dfile=public_mm/src/javaapi/dist/prologbeans.jar -DgroupId=sics.se -DartifactId=prologbeans -Dversion=4.2.1 -Dpackaging=jar

Install public_mm/src/javaapi/dist/MetaMapApi.jar into Maven repository

mvn install:install-file -Dfile=public_mm/src/javaapi/dist/MetaMapApi.jar -DgroupId=gov.nih.nlm.nls.metamap -DartifactId=metamap-api -Dversion=2012 -Dpackaging=jar

## Usage

FIXME: explanation

    $ java -jar chem-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...


## Mallet

see scripts/setup.clj, src/chem/setup.clj, src/chem/paths.clj and 
src/chem/mallet.clj for information on generating features.

### running mallet 

Training
========

java -cp /usr/local/pub/machinelearning/mallet-2.0.7/class:/usr/local/pub/machinelearning/mallet-2.0.7/lib/mallet-deps.jar \
    cc.mallet.fst.SimpleTagger --train true --iterations 250 \
    --threads 8 --model-file chemicalcrf data/features/training/*

Testing
=======

java -cp /usr/local/pub/machinelearning/mallet-2.0.7/class:/usr/local/pub/machinelearning/mallet-2.0.7/lib/mallet-deps.jar \
    cc.mallet.fst.SimpleTagger



## Evaluation

To evaluate:

bc-evaluate subsume-chemdner-resultlist.txt /nfsvol/nlsaux16/II_Group_WorkArea/Lan/projects/BioCreative/2013/CHEMDNER_TRAIN_V01/cdi_ann_training_13-07-31.txt


### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
