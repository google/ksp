# Frequently Asked Questions

## Why KSP?
KSP has several advantages over KAPT:
* It is faster.
* The API is more fluent for Kotlin users.
* It supports multiple round processing on generated Kotlin sources.
* It is being designed with multiplatform compatibility in mind.

## Why is KSP faster than KAPT?
KAPT has to parse and resolve every type reference in order to generate Java stubs, whereas KSP resolves references on-demand. Delegating to javac also takes time.

Additionally, KSP’s incremental processing model has a finer granularity than just isolating and aggregating. It finds more opportunities to avoid reprocessing everything. Also, because KSP traces symbol resolutions dynamically, a change in a file is less likely to pollute other files and therefore the set of files to be reprocessed is smaller. This is not possible for KAPT because it delegates processing to javac.

## Is KSP Kotlin specific?
KSP can process Java sources as well. The API is unified, meaning that when you parse a Java class and a Kotlin class you get a unified data structure in KSP.

## What is KSP’s project status and roadmap?
KSP is currently in the alpha phase, which means that the API is open to feedback and is subject to changes. You are welcome to give it a try but we would recommend not using it in production yet.

Once KSP enters beta, the API will be stabilized and the development focus will be on fixing issues / bugs in the implementation. We’ll do our best to minimize backward compatibility issues starting from the beta release.
