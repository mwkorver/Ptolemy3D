## Contributing to pTolemy3D ##


### Contributors ###

pTolemy3D aims to become the open standard for 3D web-based mapping applications using data from any source. pTolemy3D intends to enable unrestricted commercial and non-profit activities. Code contributors, both those contributing code to the Viewer and to data tools, help achieve these goals.

For a full list of Contributors to the pTolemy3D project, see [Authors](PtolemyContributeCla.md).


### Contributors License Agreements ###

pTolemy3D is open source and welcomes contributions from the community of people finding value in it. To accept contributions, the pTolemy3D project asks contributors to submit a Contributor License Agreement, which is similar in substance to the Apache Foundation's contributor agreement.

This document helps the project to transfer copyright of the code to an appropriate organization, such as OSGEO, when the time comes.
You can indicate your agreement by simply emailing a completed copy (gif, pdf etc) of the agreement to mark\_atmark\_spatialcloud.com

  * Individuals should submit the [Independent Contributor License Agreement (ICLA)](PtolemyContributeIcla.md)
  * Corporations should submit the [Corporate Contributor License Agreement (CCLA)](PtolemyContributeCcla.md)

Legally, copyright must be assigned to someone, so the pTolemy3D copyright will remain Mark Korver's until appropriate non profit such as OSGEO is ready to assume ownership. The purpose of the Contributor License Agreements is to clearly define the terms under which intellectual property has been contributed to pTolemy3D and thereby allow us to defend the project should there be a legal dispute regarding the software at some future time. This allows us to cleanly transfer pTolemy3D to another organization such as OSGEO in the future.

For a list of individuals and companies participating in pTolemy3D with signed CLAs, see: [CLAs](PtolemyContributeCla.md)
Submitting Code, Documentation, etc.

  * Start a ticket: If you have found a bug or a feature that you would like added to pTolemy3D, please file a ticket in the pTolemy3D Trac.
> > See FilingTickets for more info on how to provide a useful description of your bug or feature.
  * Upload your patch: If you have a patch that fixes the bug or implements the feature, you should take the following steps:
    1. Fill out a Contributor License Agreement -- See above.
> > 2. Attach your patch to the ticket -- See CreatingPatches for info on the preferred way to make a patch.
> > 3. Mark the ticket for review -- Add review to the keyword field for the ticket.
> > 4. Email the list -- Send an email to mark\_atmark\_spatialcloud.com with a brief summary of (and a link to) the ticket and any relevant notes about your patch. Please be sure to state clearly that you would like to have the patch reviewed.
> > 5. Wait for review -- A project committer will review your ticket ASAP!

  * The core pTolemy3D viewer code from pTolemy3D.org including ptolemy3D.js is made available under the [GPLv3 License](http://www.gnu.org/licenses/gpl-3.0.html), other ancillary code may be available under the less restrictive MIT License.

### Acquiring and Using Repository Commit Privileges ###

  * Anyone can request that the [Steering Committee](PtolemyOrgSteeringCommittee.md) give them the ability to commit changes directly to http://svn.pTolemy3D.org/.
> > Generally, we prefer to give committer access to developers who have demonstrated expertise and committment to the project.
  * Developers with committer access may make changes to their sandbox directory without seeking [Steering Committee](PtolemyOrgSteeringCommittee.md) approval, refer to the

Code changes should not be made without a corresponding ticket. Trac will automatically pick up on messages with text in a specific format, to help you manage tickets from commit messages. Including text of the form:

  * (Closes #925) -- Closes the ticket (with the revision number and commit message added as a comment).
  * (References #925) -- Adds a comment to the ticket with the commit message.

  * If the patch is from a new contributor, ensure that the user has sent in a CLA before committing and add the contributor's name to [Authors](PtolemyContributeCla.md) when committing