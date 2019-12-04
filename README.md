# chemrecog

Chemical Entity Recognition System

## Installation

Download from http://example.com/FIXME.

Get MetaMap API from http://metamap.nlm.nih.gov/

Install public_mm/src/javaapi/dist/prologbeans.jar into Maven repository

mvn install:install-file -Dfile=public_mm/src/javaapi/dist/prologbeans.jar -DgroupId=sics.se -DartifactId=prologbeans -Dversion=4.2.1 -Dpackaging=jar

Install public_mm/src/javaapi/dist/MetaMapApi.jar into Maven repository

mvn install:install-file -Dfile=public_mm/src/javaapi/dist/MetaMapApi.jar -DgroupId=gov.nih.nlm.nls.metamap -DartifactId=metamap-api -Dversion=2012 -Dpackaging=jar

## Usage

The command line arguments the server are: [hostname [port]]
The server defaults to localhost on port 32000 unless specified.

    $ java -jar chem-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

### Example Request

combine5|Down-regulation of the expression of alcohol dehydrogenase 4 and CYP2E1 by the combination of α-endosulfan and dioxin in HepaRG human cells.|Pesticides and other persistent organic pollutants are considered as risk factors for liver diseases. We treated the human hepatic cell line HepaRG with both 2,3,7,8 tetrachlorodibenzo-p-dioxin (TCDD) and the organochlorine pesticide, α-endosulfan, to evaluate their combined impact on the expression of hepatic genes involved in alcohol metabolism. We show that the combination of the two pollutants (25nM TCDD and 10μM α-endosulfan) led to marked decreases in the amounts of both the mRNA (up to 90%) and protein (up to 60%) of ADH4 and CYP2E1. Similar results were obtained following 24h or 8days of treatment with lower concentrations of these pollutants. Experiments with siRNA and AHR agonists and antagonist demonstrated that the genomic AHR/ARNT pathway is necessary for the dioxin effect. The PXR, CAR and estrogen receptor alpha transcription factors were not modulators of the effects of α-endosulfan, as assessed by siRNA transfection. In another human hepatic cell line, HepG2, TCDD decreased the expression of ADH4 and CYP2E1 mRNAs whereas α-endosulfan had no effect on these genes. Our results demonstrate that exposure to a mixture of pollutants may deregulate hepatic metabolism.

### Response

alcohol|C091419|37,44;471,478|
CYP2E1|D001141|65,71;680,686;1174,1180|
α-endosulfan|D004726|94,106;1195,1207|
dioxin|D013749|111,117;924,930|
tetrachlorodibenzo-p-dioxin|D013749|307,334|
TCDD|D013749|336,340;548,552;1132,1136|
organochlorine||350,364|
CAR|C024888|948,951|
estrogen||956,964|
EOF

## Mallet

see scripts/setup.clj, src/chem/setup.clj, src/chem/paths.clj and 
src/chem/mallet.clj for information on generating features.

scripts/setup.clj       -
scripts/setupscript.clj -
scripts/

src/chem/setup.clj      - loads development and training corpori into memory
src/chem/paths.clj      - file paths to training and development corpori
src/chem/mallet.clj     - convience functions for mallet.


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

To evaluate using training-set for testing:

bc-evaluate chemdner-resultlist.txt /nfsvol/nlsaux16/II\_Group\_WorkArea/Lan/projects/BioCreative/2013/CHEMDNER\_TRAIN\_V01/cdi\_ann\_training\_13-07-31.txt

To evaluate using development-set for testing:

bc-evaluate chemdner-resultlist.txt /nfsvol/nlsaux16/II\_Group\_WorkArea/Lan/projects/BioCreative/2013/CHEMDNER\_DEVELOPMENT\_V02/cdi\_ann\_development_13-08-18.txt

To evaluate:

bc-evaluate subsume-chemdner-resultlist.txt /nfsvol/nlsaux16/II\_Group\_WorkArea/Lan/projects/BioCreative/2013/CHEMDNER\_TRAIN\_V01/cdi\_ann\_training_13-07-31.txt

Adhoc MeSH Dictionary Lookup
====================================

Use chem/src/chem/mesh_chem.clj with MeSH Descriptors and
Supplementary concept XML files to generate table file allmesh.txt.

Use the program in $GWA/wjrogers/Projects/chem/pug/diy/mockup.py (Must think of better name...)
See also, $GWA/wjrogers/Projects/chem/pug/diy/0README


/nfsvol/nls/MEDLINE\_Baseline\_Repository/MeSH/2017

  desc2017.xml
  supp2017.xml

See script $GWA/wjrogers/chem/scripts/mesh2017.clj to generate mesh_all.txt and mesh2017.termlist.

Convert mesh_all.txt to meshtermlist that will be input to mockup.py


  
### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright Â© 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
