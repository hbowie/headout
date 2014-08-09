<!-- Generated using template product-user-guide-template.mdtoc -->
<!-- Generated using template product-user-guide-template.md -->
<h1 id="headout-user-guide">HeadOut User Guide</h1>


<h2 id="table-of-contents">Table of Contents</h2>

<div id="toc">
  <ul>
    <li>
      <a href="#introduction">Introduction</a>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li>
          <a href="#system-requirements">System Requirements</a>
        </li>
        <li>
          <a href="#rights">Rights</a>
        </li>
        <li>
          <a href="#installation">Installation</a>
        </li>
      </ul>

    </li>
    <li>
      <a href="#data-fields">Data Fields</a>
    </li>
    <li>
      <a href="#user-interface">User Interface</a>
    </li>
  </ul>

</div>


<h2 id="introduction">Introduction</h2>


HeadOut performs various Markdown/HTML transformations with headings and outlines. 


<h2 id="getting-started">Getting Started</h2>


<h3 id="system-requirements">System Requirements</h3>


HeadOut is written in Java and can run on any reasonably modern operating system, including Mac OS X, Windows and Linux. HeadOut requires a Java Runtime Environment (JRE), also known as a Java Virtual Machine (JVM). The version of this JRE/JVM must be at least 6. Visit [www.java.com](http://www.java.com) to download a recent version for most operating systems. Installation happens a bit differently under Mac OS X, but generally will occur fairly automatically when you try to launch a Java app for the first time.

Because HeadOut may be run on multiple platforms, it may look slightly different on different operating systems, and will obey slightly different conventions (using the CMD key on a Mac, vs. an ALT key on a PC, for example).

<h3 id="rights">Rights</h3>


HeadOut Copyright 2013 - 2014 by Herb Bowie

HeadOut is [open source software](http://opensource.org/osd). Source code is available at [GitHub](http://github.com/hbowie/headout).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

  [www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.


<h3 id="installation">Installation</h3>


Download the latest version from [PowerSurgePub.com](http://www.powersurgepub.com/downloads.html). Decompress the downloaded file. Drag the resulting file or folder into the location where you normally store your applications. Double-click on the jar file (or the application, if you've downloaded the Mac app) to launch.


<h2 id="data-fields">Data Fields</h2>


HeadOut works with headings and outlines in Markdown and HTML.

<h2 id="user-interface">User Interface</h2>


HeadOut has a straightforward user interface consisting of the following elements.

Specify Source for Input Text
:    You may select one of the following values from the drop down menu.

	System Clipboard
	:    The current contents of the system clipboard (typically populated via a cut or copy command from some other app) will be used as input.

	Local File
	:    You can specify a local text file as input.

Type of Transformation
:    Select one from the big drop down menu in the middle of the screen.

	Create Markdown ToC from Markdown
	:    This will interpret the input text as Markdown, and create a table of contents in Markdown as output, using the headings found in the input as entries in the table of contents.

	Create HTML ToC from Markdown
	:    This will interpret the input text as Markdown, and create a table of contents in HTML as output, using the headings found in the input as entries in the table of contents.

	Add ToC to Markdown
	:    This will interpret the input text as Markdown, add a table of contents in HTML to the source, and also convert Markdown headings to HTML. This ensures that the links in the table of contents use the same IDs as the ones used for the headings. The input file must have a heading with the text 'Table of Contents'. The generated table of contents will be inserted following this heading.

	Create Markdown from OPML
	:    This will interpret the input text as an Outline in OPML format, and create a Markdown document as output, with the outline entries converted to headings.

Specify Target for Output Text
:    You may select one of the following values from the drop down menu.

	System Clipboard
	:    The output of the transformation will be placed on the system clipboard (to be picked up later via a paste in another app).

	Local File
	:    You can specify a local text file as output.

Lowest Heading Level
:    Specify the lowest heading level to be considered, when generating a table of contents.

Highest Heading Level
:    Specify the highest heading level to be considered, when generating a table of contents.

Transform Now
:    Once your other parameters are set, press this button to perform the desired transformation.


[java]:       http://www.java.com/
[pspub]:      http://www.powersurgepub.com/
[downloads]:  http://www.powersurgepub.com/downloads.html
[osd]:		  http://opensource.org/osd
[gnu]:        http://www.gnu.org/licenses/
[apache]:	     http://www.apache.org/licenses/LICENSE-2.0.html
[markdown]:		http://daringfireball.net/projects/markdown/
[multimarkdown]:  http://fletcher.github.com/peg-multimarkdown/

[wikiq]:     http://www.wikiquote.org
[support]:   mailto:support@powersurgepub.com
[fortune]:   http://en.wikipedia.org/wiki/Fortune_(Unix)
[opml]:      http://en.wikipedia.org/wiki/OPML
[textile]:   http://en.wikipedia.org/wiki/Textile_(markup_language)
[pw]:        http://www.portablewisdom.org

[store]:     http://www.powersurgepub.com/store.html

[pegdown]:   https://github.com/sirthias/pegdown/blob/master/LICENSE
[parboiled]: https://github.com/sirthias/parboiled/blob/master/LICENSE
[Mathias]:   https://github.com/sirthias

[club]:         clubplanner.html
[filedir]:      filedir.html
[metamarkdown]: metamarkdown.html
[template]:     template.html

[mozilla]:    http://www.mozilla.org/MPL/2.0/


