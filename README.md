# Pdf translator

tpdf is contextual translator.  
You can translate the pdf files without selecting word or text with click mouse, 
simply place the cursor over the word.
You can see the video for review demonstrating the work of tpdf.<br/>
[demo.mp4](https://drive.google.com/open?id=0B7fAKmTRcC2IX3VTbmhXSXlocnM)

-----
### Feature:
* linux or windows.

-----

### Requirements and restrictions:
* Its work requires jre 1.8
* compile: mvn assembly:assembly -DdescriptorId=jar-with-dependencies -DskipTests
* Launching from the command line: java -jar target/tpdf.jar --pdf=<file path> 