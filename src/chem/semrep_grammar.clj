(ns chem.semrep-grammar)

;; % ------------------------------------------- %
;; % File:      ebnf.semrep.txt                  %
;; % Author:    Stefanie Anstein, Gerhard Kremer %
;; % Purpose:   definition of sem.representation %
;; % ------------------------------------------- %

(def chem-semrep-grammar
"compd = 'compd', '(', PAR_PHR,
        'pref', '(', P_LIST, ')',
        'suff', '(', S_LIST, ')', ')';

PAR_PHR = OP1_CONSTRUCT
	| OP2_CONSTRUCT
	| OP3_CONSTRUCT       % sugar-specific
	| OP4_CONSTRUCT
	| OP5_CONSTRUCT	  
	| CLOSED_CLASS ;

P_LIST = '[', {PREF_CONSTRUCT}, ']'
       | '[', PREF_CONSTRUCT, {' ,', PREF_CONSTRUCT}, ']'
       | '[', ']';         % no prefix

S_LIST = '[', {SUFF_CONSTRUCT}, ']';
       | '[', SUFF_CONSTRUCT , {' ,', SUFF_CONSTRUCT}, ']'
       | '[', ']';         % no suffix

OP1_CONSTRUCT = OP1, '(', STRUCT, ')';

OP2_CONSTRUCT = OP2, '(', LOCS_MULT, ',', STRUCT, ')';

OP3_CONSTRUCT = OP3, '(', LOCS_MULT, ',', NUM, ',', STRUCT, ')';

OP4_CONSTRUCT = '(', LOCS_MULT, ',', CLOSED_CLASS, ')';

OP5_CONSTRUCT = COMPD, {'+' , COMPD }, '+', CLOSED_CLASS ;

CLOSED_CLASS = CLOSED_CLASS_PRED, '(', CLOSED_CLASS_MORPH, ')';

PREF_CONSTRUCT = LOCS_MULT, '-', PREFS
	       | LOCS_MULT, '-', COMPD ;

SUFF_CONSTRUCT = LOCS_MULT, '-', SUFF
	       | LOCS_MULT, '-', ADJSUFF ;

STRUCT = PAR_PHR
       | NUM , '*' , ELEMENTS ;

LOCS_MULT = NUM, '*', '[', NUMS , ']'
  	  | UNKNO, '*', '[', NUMS, ']'
	  | NUM, '*', '[', UNKNO, ']'
	  | UNKNO, '*', '[', UNKNO, ']';

PREFS = PREF
      | ELEMENT, '-', PREF ;   % sugar-specific (?)

ADJSUFF = ADJSUFF_PRED , '(' , ADJSUFF_MORPH, ')',
	'+' , CLASSNAME_PRED , '(', CLASSNAME_MORPH, ')';

ELEMENTS = ELEMENT
	 | '(', ELEMENT, '+', ELEMENT, ')';

NUMS = NUM
     | NUM, {',', NUM};
")