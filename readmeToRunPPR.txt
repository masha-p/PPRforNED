# personalized_page_rank_for_named_entity_disambiguation
This repository contains code used in the NAACL 2015 paper "Personalized Page Rank for Named Entity Disambiguation" by Maria Pershina, Yifan He, Ralph Grishman. 

Please consider citing our paper.

This is java PPRforNED code.

The code is self-contained, no special libraries is used, everything is just in one class (<800 lines).

You will need to change the name of the package and to set up three parameters at the beginning of main function (currently they are hard coded):

// Path to the file with freebase popularity scores, posted on GitHub, check link in paper.
String dir_freebase = “”;
// Path to the directory with files with candidates, posted on GitHub, check link in paper.
String dir_in = “”;
// Path to already created directory where output statistics will be written.
String dir_out = “”;

Running this code will produce three output files in the dir_out directory: “wrong_run_name” (disambiguation mistakes), 
“correct_run_name” (correct output), 
“all_run_name” (both).

Inside the code I listed results of running it on the PPRforNED data, they are a little bit better than those in the paper (~ +0.1) but overall consistent with what is reported there.
It is due to the randomness of the process - I may have changed my random seed at some point.

Freebase popularity scores are posted in the same directory as candidates for PPRforNED:
https://github.com/masha-p/PPRforNED

So now you have everything to replicate the results using this code.

Please let me know if you have any questions.

Thanks!
Masha