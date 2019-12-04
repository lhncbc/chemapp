# Design: chemrecog

Introduction

## Diagram

#+BEGIN_EXAMPLE
	         Document

chemTagger          metamap

#+END_EXAMPLE



## Annotation List Viewer

Request Form:
   source:  pubmed or chemdner
   docids: list of pmids or docids
   submit

Response Page
   list of results:
      pmid + number of annotations

Document Annotation page



## Options


## Examples

;; user> (def chemdner-dev-abstracts (chem.chemdner-tools/load-chemdner-abstracts chem.paths/training-dev-text))
;; user> (pprint (first chemdner-dev-abstracts))
;; ["23104419"
;;  "Transient gestational and neonatal hypothyroidism-induced
;;  specific changes in androgen receptor expression in skeletal and
;;  cardiac muscles of adult rat."
;;  "The present study aims to identify the association between
;; androgen status and metabolic activity in skeletal and cardiac
;; muscles of adult rats with transient gestational/neonatal-onset
;; hypothyroidism. Pregnant and lactating rats were made hypothyroid
;; by exposing to 0.05% methimazole in drinking water; gestational
;; exposure was from embryonic day 9-14 (group II) or 21 (group III),
;; lactational exposure was from postnatal day 1-14 (group IV) or 29
;; (group V). Serum was collected for hormone assay. Androgen receptor
;; status, Glu-4 expression, and enzyme activities were assessed in
;; the skeletal and cardiac muscles. Serum testosterone and estradiol
;; levels decreased in adult rats of groups II and III, whereas
;; testosterone remained normal but estradiol increased in group IV
;; and V, when compared to coeval control. Androgen receptor ligand
;; binding activi ty increased in both muscle phenotypes with a
;; consistent increase in the expression level of its mRNA and protein
;; expressions except in the forelimb of adult rats with transient
;; hypothyroidism (group II-V). Glut-4 expression remained normal in
;; skeletal and cardiac muscle of experimental rats. Specific activity
;; of hexokinase and lactate dehydrogenase increased in both muscle
;; phenotypes whereas, creatine kinase activity increased in skeletal
;; muscles alone. It is concluded that transient
;; gestational/lactational exposure to methimazole results in
;; hypothyroidism during prepuberal life whereas it increases AR
;; status and glycolytic activity in skeletal and cardiac muscles even
;; at adulthood. Thus, the present study suggests that euthyroid
;; status during prenatal and early postnatal life is essential to
;; have optimal AR status and metabolic activity at adulthood."]
;; nil
;; user> 


## annotations and

### pmid 23122105

Title:

"Non-target screening of Allura Red AC photodegradation products in a
beverage through ultra high performance liquid chromatography coupled
with hybrid triple quadrupole/linear ion trap mass spectrometry."

Abstract:

"The study deals with the identification of the degradation products
formed by simulated sunlight photoirradiation in a commercial beverage
that contains Allura Red AC dye. An UHPLC-MS/MS method, that makes use
of hybrid triple quadrupole/linear ion trap, was developed. In the
identification step the software tool information dependent
acquisition (IDA) was used to automatically obtain information about
the species present and to build a multiple reaction monitoring (MRM)
method with the MS/MS fragmentation pattern of the species
considered. The results indicate that the identified degradation
products are formed from side-reactions and/or interactions among the
dye and other ingredients present in the beverage (ascorbic acid,
citric acid, sucrose, aromas, strawberry juice, and extract of
chamomile flowers). The presence of aromatic amine or amide
functionalities in the chemical structures proposed for the
degradation products might suggest potential hazards to consumer
health."

Chemdner gold standard

"Allura Red AC" "sucrose" "aromatic amine or amide" "ascorbic acid"
"citric acid"

MetaMap matched terms after filtering for semantic types:
"carbohydrate", "inorganic chemical", "organic chemical", "chemical",
"chemical viewed structurally", "element, ion, or isotope":

"chamomile flowers" "sucrose" "amine" "ascorbic acid" "aromatic"
"amide" "allura red ac dye" "citric acid"

Normchem terms:
"IDA" "chamomile"

Partial match terms:
"acid" "citric acid"



## flow 1.

1. apply metamap to documents filtering by semantic types
   keep terms
   keep User defined acronyms (UDAs)
2. apply partial match
3. apply normchem and remove any terms that are UDAs

## MTI filtering 

see src/chem/mti_filtering.clj



## Methods

### normchem
engine: :normchem

### metamap
engine: :normchem

### partial-match
engine: :partial

### partial-match with normchem
engine: :partial-normchem


### partial-match with metamap
engine: :partial-enhanced

### partial-match with metamap with subsuming
engines: 

### partial-match with opsin
engines: :partial-opsin

### partial-match with opsin and normchem
engines: :partial-normchem :partial-opsin

### partial-match and token-match with opsin and normchem 
:token-opsin :partial-normchem :partial-opsin




...

### Speed

timing tests using the following commands:

    (time (def subsume-result (chem.stacking/subsume-classify-record mm-api-instance record)))
    (time (def ner-result (chem.stacking/stanford-ner-classify-record ner-classifier record)))
    (time (def enchilad0-result (chem.stacking/enchilada0-classify-record record)))

 subsume:     232285.406828 msec  87800.983504 msecs
 enchildada0: 3516.717708 msecs 28173.301469 msecs 1977.993199 msecs 2009.773325 msecs
 ner:         278.611298 msecs 130.748993 msecs 95.190651 msecs



### mallet

Load data info two-dimensional array of tokens

    String [][] tokens;

for unlabelled data:

input:

    Preclinical 
    Assessment 
    of 
    Ketamine 
    . 
    BACKGROUND 
    : 
    Ketamine 
    is 
    used 
    ...

tokens:

    tokens[0][0] "Preclinical"
    tokens[1][0] "Assessment"
    tokens[2][0] "of"
    tokens[3][0] "Ketamine"
    tokens[4][0] "."
    tokens[5][0] "BACKGROUND"
    tokens[6][0] ":"
    tokens[7][0] "ketamine"
    tokens[8][0] "is"
    tokens[9][0] "used"
    ...

Java Code:

    FeatureVector[] fvs = new FeatureVector[tokens.length];
    nFeatures = tokens[l].length;
    

     ArrayList<Integer> featureIndices = new ArrayList<Integer>();
     for (int f = 0; f < nFeatures; f++) {
      	int featureIndex = features.lookupIndex(tokens[l][f]);
       	// gdruck
       	// If the data alphabet's growth is stopped, featureIndex
       	// will be -1.  Ignore these features.
       	if (featureIndex >= 0) {
       		featureIndices.add(featureIndex);
       	}
       }
       int[] featureIndicesArr = new int[featureIndices.size()];
       for (int index = 0; index < featureIndices.size(); index++) {
       	featureIndicesArr[index] = featureIndices.get(index);
       }
      	fvs[l] = featureInductionOption.value ? new AugmentableFeatureVector(features, featureIndicesArr, null, featureIndicesArr.length) : 
       	new FeatureVector(features, featureIndicesArr);
     }

    ObjectInputStream s =
           new ObjectInputStream(new FileInputStream(modelOption.value));
         crf = (CRF) s.readObject();
         s.close();
       }

### Output for MTI2

Output should be pipe-separated.

Output should contain:

pmid 
MeSH Id (Descriptor or SCR)
MeshTerm (term from source MSH)
UMLS Concept (if available)
Span in text (start, len) or (start, end)


### Mallet features

Each mallet feature contains the following elements separated by
newlines.

    part-of-speech character-class term

Example:

    "\nNN\nuc\nTEA\n"

The part-of-speech in the feature should be normalized to noun, verb,
etc. (i.e. remove make derivative parts of speech into normalized
form, NN and NNP map to noun, for example.) or remove part-of-speech
as a feature.

### Bugs

...



### Any Other Sections
### That You Think
### Might be Useful

