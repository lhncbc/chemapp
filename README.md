# chemapp/chemrecog

chemapp - Chemical Entity Recognition System

## Installation

### Standalone web app and Socket Server Setup

The socket server and standalone web use the same directory
organization.  The directories config and data must be in the
top-level directory "chem" as chem/config and chem/data.

### Deployment to Tomcat

Both config and data must reside in chem/war-resources.  This can be
done by symbolically linking the directories to the ones in
chem/config and chem/data.  The "lein ring uberwar" will automatically
copy the content of the directories to the deployment jar.

## Usage

The command line arguments the server are: [hostname [port]]
The server defaults to localhost on port 32000 unless specified.

    $ java -jar chem-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## External Data (Mostly indexes)

The following files and directories data directory reside in the data
directory.  When using the socket server the data directory resides in
directory the server is executed from.  When using the servlet the
data directory resides in the servlet engine directory (usually
tomcat) or in some cases it must be deployed with the servlet and
resides in the servlet deployment directory (for instance:
webapps/chemapp.)

    corncob_lowercase.txt  - a file 
    ivf       - contains IRutils version of normchem database (2017)
    lucenedb  - contains lucene version of normchem database (2015)
    models    - OpenNLP models


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

  
### Bugs

The NormChem dictionary using Lucene indexes is currently broken.  The
IRUtils dictionary currently provides the same function so the Lucene
version will probably be deprecated.

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright Â© 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
