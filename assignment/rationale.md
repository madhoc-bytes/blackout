Jeffery Pan - z5310210


Key Decisions

Because many of the methods required by specification required a decent amount of logic, I modularised code where it felt necessary. This can be seen in moving satellites, finding the recipients of files, checking compatibility between 2 apparatuses, and so on. On the contrary, some logic was left within the specified methods themselves as it deemed to be self-explanatory. 

Originally, I wanted to create exclusive classes for satellites and devices as they each had their own properties / functionality. However, upon completing task 1, I realised that the common themes between the 2 entities had lead to an incoherence with the DRY principle. As such I created a superclass, Apparatus, that encompasses the subclasses: Satellite and Device. Doing so allowed me to also enshrine the KISS principle, as I only had to loop through 1 list of apparatuses, instead of 2 lists (devices and satellites).

I also created a class File that has an aggregation to apparatus and a further subclass QuantumFile which allows me to follow KISS, while dealing with the nuances of shrinking satellites. 

Faced with the difficulty of sending files, I decided to not only have a list of files for each apparatus, but also an outbox: a list of files currently being sent. This simplified the algorithm for finding the amount of bandwidth that should be allocated to each file. The file sending process works as follows:

1. sendFile is called within simulation
if both apparatuses are in range, and no exceptions are thrown:
2. file is copied into sender's outbox AND empty file with file.getName is added to recipients 
3. find the correct number of bytes to send based on bandwidths. 
4. transfer those bytes from the file in the outbox to the recipient
5. remove files in the outbox if they're empty (finished sending all bytes)

