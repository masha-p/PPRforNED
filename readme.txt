# personalized_page_rank_for_named_entity_disambiguation
This repository contains data used in the NAACL 2015 paper "Personalized Page Rank for Named Entity Disambiguation" by Maria Pershina, Yifan He, Ralph Grishman.

==================================
Candidate files vs AIDA documents.

Folders AIDA_candidates/PART_1_1000 and AIDA_candidates/PART_1001_1393 contain total of 1393 files:
1000 files in subfolder PART_1_1000 and another 393 files in subfolder PART_1001_1393.
File #N has wikipedia candidates, generated for pretagged entities in the document #N of AIDA dataset, available at
http://www.mpi-inf.mpg.de/yago-naga/aida/

===================================
The format of the file. Entities and candidates.

For every pretagged entity in the original AIDA document #N, there is a line, starting from word "ENTITY", in the file #N.
For example, file #9 starts from entity Syria:
ENTITY	text:Syria	normalName:syria	predictedType:UNK	q:true	qid:Q9	docId:9	origText:Syria	url:http://en.wikipedia.org/wiki/Syria
The line has entity mention "text:Syria", has normalized string for this entity "normalName:syria",  and has a correct disambiguation link at the end "url:http://en.wikipedia.org/wiki/Syria". There are other statistics as well in the line, such as type "predictedType:UNK" , document ID "docId:9", etc.
If entity does not have a correct link in AIDA document, then it has "url:NIL", for example:
ENTITY	text:EU	normalName:eu	predictedType:UNK	q:true	qid:Q1	docId:1	origText:EU	url:NIL

Lines after entity are candidates for this entity.
The first line after entity is the correct candidate if this entity has one, for example first line after Syria entity in file 9 is
CANDIDATE	id:7515849	inCount:15845	outCount:721	links:1928858;12793336;2245563;879212;28542628;39513173;7515849;27473	url:http://en.wikipedia.org/wiki/Syria	name:Syria	normalName:syria	normalWikiTitle:syria	predictedType:GPE	

Every candidate has unique ID ("id:7515849") associated with wikipedia link ("url:http://en.wikipedia.org/wiki/Syria"),
it has number of wikipedia incoming and outgoing links ("inCount:15845 outCount:721"),
it has normalized name of its wikipedia url ("normalName:syria") and type ("predictedType:GPE").

Field "links" represents (outgoing) connections of this candidate to other candidates ID in this document:
"links:1928858;12793336;2245563;879212;28542628;39513173;7515849;27473"


===================================
Graph construction.

Every entity is its normalName concatenated with its correct URL.
There may be several entities in the document with identical normalName and identical correct URL - they are all collapsed together since they have identical set of candidates.
Every node is a pair of an entity and a candidate, generated for this entity.
If same candidate is generated for two different entities then there will be two different nodes with this candidate - one for each entity.
These nodes, corresponding to the same candidate, will be connected by an edge.
Edges are inserted between nodes for different entities whenever one of nodes has an outgoing link to another one.
There are no edges between nodes, generated for the same entity.
Freebase scores, used as initial similarity, are normalized across all candidates for each entity.

===================================
Personalized Page Rank.

To compute PPR weights we use the Monte Carlo approach, proposed by Fogaras and Racz, 2004.
Every node is initialized with 2000 random walks, teleport probability is set to 0.2, five steps of PPR are performed.
Experiments show, that dropping first iteration (walks, finished after first step of PPR) improves the performance.
We take empirical distribution of all walks, finished after 2,3,4,5 steps of PPR to obtain pairwise weights PPR(source-->end).
Total number of finished walks is the same for all nodes and can be computed as a sum of geometric series
#finished = 0.2*0.8*2000 + 0.2*0.64*2000 + 0.2*0.512*2000 + 0.2*0.2048*2000 = 944.64
Thus, one can use #finished as a normalization factor to obtain distribution of walks for the source node.
We combine them with Freebase popularity scores (initial similarity) and apply constraints to compute coherence using formula (3) in Section 4.2 (Coherence and Constraints).
We then compute final score using formula (5) in Section 4.3 (PPRSim).